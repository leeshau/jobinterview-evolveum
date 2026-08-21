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
import org.lesek.usermanagement.model.Policy;
import org.lesek.usermanagement.service.PolicyService;
import org.lesek.usermanagement.ui.AppContext;
import org.lesek.usermanagement.ui.navigation.AppContextAware;
import org.lesek.usermanagement.ui.navigation.LoadedView;
import org.lesek.usermanagement.ui.navigation.NavigationAware;
import org.lesek.usermanagement.ui.navigation.Refreshable;
import org.lesek.usermanagement.ui.navigation.SceneNavigator;

public class PolicyListController implements NavigationAware, AppContextAware, Refreshable {

    @FXML
    private ListView<Policy> policyListView;

    private SceneNavigator navigator;
    private PolicyService policyService;

    @Override
    public void setNavigator(SceneNavigator navigator) {
        this.navigator = navigator;
    }

    @Override
    public void setAppContext(AppContext appContext) {
        this.policyService = appContext.policyService();
        refreshPolicies();
    }

    @Override
    public void refresh() {
        refreshPolicies();
    }

    @FXML
    private void initialize() {
        policyListView.setCellFactory(list -> new PolicyRowCell());
        policyListView.setOnMouseClicked(event -> {
            Policy selected = policyListView.getSelectionModel().getSelectedItem();
            if (selected != null) {
                showDetail(selected);
            }
        });
    }

    @FXML
    private void onCreatePolicy() {
        LoadedView<PolicyDetailController> detail = navigator.load("/fxml/policy-detail-view.fxml");
        detail.controller().setPolicyForCreation();
        navigator.navigateTo(detail);
    }

    private void refreshPolicies() {
        policyListView.getItems().setAll(policyService.getAllPolicies());
    }

    private void showDetail(Policy policy) {
        LoadedView<PolicyDetailController> detail = navigator.load("/fxml/policy-detail-view.fxml");
        detail.controller().setPolicy(policy);
        navigator.navigateTo(detail);
    }

    private void deletePolicy(Policy policy) {
        if (!confirmDelete(policy)) {
            return;
        }
        // PolicyService.deletePolicy already recomputes and re-persists the
        // policy assignment snapshot for every user, so nobody is left
        // referencing a policy that no longer exists.
        navigator.runBusy(() -> policyService.deletePolicy(policy.id()), this::refreshPolicies);
    }

    private boolean confirmDelete(Policy policy) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                "Do you really want to delete policy \"" + policy.name() + "\"?", ButtonType.YES, ButtonType.NO);
        alert.setTitle("Delete policy");
        alert.setHeaderText(null);
        return alert.showAndWait().filter(button -> button == ButtonType.YES).isPresent();
    }

    /**
     * Shows the policy's username with a Delete button that removes it without
     * triggering the row's own click-to-edit navigation.
     */
    private class PolicyRowCell extends ListCell<Policy> {
        private final Label nameLabel = new Label();
        private final Button deleteButton = new Button("Delete");
        private final HBox container = new HBox(8, nameLabel, spacer(), deleteButton);

        PolicyRowCell() {
            deleteButton.setOnAction(event -> {
                Policy policy = getItem();
                if (policy != null) {
                    deletePolicy(policy);
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
        protected void updateItem(Policy policy, boolean empty) {
            super.updateItem(policy, empty);
            if (empty || policy == null) {
                setGraphic(null);
            } else {
                nameLabel.setText(policy.name());
                setGraphic(container);
            }
        }
    }
}
