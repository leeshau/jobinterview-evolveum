package org.lesek.usermanagement.ui.navigation;

/**
 * Implemented by controllers that should re-fetch their data from the
 * backend every time their view becomes visible again.
 * e.g.: a list screen that must reflect edits made on a detail screen navigated away to and back from.
 */
public interface Refreshable {

    void refresh();
}
