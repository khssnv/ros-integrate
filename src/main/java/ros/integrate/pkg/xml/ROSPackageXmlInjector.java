package ros.integrate.pkg.xml;

import com.intellij.lang.html.HTMLLanguage;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.InjectedLanguagePlaces;
import com.intellij.psi.LanguageInjector;
import com.intellij.psi.PsiLanguageInjectionHost;
import com.intellij.psi.xml.XmlElementType;
import com.intellij.psi.xml.XmlTag;
import com.intellij.psi.xml.XmlText;
import com.intellij.psi.xml.XmlToken;
import org.jetbrains.annotations.NotNull;
import ros.integrate.pkg.xml.condition.lang.ROSConditionLanguage;

/**
 * Injects things for the ROS package.xml
 */
public class ROSPackageXmlInjector implements LanguageInjector {
    @Override
    public void getLanguagesToInject(@NotNull PsiLanguageInjectionHost host, @NotNull InjectedLanguagePlaces injectionPlacesRegistrar) {
        if (PackageXmlUtil.getWrapper(host.getContainingFile()) == null) {
            return;
        }
        XmlTag tag = PackageXmlUtil.getParentTag(host);
        if (tag == null) {
            return;
        }
        if (host instanceof XmlText && tag.getName().equals("description")) {
            injectionPlacesRegistrar.addPlace(HTMLLanguage.INSTANCE,
                    new TextRange(0, host.getTextLength()), null, null);
        }
        if (host instanceof XmlToken && ((XmlToken) host).getTokenType().equals(XmlElementType.XML_ATTRIBUTE_VALUE_TOKEN)
                && "condition".equals(PackageXmlUtil.getAttributeName(host))) {
            injectionPlacesRegistrar.addPlace(ROSConditionLanguage.INSTANCE, new TextRange(0, host.getTextLength()),
                    null, null);
        }
    }
}
