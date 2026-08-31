package org.lesek.usermanagement.ui.pages;

import org.apache.wicket.markup.html.link.BookmarkablePageLink;

/**
 * Landing page - equivalent of the JavaFX app's welcome screen, offering
 * navigation to the users and policies lists.
 */
public class WelcomePage extends BasePage {

    public WelcomePage() {
        add(new BookmarkablePageLink<>("viewUsers", UserListPage.class));
        add(new BookmarkablePageLink<>("viewPolicies", PolicyListPage.class));
    }
}
