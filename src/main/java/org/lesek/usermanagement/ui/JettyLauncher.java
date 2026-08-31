package org.lesek.usermanagement.ui;

import org.apache.wicket.protocol.http.WicketFilter;
import org.eclipse.jetty.ee10.servlet.FilterHolder;
import org.eclipse.jetty.ee10.servlet.ServletContextHandler;
import org.eclipse.jetty.server.Server;
import org.lesek.usermanagement.UserManagementApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;

import jakarta.servlet.DispatcherType;
import java.util.EnumSet;

/**
 * Composition root for the web UI: starts the (non-web) Spring Boot context
 * that holds all backend beans (repositories/services/policy engine), then
 * boots a plain embedded Jetty server hosting the Wicket application - no
 * external application server, no WAR.
 */
public final class JettyLauncher {

    private static final int DEFAULT_PORT = 8080;

    private JettyLauncher() {
    }

    public static void main(String[] args) throws Exception {
        ConfigurableApplicationContext springContext = new SpringApplicationBuilder(UserManagementApplication.class)
                .web(org.springframework.boot.WebApplicationType.NONE)
                .run(args);

        int port = Integer.getInteger("server.port", DEFAULT_PORT);

        Server server = new Server(port);

        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");
        server.setHandler(context);

        FilterHolder wicketFilterHolder = new FilterHolder(WicketFilter.class);
        wicketFilterHolder.setInitParameter(WicketFilter.APP_FACT_PARAM,
                SpringBackedWicketFilterFactory.class.getName());
        wicketFilterHolder.setInitParameter(WicketFilter.FILTER_MAPPING_PARAM, "/*");
        context.addFilter(wicketFilterHolder, "/*", EnumSet.of(DispatcherType.REQUEST));
        context.setAttribute(SpringBackedWicketFilterFactory.SPRING_CONTEXT_ATTRIBUTE, springContext);

        server.start();
        server.join();
    }
}
