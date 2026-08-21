package org.lesek.usermanagement.ui.navigation;

/**
 * Implemented by controllers that can have unsaved changes. Consulted by
 * {@link SceneNavigator#goBack} before leaving the view, so navigating away
 * (via the back button or a screen's own "cancel") can prompt the user
 * instead of silently discarding edits.
 */
public interface UnsavedChangesGuard {

    /**
     * @return true if it is fine to leave now - either there is nothing
     * unsaved, or the user confirmed discarding it.
     */
    boolean confirmDiscardIfDirty();
}
