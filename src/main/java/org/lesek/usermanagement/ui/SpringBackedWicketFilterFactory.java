package org.lesek.usermanagement.ui;

import org.apache.wicket.protocol.http.IWebApplicationFactory;
import org.apache.wicket.protocol.http.WebApplication;
import org.apache.wicket.protocol.http.WicketFilter;
import org.springframework.context.ApplicationContext;

/**
 * Builds the {@link WicketApplication}, wiring it to the Spring
 * {@link ApplicationContext} that {@link JettyLauncher} stashed as a
 * servlet-context attribute (there is no {@code ContextLoaderListener}
 * here, since Spring Boot's web layer is not used - only its IoC container).
 */
public class SpringBackedWicketFilterFactory implements IWebApplicationFactory {

    public static final String SPRING_CONTEXT_ATTRIBUTE =
            "org.lesek.usermanagement.ui.SPRING_APPLICATION_CONTEXT";

    @Override
    public WebApplication createApplication(WicketFilter filter) {
        ApplicationContext springContext = (ApplicationContext) filter.getFilterConfig()
                .getServletContext()
                .getAttribute(SPRING_CONTEXT_ATTRIBUTE);
        if (springContext == null) {
            throw new IllegalStateException(
                    "Spring ApplicationContext not found in servlet context attribute " + SPRING_CONTEXT_ATTRIBUTE);
        }
        return new WicketApplication(springContext);
    }

    @Override
    public void destroy(WicketFilter filter) {
        // Nothing to clean up here; the Spring context is shut down by JettyLauncher.
    }
}
