package org.lesek.usermanagement.ui.pages;

import de.agilecoders.wicket.webjars.request.resource.WebjarsCssResourceReference;
import de.agilecoders.wicket.webjars.request.resource.WebjarsJavaScriptResourceReference;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.apache.wicket.request.resource.ResourceReference;
import org.lesek.usermanagement.ui.pages.policy.PolicyListPage;
import org.lesek.usermanagement.ui.pages.user.UserListPage;

/**
 * Shared layout (header, navigation, styling) for every page of the app -
 * the web-idiomatic replacement for the JavaFX app's shared scene chrome.
 * Bootstrap is pulled from the "org.webjars:bootstrap" Maven dependency
 * (served by wicket-webjars) rather than a CDN or vendored files, so the UI
 * works offline and Bootstrap is upgraded like any other dependency.
 */
public abstract class BasePage extends WebPage {

    private static final ResourceReference BOOTSTRAP_CSS =
            new WebjarsCssResourceReference("bootstrap/current/css/bootstrap.min.css");
    private static final ResourceReference BOOTSTRAP_JS =
            new WebjarsJavaScriptResourceReference("bootstrap/current/js/bootstrap.bundle.min.js");
    private static final PackageResourceReference APP_CSS =
            new PackageResourceReference(BasePage.class, "app.css");

    private String screenClass;

    public BasePage() {
        add(new BookmarkablePageLink<>("navBrand", WelcomePage.class));
        add(new BookmarkablePageLink<>("navHome", WelcomePage.class));
        add(new BookmarkablePageLink<>("navUsers", UserListPage.class));
        add(new BookmarkablePageLink<>("navPolicies", PolicyListPage.class));
    }

    /**
     * Lets a subclass tint the whole page background, not just the content
     * card, by naming a CSS class defined in app.css (e.g. "screen-user",
     * "screen-policy") - the actual colors live in CSS, not in Java.
     */
    protected void setScreenClass(String cssClass) {
        this.screenClass = cssClass;
    }

    @Override
    public void renderHead(IHeaderResponse response) {
        super.renderHead(response);
        response.render(CssHeaderItem.forReference(BOOTSTRAP_CSS));
        response.render(JavaScriptHeaderItem.forReference(BOOTSTRAP_JS));
        response.render(CssHeaderItem.forReference(APP_CSS));
        if (screenClass != null) {
            response.render(OnDomReadyHeaderItem.forScript(
                    "document.body.classList.add('" + screenClass + "');"));
        }
    }
}
