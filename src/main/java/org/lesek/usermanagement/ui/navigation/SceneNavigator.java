package org.lesek.usermanagement.ui.navigation;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import org.lesek.usermanagement.ui.AppContext;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Owns the application's single root layout: a back button that appears
 * whenever there is a previous view to return to, wrapped around whichever
 * content view is currently on screen, plus a small busy spinner overlay
 * shown while a blocking operation ({@link #runBusy}) is in progress.
 */
public class SceneNavigator {

    private record HistoryEntry(Parent view, Object controller) {
    }

    private final BorderPane shell = new BorderPane();
    private final StackPane root = new StackPane(shell);
    private final Button backButton = new Button("← Back");
    private final ProgressIndicator busyIndicator = new ProgressIndicator();
    private final Deque<HistoryEntry> history = new ArrayDeque<>();
    private final AppContext appContext;
    private Object currentController;

    public SceneNavigator(AppContext appContext) {
        this.appContext = appContext;
        backButton.setOnAction(event -> goBack());
        backButton.setVisible(false);
        backButton.setManaged(false);

        HBox topBar = new HBox(backButton);
        topBar.setPadding(new Insets(10));
        shell.setTop(topBar);

        busyIndicator.setMaxSize(40, 40);
        busyIndicator.setVisible(false);
        busyIndicator.setManaged(false);
        StackPane.setAlignment(busyIndicator, javafx.geometry.Pos.CENTER);
        root.getChildren().add(busyIndicator);
    }

    public Parent getRoot() {
        return root;
    }

    /**
     * Shows a small spinner, then runs {@code blockingWork} synchronously once that spinner has actually been rendered,
     * then hides the spinner and runs {@code onComplete}.
     * Used to give visual feedback for a synchronous call (e.g. a policy assignment recompute)
     * without moving it to a background thread.
     */
    public void runBusy(Runnable blockingWork, Runnable onComplete) {
        busyIndicator.setVisible(true);
        busyIndicator.setManaged(true);
        Platform.runLater(() -> {
            try {
                blockingWork.run();
            } finally {
                busyIndicator.setVisible(false);
                busyIndicator.setManaged(false);
                if (onComplete != null) {
                    onComplete.run();
                }
            }
        });
    }

    /**
     * Loads an FXML view and wires its controller.
     * {@link AppContextAware} controllers get the backend services.
     * Loading does not show the view - call {@link #showInitial} or {@link #navigateTo} once the controller has
     * been configured.
     */
    public <T> LoadedView<T> load(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent view = loader.load();
            T controller = loader.getController();
            if (controller instanceof NavigationAware navigationAware) {
                navigationAware.setNavigator(this);
            }
            if (controller instanceof AppContextAware appContextAware) {
                appContextAware.setAppContext(appContext);
            }
            return new LoadedView<>(view, controller);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to load view " + fxmlPath, e);
        }
    }

    /**
     * Shows the given view as the very first screen - no back button, empty history.
     */
    public void showInitial(LoadedView<?> loadedView) {
        history.clear();
        currentController = loadedView.controller();
        setContent(loadedView.view());
    }

    /**
     * Shows the given view, keeping the current one so the back button can return to it.
     */
    public void navigateTo(LoadedView<?> loadedView) {
        if (currentController instanceof UnsavedChangesGuard guard && !guard.confirmDiscardIfDirty()) {
            return;
        }
        Parent currentView = (Parent) shell.getCenter();
        if (currentView != null) {
            history.push(new HistoryEntry(currentView, currentController));
        }
        currentController = loadedView.controller();
        setContent(loadedView.view());
    }

    /**
     * Returns to the previous view, unless the view being left implements {@link UnsavedChangesGuard} and declines
     * e.g.: the user chose not to discard unsaved edits, in which case navigation is canceled.
     */
    public void goBack() {
        if (history.isEmpty()) {
            return;
        }
        if (currentController instanceof UnsavedChangesGuard guard && !guard.confirmDiscardIfDirty()) {
            return;
        }
        HistoryEntry previous = history.pop();
        currentController = previous.controller();
        setContent(previous.view());
    }

    private void setContent(Parent view) {
        shell.setCenter(view);
        boolean canGoBack = !history.isEmpty();
        backButton.setVisible(canGoBack);
        backButton.setManaged(canGoBack);
        if (currentController instanceof Refreshable refreshable) {
            refreshable.refresh();
        }
    }
}
