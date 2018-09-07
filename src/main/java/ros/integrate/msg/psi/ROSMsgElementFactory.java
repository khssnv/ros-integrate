package ros.integrate.msg.psi;

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFileFactory;
import ros.integrate.msg.ROSMsgFileType;

public class ROSMsgElementFactory {
    public static final String ANNOTATION_PREFIX = "# noinspection ";

    public static ROSMsgComment createAnnotation(Project project, String annotationText) {
        final ROSMsgFile file = createFile(project, ANNOTATION_PREFIX + annotationText);
        return (ROSMsgComment) file.getFirstChild();
    }

    public static ROSMsgField createField(Project project, String name, String value) {
        final ROSMsgFile file = createFile(project, name + " " + value);
        return (ROSMsgField) file.getFirstChild();
    }

    public static ROSMsgField createField(Project project, String name) {
        final ROSMsgFile file = createFile(project, name);
        return (ROSMsgField) file.getFirstChild();
    }

    public static ROSMsgField createSeperator(Project project) {
        final ROSMsgFile file = createFile(project, "---");
        return (ROSMsgField) file.getFirstChild();
    }

    public static PsiElement createCRLF(Project project) {
        final ROSMsgFile file = createFile(project, "\n");
        return file.getFirstChild();
    }

    public static ROSMsgFile createFile(Project project, String text) {
        String name = "dummy.msg";
        return (ROSMsgFile) PsiFileFactory.getInstance(project).
                createFileFromText(name, ROSMsgFileType.INSTANCE, text);
    }

    public static PsiElement createType(Project project, String typeName) {
        final ROSMsgFile file = createFile(project, typeName + " dummyName");
        return file.getFirstChild().getFirstChild();
    }

    public static ROSMsgFile createFile(String fileName, PsiDirectory directory) {
        return (ROSMsgFile) directory.createFile(fileName + ".msg");
    }
}
