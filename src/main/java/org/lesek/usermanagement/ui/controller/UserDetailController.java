package org.lesek.usermanagement.ui.controller;

import io.micrometer.common.util.StringUtils;
import javafx.beans.InvalidationListener;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.lesek.usermanagement.model.Policy;
import org.lesek.usermanagement.model.User;
import org.lesek.usermanagement.service.PolicyAssignmentService;
import org.lesek.usermanagement.service.PolicyService;
import org.lesek.usermanagement.service.UserService;
import org.lesek.usermanagement.ui.AppContext;
import org.lesek.usermanagement.ui.navigation.AppContextAware;
import org.lesek.usermanagement.ui.navigation.LoadedView;
import org.lesek.usermanagement.ui.navigation.NavigationAware;
import org.lesek.usermanagement.ui.navigation.SceneNavigator;
import org.lesek.usermanagement.ui.navigation.UnsavedChangesGuard;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

public class UserDetailController implements NavigationAware, AppContextAware, UnsavedChangesGuard {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    private static final double ORGANIZATION_UNIT_WIDTH = 220;

    @FXML
    private TextField usernameField;
    @FXML
    private TextField firstNameField;
    @FXML
    private TextField lastNameField;
    @FXML
    private TextField emailField;
    @FXML
    private VBox organizationUnitRowsContainer;
    @FXML
    private DatePicker birthDatePicker;
    @FXML
    private DatePicker registeredOnPicker;
    @FXML
    private ListView<Policy> assignedPoliciesListView;
    @FXML
    private Button cancelButton;
    @FXML
    private Button saveButton;

    private final List<OrganizationUnitRow> organizationUnitRows = new ArrayList<>();

    private SceneNavigator navigator;
    private UserService userService;
    private PolicyService policyService;
    private PolicyAssignmentService policyAssignmentService;
    private List<String> availableOrganizationUnits = List.of();
    private boolean populating;
    private boolean dirty;
    private boolean refreshingOrganizationUnitChoices;
    /** The last-saved state being edited, or {@code null} when creating a new user. */
    private User originalUser;

    @Override
    public void setNavigator(SceneNavigator navigator) {
        this.navigator = navigator;
    }

    @Override
    public void setAppContext(AppContext appContext) {
        this.userService = appContext.userService();
        this.policyService = appContext.policyService();
        this.policyAssignmentService = appContext.policyAssignmentService();
        this.availableOrganizationUnits = appContext.organizationUnitService().getAllOrganizationUnits();
    }

    @FXML
    private void initialize() {
        InvalidationListener recomputeDirty = observable -> {
            if (!populating) {
                recomputeDirty();
            }
        };
        usernameField.textProperty().addListener(recomputeDirty);
        firstNameField.textProperty().addListener(recomputeDirty);
        lastNameField.textProperty().addListener(recomputeDirty);
        emailField.textProperty().addListener(recomputeDirty);
        birthDatePicker.valueProperty().addListener(recomputeDirty);
        registeredOnPicker.valueProperty().addListener(recomputeDirty);

        assignedPoliciesListView.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(Policy policy, boolean empty) {
                super.updateItem(policy, empty);
                setText(empty || policy == null ? null : policy.name());
            }
        });
        assignedPoliciesListView.setOnMouseClicked(event -> {
            Policy selected = assignedPoliciesListView.getSelectionModel().getSelectedItem();
            if (selected != null) {
                showPolicyDetail(selected);
            }
        });
    }

    private void showPolicyDetail(Policy policy) {
        LoadedView<PolicyDetailController> detail = navigator.load("/fxml/policy-detail-view.fxml");
        detail.controller().setPolicy(policy);
        navigator.navigateTo(detail);
    }

    /**
     * Shows an existing user for editing.
     * The username is the user's identifier so it cannot be changed here.
     */
    public void setUser(User user) {
        this.originalUser = user;
        populating = true;
        usernameField.setText(user.username());
        usernameField.setDisable(true);
        firstNameField.setText(user.firstName());
        lastNameField.setText(user.lastName());
        emailField.setText(user.emailAddress());
        clearOrganizationUnitRows();
        for (String unit : user.organizationUnit()) {
            createOrganizationUnitRow().setValue(unit);
        }
        refreshOrganizationUnitChoices();
        birthDatePicker.setValue(user.birthDate());
        registeredOnPicker.setValue(user.registeredOn());
        assignedPoliciesListView.getItems().setAll(policiesForUser(user.username()));
        populating = false;
        setDirty(false);
    }

    /**
     * Shows a blank form for creating a new user. Save &amp; close (and
     * Cancel) are visible right away, since there is nothing to save yet
     * but leaving would still discard whatever has been typed in.
     */
    public void setUserForCreation() {
        this.originalUser = null;
        populating = true;
        usernameField.setText("");
        usernameField.setDisable(false);
        firstNameField.setText("");
        lastNameField.setText("");
        emailField.setText("");
        clearOrganizationUnitRows();
        birthDatePicker.setValue(null);
        registeredOnPicker.setValue(LocalDate.now());
        assignedPoliciesListView.getItems().clear();
        populating = false;
        setDirty(true);
    }

    @FXML
    private void onAddOrganizationUnit() {
        createOrganizationUnitRow();
        refreshOrganizationUnitChoices();
        if (!populating) {
            recomputeDirty();
        }
    }

    private OrganizationUnitRow createOrganizationUnitRow() {
        OrganizationUnitRow row = new OrganizationUnitRow();
        organizationUnitRows.add(row);
        organizationUnitRowsContainer.getChildren().add(row.root);
        return row;
    }

    private void removeOrganizationUnitRow(OrganizationUnitRow row) {
        organizationUnitRows.remove(row);
        organizationUnitRowsContainer.getChildren().remove(row.root);
        refreshOrganizationUnitChoices();
        if (!populating) {
            recomputeDirty();
        }
    }

    private void clearOrganizationUnitRows() {
        organizationUnitRows.clear();
        organizationUnitRowsContainer.getChildren().clear();
    }

    /**
     * Keeps every row's choice restricted to units no other row is
     * currently using, so the same organization unit can never be picked
     * twice.
     */
    private void refreshOrganizationUnitChoices() {
        if (refreshingOrganizationUnitChoices) {
            return;
        }
        refreshingOrganizationUnitChoices = true;
        try {
            for (OrganizationUnitRow row : organizationUnitRows) {
                String current = row.value();
                List<String> usedByOtherRows = organizationUnitRows.stream()
                        .filter(other -> other != row)
                        .map(OrganizationUnitRow::value)
                        .filter(Objects::nonNull)
                        .toList();
                List<String> available = availableOrganizationUnits.stream()
                        .filter(unit -> !usedByOtherRows.contains(unit))
                        .toList();
                row.unitComboBox.getItems().setAll(available);
                row.unitComboBox.setValue(current);
            }
        } finally {
            refreshingOrganizationUnitChoices = false;
        }
    }

    private List<String> selectedOrganizationUnits() {
        return organizationUnitRows.stream()
                .map(OrganizationUnitRow::value)
                .filter(Objects::nonNull)
                .toList();
    }

    private List<Policy> policiesForUser(String userName) {
        return policyAssignmentService.getPolicyIdsForUser(userName).stream()
                .flatMap(policyId -> policyService.getPolicy(policyId).stream())
                .toList();
    }

    @FXML
    private void onSaveAndClose() {
        if (StringUtils.isBlank(usernameField.getText())) {
            showValidationAlert("Id is required.");
            return;
        }
        if (StringUtils.isBlank(firstNameField.getText())) {
            showValidationAlert("First name is required.");
            return;
        }
        if (StringUtils.isBlank(lastNameField.getText())) {
            showValidationAlert("Last name is required.");
            return;
        }
        if (StringUtils.isBlank(emailField.getText())) {
            showValidationAlert("Email is required.");
            return;
        }
        if (!isValidEmail(emailField.getText())) {
            showValidationAlert("Please enter a valid email address.");
            return;
        }
        if (birthDatePicker.getValue() == null) {
            showValidationAlert("Birth date is required.");
            return;
        }
        if (registeredOnPicker.getValue() == null) {
            showValidationAlert("Registered on is required.");
            return;
        }
        LocalDate birthDate = birthDatePicker.getValue();
        LocalDate registeredOn = registeredOnPicker.getValue();
        LocalDate today = LocalDate.now();
        if (birthDate != null && birthDate.isAfter(today)) {
            showValidationAlert("Birth date cannot be in the future.");
            return;
        }
        if (registeredOn != null && registeredOn.isAfter(today)) {
            showValidationAlert("Registered on cannot be in the future.");
            return;
        }
        if (birthDate != null && registeredOn != null && registeredOn.isBefore(birthDate)) {
            showValidationAlert("Registered on cannot be older than birth date.");
            return;
        }
        // do always in the last place due to database check
        if (originalUser == null && userService.getUser(usernameField.getText()).isPresent()) {
            showValidationAlert("A user with this id already exists.");
            return;
        }
        User updatedUser = buildUpdatedUser();
        navigator.runBusy(() -> userService.saveUser(updatedUser), () -> {
            setDirty(false);
            navigator.goBack();
        });
    }

    private boolean isValidEmail(String email) {
        return email != null && EMAIL_PATTERN.matcher(email.trim()).matches();
    }

    private void showValidationAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message, ButtonType.OK);
        alert.setTitle("Invalid user");
        alert.setHeaderText(null);
        alert.showAndWait();
    }

    @FXML
    private void onCancel() {
        navigator.goBack();
    }

    @Override
    public boolean confirmDiscardIfDirty() {
        if (!dirty) {
            return true;
        }
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                "You have unsaved changes. Are you sure you want to leave without saving?", ButtonType.YES, ButtonType.NO);
        alert.setTitle("Unsaved changes");
        alert.setHeaderText(null);
        return alert.showAndWait().filter(button -> button == ButtonType.YES).isPresent();
    }

    /**
     * Recomputes whether the form differs from the last-saved state, rather
     * than latching dirty on the first change and never clearing it - e.g.
     * typing something and then undoing it should not leave Save/Cancel
     * shown. Always dirty while creating a new user, since there is no
     * saved state to compare against yet.
     */
    private void recomputeDirty() {
        if (originalUser == null) {
            setDirty(true);
            return;
        }
        setDirty(!isSameAsOriginal(buildUpdatedUser()));
    }

    private boolean isSameAsOriginal(User candidate) {
        return Objects.equals(candidate.username(), originalUser.username())
                && Objects.equals(candidate.firstName(), originalUser.firstName())
                && Objects.equals(candidate.lastName(), originalUser.lastName())
                && Objects.equals(candidate.emailAddress(), originalUser.emailAddress())
                && Set.copyOf(candidate.organizationUnit()).equals(Set.copyOf(originalUser.organizationUnit()))
                && Objects.equals(candidate.birthDate(), originalUser.birthDate())
                && Objects.equals(candidate.registeredOn(), originalUser.registeredOn());
    }

    private void setDirty(boolean value) {
        this.dirty = value;
        cancelButton.setVisible(value);
        cancelButton.setManaged(value);
        saveButton.setVisible(value);
        saveButton.setManaged(value);
    }

    private User buildUpdatedUser() {
        return new User(
                usernameField.getText(),
                firstNameField.getText(),
                lastNameField.getText(),
                emailField.getText(),
                selectedOrganizationUnits(),
                birthDatePicker.getValue(),
                registeredOnPicker.getValue());
    }

    /**
     * One organization unit row: a combo box picking the unit, and a button
     * removing the row.
     */
    private class OrganizationUnitRow {
        private final ComboBox<String> unitComboBox = new ComboBox<>();
        private final Button removeButton = new Button("✕");
        private final HBox root = new HBox(8, unitComboBox, removeButton);

        OrganizationUnitRow() {
            unitComboBox.setPrefWidth(ORGANIZATION_UNIT_WIDTH);
            unitComboBox.setMinWidth(ORGANIZATION_UNIT_WIDTH);
            unitComboBox.setMaxWidth(ORGANIZATION_UNIT_WIDTH);
            unitComboBox.valueProperty().addListener((observable, oldValue, newValue) -> {
                if (refreshingOrganizationUnitChoices) {
                    // Selection was cleared/restored by refreshOrganizationUnitChoices() itself
                    // while rebuilding the item list - not a real user choice.
                    return;
                }
                refreshOrganizationUnitChoices();
                if (!populating) {
                    recomputeDirty();
                }
            });
            removeButton.setOnAction(event -> removeOrganizationUnitRow(this));
        }

        String value() {
            return unitComboBox.getValue();
        }

        void setValue(String value) {
            unitComboBox.setValue(value);
        }
    }
}
