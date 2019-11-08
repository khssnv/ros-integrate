package ros.integrate.pkg;

import com.intellij.ide.highlighter.XmlFileType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.newvfs.events.VFileEvent;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.search.FileTypeIndex;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.GlobalSearchScopesCore;
import com.intellij.psi.xml.XmlFile;
import com.intellij.util.containers.MultiMap;
import com.intellij.util.containers.SortedList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ros.integrate.pkg.xml.PackageXmlUtil;
import ros.integrate.pkg.xml.ROSPackageXml;
import ros.integrate.pkt.lang.ROSPktLanguage;
import ros.integrate.pkt.psi.ROSPktFile;
import ros.integrate.pkg.psi.ROSPackage;

import java.util.*;
import java.util.concurrent.ConcurrentMap;

import static ros.integrate.pkg.psi.ROSPackage.RootType;

public abstract class ROSPackageFinderBase implements ROSPackageFinder {
    @Override
    public void findAndCache(Project project, ConcurrentMap<String, ROSPackage> pkgCache) {
        /*
         * the files that actually determine whether or not a directory is a package is the package.xml file.
         * the following condition is true:
         * A directory is a ROS package iff one of its roots directly contains a package.xml file
         */
        FileTypeIndex.getFiles(XmlFileType.INSTANCE, getScope(project))
                .stream().filter(xml -> xml.getName().equals(PackageXmlUtil.PACKAGE_XML))
                .map(vXml -> investigateXml(vXml, project, pkgCache))
                .filter(newPkg -> newPkg != ROSPackage.ORPHAN)
                .forEach(newPkg -> pkgCache.putIfAbsent(newPkg.getName(), newPkg));
    }

    @NotNull
    private ROSPackage investigateXml(@NotNull VirtualFile vXml, Project project, ConcurrentMap<String, ROSPackage> pkgCache) {
        // 1. get package name
        // FIXME for now, since we don't want to read into files just yet, we will use the directory to name packages.
        String pkgName = vXml.getParent().getName();
        // 2. search for the package in the cache. If it exists, move on to next xml.
        if (pkgCache != null && pkgCache.getOrDefault(pkgName, ROSPackage.ORPHAN) != ROSPackage.ORPHAN)
            return ROSPackage.ORPHAN;
        // 3. get package.xml file in XML PSI form
        XmlFile pkgXml = (XmlFile) Objects.requireNonNull(PsiManager.getInstance(project).findFile(vXml));
        // 4. get package root dir
        PsiDirectory xmlRoot = pkgXml.getContainingDirectory();
        // 4.5. TODO other roots of compiled packages: lib,include,bin(?),etc
        // 5. TODO try getting CMakeLists.txt since this is a project package
        // 6. find all packet files
        // FIXME for now they will just be searched in the project regardless of CMakeLists.txt
        List<ROSPktFile> pkgPackets = findPacketFiles(xmlRoot/*,pkgCMake*/);
        // 7. make package and cache it. MAKE SURE IT IS NOT ROSPackage.ORPHAN

        return tryNewROSPackage(project, pkgName, xmlRoot , pkgXml, pkgPackets);
    }

    @NotNull
    private List<ROSPktFile> findPacketFiles(@NotNull PsiDirectory pkgRoot) {
        List<ROSPktFile> ret = new SortedList<>(Comparator.comparing(ROSPktFile::getQualifiedName));
        for (PsiFile file : pkgRoot.getFiles()) {
            if (file.getLanguage() == ROSPktLanguage.INSTANCE) {
                ret.add((ROSPktFile) file);
            }
        }
        for (PsiDirectory subDir : pkgRoot.getSubdirectories()) {
            ret.addAll(findPacketFiles(subDir));
        }
        return ret;
    }

    @Override
    public MultiMap<ROSPackage, VFileEvent> investigate(@NotNull Project project, @NotNull Collection<VFileEvent> events) {
        MultiMap<ROSPackage, VFileEvent> ret = new MultiMap<>();
        // 1. check who is under the jurisdiction of this finder
        Set<VirtualFile> directoriesToSearch = new TreeSet<>(Comparator.comparing(VirtualFile::getPath,
                new TreePathComparator()));
        events.stream().filter(event -> inFinder(event,project))
                .map(ROSPackageUtil::getParentOfEvent)
                .filter(Objects::nonNull)
                .forEach(directoriesToSearch::add);
        removeUnneededChildDirs(directoriesToSearch);
        // 2. do XML parents first and get a list of parent directories, use these to create ROSPackages using original method.
        List<PsiFile> pkgFiles = new LinkedList<>();
        directoriesToSearch.stream()
                .map(dir -> new GlobalSearchScopesCore.DirectoryScope(project,dir,true))
                .map(scope -> FilenameIndex.getFilesByName(project, PackageXmlUtil.PACKAGE_XML,scope))
                .map(Arrays::asList)
                .forEach(pkgFiles::addAll);
        pkgFiles.forEach(xml -> {
            ROSPackage newPkg = investigateXml(xml.getVirtualFile(), project, null);
            if (newPkg != ROSPackage.ORPHAN) {
                ret.putValues(newPkg, new ArrayList<>(0));
            }
        });
        events.parallelStream().forEach(event -> sortEventByRoot(event, ret));
        return ret;
    }

    private void removeUnneededChildDirs(@NotNull Set<VirtualFile> directoriesToSearch) {
        Iterator<VirtualFile> iter = directoriesToSearch.iterator();
        if (!iter.hasNext()) {
            return;
        }
        VirtualFile vParent = iter.next();
        for (VirtualFile vFile = vParent; iter.hasNext(); vFile = iter.next()) { // this cannot be possible with bad sorting.
            if (vFile.equals(vParent)) {
                continue;
            }
            if (ROSPackageUtil.belongsToRoot(vParent, vFile)) {
                iter.remove();
            } else {
                vParent = vFile;
            }
        }
    }

    private void sortEventByRoot(@NotNull VFileEvent event, @NotNull MultiMap<ROSPackage, VFileEvent> map) {
        if(map.containsScalarValue(event)) {
            return;
        }
        for (ROSPackage pkg : map.keySet()) {
            for (PsiDirectory root : pkg.getRoots()) {
                if (ROSPackageUtil.belongsToRoot(root, event)) {
                    map.putValue(pkg, event);
                    return;
                }
            }
        }
    }

    @Nullable
    @Override
    public CacheCommand investigateChanges(Project project, ROSPackage pkg) {
        if(!getPackageType().isInstance(pkg)) {
            return null;
        }
        // 0. check the package is under the jurisdiction of this finder.
        PsiDirectory xmlRoot = Objects.requireNonNull(pkg.getRoot(RootType.SHARE));
        if(notInFinder(xmlRoot.getVirtualFile(), project)) { // something is up with the root
            if(xmlRoot.getParentDirectory() != null && // check parent - if its in the project, the dir was deleted.
                    notInFinder(xmlRoot.getParentDirectory().getVirtualFile(), project)) {
                return null;
            }
            return CacheCommand.DELETE;
        }
        // 1. check package XML exists. if some don't -> return DELETE.
        XmlFile newXml = PackageXmlUtil.findPackageXml(xmlRoot);
        if(newXml == null) {
            return CacheCommand.DELETE;
        }
        // if package.xml was changed, change it in the package.
        if(!newXml.equals(Optional.ofNullable(pkg.getPackageXml()).map(ROSPackageXml::getRawXml).get())) {
            pkg.setPackageXml(newXml);
        }
        // 2. check for new packets in root & apply changes
        pkg.setPackets(findPacketFiles(xmlRoot));
        // 3. check new name of PSI directory (XML in the future. if renamed -> set RENAME and continue)
        if(!xmlRoot.getName().equals(pkg.getName())) { // change to XML eventually
            String newName = xmlRoot.getName();
            if(!pkg.getName().equals(newName)) {
                pkg.setName(newName);
            }
            return CacheCommand.RENAME;
        } else {
            return CacheCommand.NONE;
        }
    }

    /**
     * @return the package class this finder is responsible for
     */
    abstract Class<? extends ROSPackage> getPackageType();

    /**
     * attempts to create a new package based on found info.
     * @param project the project this finder belongs to
     * @param pkgName the name of the new package
     * @param xmlRoot the root of the package.xml file.
     * @param pkgXml the pkg.xml file
     * @param pkgPackets all packets found in the package, using the CMakeLists.txt and a directory visitor.
     * @return {@link ROSPackage#ORPHAN} if package could not be created, otherwise a new package.
     * @implNote if the return is {@link ROSPackage#ORPHAN} make sure to log it.
     */
    @NotNull
    abstract ROSPackage tryNewROSPackage(Project project, String pkgName, PsiDirectory xmlRoot,
                                         XmlFile pkgXml, List<ROSPktFile> pkgPackets);

    /**
     * @param event the event to test
     * @param project the project the finder belongs to
     * @return {@code true} if the event is under the jurisdiction of the finder, false otherwise.
     */
    abstract boolean inFinder(@NotNull VFileEvent event, @NotNull Project project);

    /**
     * @param file the file to test
     * @param project the project the finder belongs to
     * @return {@code false} if the file is under the jurisdiction of the finder, true otherwise.
     */
    abstract boolean notInFinder(@NotNull VirtualFile file, @NotNull Project project);

    @NotNull
    abstract GlobalSearchScope getScope(Project project); // should be updated to protected
}
