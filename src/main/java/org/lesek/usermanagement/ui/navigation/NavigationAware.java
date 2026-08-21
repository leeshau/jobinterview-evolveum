package org.lesek.usermanagement.ui.navigation;

/**
 * Implemented by controllers that need to trigger navigation themselves
 * (as opposed to just being navigated to and populated with data).
 */
public interface NavigationAware {

    void setNavigator(SceneNavigator navigator);
}
