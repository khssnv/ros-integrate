// This is a generated file. Not intended for manual editing.
package ros.integrate.msg.psi;

import com.intellij.psi.tree.IElementType;
import com.intellij.psi.PsiElement;
import com.intellij.lang.ASTNode;
import ros.integrate.msg.psi.impl.*;

public interface ROSMsgTypes {

  IElementType COMMENT = new ROSMsgElementType("COMMENT");
  IElementType CONST = new ROSMsgElementType("CONST");
  IElementType FIELD_NAME = new ROSMsgElementType("FIELD_NAME");
  IElementType PROPERTY = new ROSMsgElementType("PROPERTY");
  IElementType SEPARATOR = new ROSMsgElementType("SEPARATOR");
  IElementType TYPE = new ROSMsgElementType("TYPE");

  IElementType CONST_ASSIGNER = new ROSMsgTokenType("CONST_ASSIGNER");
  IElementType CRLF = new ROSMsgTokenType("CRLF");
  IElementType CUSTOM_TYPE = new ROSMsgTokenType("CUSTOM_TYPE");
  IElementType KEYTYPE = new ROSMsgTokenType("KEYTYPE");
  IElementType LBRACKET = new ROSMsgTokenType("LBRACKET");
  IElementType LINE_COMMENT = new ROSMsgTokenType("LINE_COMMENT");
  IElementType NAME = new ROSMsgTokenType("NAME");
  IElementType NEG_OPERATOR = new ROSMsgTokenType("NEG_OPERATOR");
  IElementType NUMBER = new ROSMsgTokenType("NUMBER");
  IElementType RBRACKET = new ROSMsgTokenType("RBRACKET");
  IElementType SERVICE_SEPARATOR = new ROSMsgTokenType("SERVICE_SEPARATOR");
  IElementType STRING = new ROSMsgTokenType("STRING");

  class Factory {
    public static PsiElement createElement(ASTNode node) {
      IElementType type = node.getElementType();
       if (type == COMMENT) {
        return new ROSMsgCommentImpl(node);
      }
      else if (type == CONST) {
        return new ROSMsgConstImpl(node);
      }
      else if (type == FIELD_NAME) {
        return new ROSMsgFieldNameImpl(node);
      }
      else if (type == PROPERTY) {
        return new ROSMsgPropertyImpl(node);
      }
      else if (type == SEPARATOR) {
        return new ROSMsgSeparatorImpl(node);
      }
      else if (type == TYPE) {
        return new ROSMsgTypeImpl(node);
      }
      throw new AssertionError("Unknown element type: " + type);
    }
  }
}
