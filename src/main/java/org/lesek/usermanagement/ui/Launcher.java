package org.lesek.usermanagement.ui;

/**
 * Entry point used when running from a plain classpath (e.g. an IDE's
 * "Run" on a {@code main} method, or {@code java -cp ...}).
 * <p>
 * The JDK launcher special-cases any main class that extends
 * {@link javafx.application.Application}: when invoked directly via
 * {@code -cp} (rather than through {@code --module-path}), it aborts with
 * "JavaFX runtime components are missing" even if the JavaFX jars are on the
 * classpath. Routing through this separate, non-Application class sidesteps
 * that check entirely.
 */
public class Launcher {

    public static void main(String[] args) {
        MainApp.main(args);
    }
}
