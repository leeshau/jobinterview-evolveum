package org.lesek.usermanagement.ui.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import org.lesek.usermanagement.model.User;
import org.lesek.usermanagement.service.UserService;
import org.lesek.usermanagement.ui.AppContext;
import org.lesek.usermanagement.ui.navigation.AppContextAware;
import org.lesek.usermanagement.ui.navigation.LoadedView;
import org.lesek.usermanagement.ui.navigation.NavigationAware;
import org.lesek.usermanagement.ui.navigation.Refreshable;
import org.lesek.usermanagement.ui.navigation.SceneNavigator;

public class UserListController implements NavigationAware, AppContextAware, Refreshable {

    @FXML
    private ListView<User> userListView;

    private SceneNavigator navigator;
    private UserService userService;

    @Override
    public void setNavigator(SceneNavigator navigator) {
        this.navigator = navigator;
    }

    @Override
    public void setAppContext(AppContext appContext) {
        this.userService = appContext.userService();
        refreshUsers();
    }

    @Override
    public void refresh() {
        refreshUsers();
    }

    @FXML
    private void initialize() {
        userListView.setCellFactory(list -> new UserRowCell());
        userListView.setOnMouseClicked(event -> {
            User selected = userListView.getSelectionModel().getSelectedItem();
            if (selected != null) {
                showDetail(selected);
            }
        });
    }

    @FXML
    private void onCreateUser() {
        LoadedView<UserDetailController> detail = navigator.load("/fxml/user-detail-view.fxml");
        detail.controller().setUserForCreation();
        navigator.navigateTo(detail);
    }

    private void refreshUsers() {
        userListView.getItems().setAll(userService.getAllUsers());
    }

    private void showDetail(User user) {
        LoadedView<UserDetailController> detail = navigator.load("/fxml/user-detail-view.fxml");
        detail.controller().setUser(user);
        navigator.navigateTo(detail);
    }

    private void deleteUser(User user) {
        if (!confirmDelete(user)) {
            return;
        }
        navigator.runBusy(() -> userService.deleteUser(user.username()), this::refreshUsers);
    }

    private boolean confirmDelete(User user) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                "Do you really want to delete user \"" + user.getFullName() + "\"?", ButtonType.YES, ButtonType.NO);
        alert.setTitle("Delete user");
        alert.setHeaderText(null);
        return alert.showAndWait().filter(button -> button == ButtonType.YES).isPresent();
    }

    /**
     * Shows the user's username with a Delete button that removes them without
     * triggering the row's own click-to-edit navigation.
     */
    private class UserRowCell extends ListCell<User> {
        private final Label nameLabel = new Label();
        private final Button deleteButton = new Button("Delete");
        private final HBox container = new HBox(8, nameLabel, spacer(), deleteButton);

        UserRowCell() {
            deleteButton.setOnAction(event -> {
                User user = getItem();
                if (user != null) {
                    deleteUser(user);
                }
            });
            deleteButton.addEventFilter(MouseEvent.MOUSE_CLICKED, MouseEvent::consume);
        }

        private Region spacer() {
            Region region = new Region();
            HBox.setHgrow(region, Priority.ALWAYS);
            return region;
        }

        @Override
        protected void updateItem(User user, boolean empty) {
            super.updateItem(user, empty);
            if (empty || user == null) {
                setGraphic(null);
            } else {
                nameLabel.setText(user.getFullName());
                setGraphic(container);
            }
        }
    }
}
