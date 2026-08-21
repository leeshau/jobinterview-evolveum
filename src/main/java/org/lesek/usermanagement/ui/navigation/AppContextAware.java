package org.lesek.usermanagement.ui.navigation;

import org.lesek.usermanagement.ui.AppContext;

/**
 * Implemented by controllers that need access to backend services.
 */
public interface AppContextAware {

    void setAppContext(AppContext appContext);
}
