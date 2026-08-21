package org.lesek.usermanagement.ui.navigation;

import javafx.scene.Parent;

/**
 * An FXML view together with its controller, as returned by {@link SceneNavigator#load},
 * so the caller can pass data to the controller even before the view is actually rendered.
 */
public record LoadedView<T>(Parent view, T controller) {
}
