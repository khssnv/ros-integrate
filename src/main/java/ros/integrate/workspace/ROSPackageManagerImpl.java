package ros.integrate.workspace;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.*;
import com.intellij.openapi.vfs.newvfs.BulkFileListener;
import com.intellij.openapi.vfs.newvfs.events.VFileEvent;
import com.intellij.psi.PsiDirectory;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.MultiMap;
import com.intellij.util.containers.SortedList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ros.integrate.workspace.psi.ROSPackage;

import java.util.*;
import java.util.concurrent.ConcurrentMap;

public class ROSPackageManagerImpl implements ROSPackageManager {
    private final ConcurrentMap<String, ROSPackage> pkgCache = ContainerUtil.createConcurrentSoftValueMap();
    private final Project project;

    private static List<ROSPackageFinder> finders = ROSPackageFinder.EP_NAME.getExtensionList();

    public ROSPackageManagerImpl(@NotNull Project project) {
        this.project = project;
    }

    @Override
    public void projectOpened() {
        findAndCachePackages();
        // add a watch to VirtualFileSystem that will trigger this
        project.getMessageBus().connect().subscribe(VirtualFileManager.VFS_CHANGES, new BulkFileListener() {
            @Override
            public void after(@NotNull List<? extends VFileEvent> events) {
                doBulkFileChangeEvents(events);
            }
        });
    }

    private void findAndCachePackages() {
        // use each package finder to make and cache packages
        finders.forEach(finder -> finder.findAndCache(project,pkgCache));
    }

    private void doBulkFileChangeEvents(@NotNull List<? extends VFileEvent> events) {
        // 1. group by parent dir name (convert to package if possible)
        List<ROSPackage> affectedPackages = new SortedList<>(Comparator.comparing(ROSPackage::getName));
        List<VFileEvent> affectedOrphans = new SortedList<>(Comparator.comparing(VFileEvent::getPath)),
                affectedOrphansOld = new SortedList<>(Comparator.comparing(VFileEvent::getPath));
        affectedOrphansOld.addAll(events); boolean orphansRemainedTheSame = false;
        while (!orphansRemainedTheSame) {
            affectedOrphansOld.parallelStream().forEach(event -> sortToLists(event,affectedPackages,affectedOrphans));
            // 2. figure out what happened per package per file & react accordingly (create new package, delete new package, modify details)
            affectedPackages.forEach(this::applyChangesToPackage);
            if (affectedOrphans.containsAll(affectedOrphansOld)) {
                orphansRemainedTheSame = true;
            } else {
                affectedOrphansOld.retainAll(affectedOrphans);
                affectedOrphans.clear();
            }
        }
        applyChangesToOrphans(affectedOrphans); // we now know that these files are not renamed packages.
    }

    private void applyChangesToOrphans(@NotNull List<VFileEvent> events) {
        /* possible things that can happen:
         * new package
         */
        for (ROSPackageFinder finder: finders) {
            MultiMap<ROSPackage,VFileEvent> newPackages = finder.investigate(project,events);
            for (Map.Entry<ROSPackage,Collection<VFileEvent>> newPkg : newPackages.entrySet()) {
                if(newPkg.getKey() != ROSPackage.ORPHAN) {
                    pkgCache.putIfAbsent(newPkg.getKey().getName(),newPkg.getKey());
                    events.removeAll(newPkg.getValue()); // removes associated events from collection.
                }
            }
        }
    }

    private void applyChangesToPackage(@NotNull ROSPackage pkg) {
        /* possible things that can happen:
         * details update
         * removed pkg
         * package moved entirely
         */
        String oldName = pkg.getName();
        for (ROSPackageFinder finder: finders) {
            ROSPackageFinder.CacheCommand cmd = finder.investigateChanges(project,pkg);
            if(cmd == null) {
                continue;
            }
            switch (cmd) {
                case DELETE: {
                    pkgCache.remove(pkg.getName());
                    break;
                }
                case RENAME: {
                    pkgCache.remove(oldName);
                    pkgCache.putIfAbsent(pkg.getName(), pkg);
                    break;
                }
                case NONE:
                default:
                    break;
            }
        }
    }

    private void sortToLists(VFileEvent event, List<ROSPackage> affectedPackages,
                             List<VFileEvent> affectedOrphans) {
        // try to see if it falls under a package, if not put it under the orphan list
        int successfulSorts = 0;
        for (ROSPackage pkg : getAllPackages()) {
            for (PsiDirectory root : pkg.getRoots()) {
                if(ROSPackageUtil.belongsToRoot(root,event)) {
                    affectedPackages.add(pkg);
                    successfulSorts++;
                    if(ROSPackageUtil.getRequiredSorts(event,root) == successfulSorts) {
                        return;
                    }
                }
            }
        }
        // if no package was found
        affectedOrphans.add(event);
    }

    @Override
    public List<ROSPackage> getAllPackages() {
        final SortedList<ROSPackage> ret = new SortedList<>(Comparator.comparing(ROSPackage::getName));
        pkgCache.forEach((name,pkg) -> ret.add(pkg));
        return ret;
    }

    @Nullable
    @Override
    public ROSPackage findPackage(String pkgName) {
        return pkgCache.get(pkgName);
    }

    @Override
    public void updatePackageName(ROSPackage pkg, String newName) {
        pkgCache.remove(pkg.getName());
        pkgCache.put(newName,pkg);
    }
}
