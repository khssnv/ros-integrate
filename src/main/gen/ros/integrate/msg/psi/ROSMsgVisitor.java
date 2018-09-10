// This is a generated file. Not intended for manual editing.
package ros.integrate.msg.psi;

import org.jetbrains.annotations.*;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiElement;

public class ROSMsgVisitor extends PsiElementVisitor {

  public void visitComment(@NotNull ROSMsgComment o) {
    visitPsiElement(o);
  }

  public void visitConst(@NotNull ROSMsgConst o) {
    visitPsiElement(o);
  }

  public void visitField(@NotNull ROSMsgField o) {
    visitPsiElement(o);
  }

  public void visitLabel(@NotNull ROSMsgLabel o) {
    visitIdentifier(o);
  }

  public void visitSeparator(@NotNull ROSMsgSeparator o) {
    visitPsiElement(o);
  }

  public void visitType(@NotNull ROSMsgType o) {
    visitIdentifier(o);
  }

  public void visitIdentifier(@NotNull ROSMsgIdentifier o) {
    visitPsiElement(o);
  }

  public void visitPsiElement(@NotNull PsiElement o) {
    visitElement(o);
  }

}
