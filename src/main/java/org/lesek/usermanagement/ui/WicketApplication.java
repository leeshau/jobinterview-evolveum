package org.lesek.usermanagement.ui;

import de.agilecoders.wicket.webjars.WicketWebjars;
import org.apache.wicket.protocol.http.WebApplication;
import org.apache.wicket.spring.injection.annot.SpringComponentInjector;
import org.lesek.usermanagement.ui.pages.WelcomePage;
import org.lesek.usermanagement.ui.pages.policy.PolicyDetailPage;
import org.lesek.usermanagement.ui.pages.policy.PolicyListPage;
import org.lesek.usermanagement.ui.pages.user.UserDetailPage;
import org.lesek.usermanagement.ui.pages.user.UserListPage;
import org.springframework.context.ApplicationContext;

/**
 * Wicket web application replacing the former JavaFX desktop UI. Beans are
 * pulled from the Spring {@link ApplicationContext} started by
 * {@link JettyLauncher} via {@code @SpringBean} injection.
 */
public class WicketApplication extends WebApplication {

    private final ApplicationContext applicationContext;

    public WicketApplication(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Override
    public Class<? extends org.apache.wicket.Page> getHomePage() {
        return WelcomePage.class;
    }

    @Override
    protected void init() {
        super.init();
        getComponentInstantiationListeners().add(new SpringComponentInjector(this, applicationContext, true));
        WicketWebjars.install(this);

        // Disable page versioning: without it, every re-render of a stateful page
        // (e.g. after a form submit) got a new numbered "page id" appended to the
        // URL (e.g. "/users/edit?2"), and the browser back button could reopen an
        // old page instance holding stale model data instead of the current one.
        getPageSettings().setVersionPagesByDefault(false);

        mountPage("/users", UserListPage.class);
        mountPage("/users/edit", UserDetailPage.class);
        mountPage("/policies", PolicyListPage.class);
        mountPage("/policies/edit", PolicyDetailPage.class);
    }
}
