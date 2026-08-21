package org.lesek.usermanagement.ui.controller;

import javafx.fxml.FXML;
import org.lesek.usermanagement.ui.navigation.LoadedView;
import org.lesek.usermanagement.ui.navigation.NavigationAware;
import org.lesek.usermanagement.ui.navigation.SceneNavigator;

public class WelcomeController implements NavigationAware {

    private SceneNavigator navigator;

    @Override
    public void setNavigator(SceneNavigator navigator) {
        this.navigator = navigator;
    }

    @FXML
    private void onViewUsers() {
        LoadedView<UserListController> userList = navigator.load("/fxml/user-list-view.fxml");
        navigator.navigateTo(userList);
    }

    @FXML
    private void onViewPolicies() {
        LoadedView<PolicyListController> policyList = navigator.load("/fxml/policy-list-view.fxml");
        navigator.navigateTo(policyList);
    }
}
