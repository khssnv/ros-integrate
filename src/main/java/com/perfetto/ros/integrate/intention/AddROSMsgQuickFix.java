package com.perfetto.ros.integrate.intention;

import com.intellij.codeInsight.intention.impl.BaseIntentionAction;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.fileEditor.ex.IdeDocumentHistory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.util.IncorrectOperationException;
import com.perfetto.ros.integrate.dialogue.NewROSMsgDialogue;
import com.perfetto.ros.integrate.psi.ROSMsgElementFactory;
import com.perfetto.ros.integrate.psi.ROSMsgFile;
import org.jetbrains.annotations.NotNull;

public class AddROSMsgQuickFix extends BaseIntentionAction {

    public AddROSMsgQuickFix(PsiElement fieldType) {
        type = fieldType;
    }

    private PsiElement type;

    @NotNull
    @Override
    public String getFamilyName() {
        return "ROS Message/Service errors";
    }

    @NotNull
    @Override
    public String getText() {
        return "Create new ROS Message in project";
    }

    @Override
    public boolean isAvailable(@NotNull Project project, Editor editor, PsiFile file) {
        return true;
    }

    @Override
    public void invoke(@NotNull Project project, Editor editor, PsiFile file)
            throws IncorrectOperationException {
        PsiDocumentManager.getInstance(project).commitAllDocuments();

        NewROSMsgDialogue dialogue = new NewROSMsgDialogue(project,file.getContainingDirectory(),type.getText());
        dialogue.show();
        if(dialogue.getExitCode() != DialogWrapper.OK_EXIT_CODE) {
            return;
        }
        ApplicationManager.getApplication().runWriteAction(() -> {

            ROSMsgFile rosMsgFile = ROSMsgElementFactory.createFile(dialogue.getFileName(), dialogue.getDirectory());

            IdeDocumentHistory.getInstance(project).includeCurrentPlaceAsChangePlace();

            if (!type.getText().equals(dialogue.getFileName())) {
                type.replace(ROSMsgElementFactory.createType(project, dialogue.getFileName()));
            }

            OpenFileDescriptor descriptor = new OpenFileDescriptor(rosMsgFile.getProject(), rosMsgFile.getVirtualFile());
            FileEditorManager.getInstance(rosMsgFile.getProject()).openTextEditor(descriptor, true);
        });
    }

    @Override
    public boolean startInWriteAction() {
        return false;
    }
}
