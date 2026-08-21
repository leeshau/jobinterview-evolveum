package org.lesek.usermanagement.ui.controller;

import javafx.beans.InvalidationListener;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.lesek.usermanagement.model.Policy;
import org.lesek.usermanagement.model.User;
import org.lesek.usermanagement.policy.condition.ConditionFactory;
import org.lesek.usermanagement.policy.condition.FieldType;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class PolicyDetailController implements NavigationAware, AppContextAware, UnsavedChangesGuard {

    @FXML
    private TextField idField;
    @FXML
    private TextField nameField;
    @FXML
    private VBox conditionRowsContainer;
    @FXML
    private ListView<User> assignedUsersListView;
    @FXML
    private Button cancelButton;
    @FXML
    private Button saveButton;

    private final List<ConditionRow> conditionRows = new ArrayList<>();

    private SceneNavigator navigator;
    private PolicyService policyService;
    private UserService userService;
    private PolicyAssignmentService policyAssignmentService;
    private ConditionFactory conditionFactory;
    private boolean populating;
    private boolean dirty;
    private boolean refreshingFieldChoices;
    /** The last-saved state being edited, or {@code null} when creating a new policy. */
    private Policy originalPolicy;

    @Override
    public void setNavigator(SceneNavigator navigator) {
        this.navigator = navigator;
    }

    @Override
    public void setAppContext(AppContext appContext) {
        this.policyService = appContext.policyService();
        this.userService = appContext.userService();
        this.policyAssignmentService = appContext.policyAssignmentService();
        this.conditionFactory = appContext.conditionFactory();
    }

    @FXML
    private void initialize() {
        InvalidationListener recomputeDirty = observable -> {
            if (!populating) {
                recomputeDirty();
            }
        };
        idField.textProperty().addListener(recomputeDirty);
        nameField.textProperty().addListener(recomputeDirty);

        assignedUsersListView.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(User user, boolean empty) {
                super.updateItem(user, empty);
                setText(empty || user == null ? null : user.getFullName());
            }
        });
        assignedUsersListView.setOnMouseClicked(event -> {
            User selected = assignedUsersListView.getSelectionModel().getSelectedItem();
            if (selected != null) {
                showUserDetail(selected);
            }
        });
    }

    private void showUserDetail(User user) {
        LoadedView<UserDetailController> detail = navigator.load("/fxml/user-detail-view.fxml");
        detail.controller().setUser(user);
        navigator.navigateTo(detail);
    }

    /**
     * Shows an existing policy for editing.
     * The id is the policy's identifier so it cannot be changed here.
     * Each field the policy declares a condition on becomes one row.
     */
    public void setPolicy(Policy policy) {
        this.originalPolicy = policy;
        populating = true;
        idField.setText(policy.id());
        idField.setEditable(false);
        nameField.setText(policy.name());
        clearConditionRows();
        policy.conditions().forEach((field, operators) -> {
            if (operators.isEmpty()) {
                return;
            }
            // just a fail-safe if anyone were to manipulate the data in JSON outside of this application
            Map.Entry<String, Object> firstOperator = operators.entrySet().iterator().next();
            ConditionRow row = createConditionRow();
            row.populate(field, firstOperator.getKey(), firstOperator.getValue());
        });
        refreshFieldChoices();
        assignedUsersListView.getItems().setAll(usersForPolicy(policy.id()));
        populating = false;
        setDirty(false);
    }

    /**
     * Shows a blank form for creating a new policy. Save &amp; close (and Cancel) are visible right away,
     * since there is nothing to save yet but leaving would still discard whatever has been typed in.
     */
    public void setPolicyForCreation() {
        this.originalPolicy = null;
        populating = true;
        idField.setText("");
        idField.setEditable(true);
        nameField.setText("");
        clearConditionRows();
        assignedUsersListView.getItems().clear();
        populating = false;
        setDirty(true);
    }

    @FXML
    private void onAddCondition() {
        createConditionRow();
        refreshFieldChoices();
        if (!populating) {
            recomputeDirty();
        }
    }

    private ConditionRow createConditionRow() {
        ConditionRow row = new ConditionRow();
        conditionRows.add(row);
        conditionRowsContainer.getChildren().add(row.root);
        return row;
    }

    private void removeConditionRow(ConditionRow row) {
        conditionRows.remove(row);
        conditionRowsContainer.getChildren().remove(row.root);
        refreshFieldChoices();
        if (!populating) {
            recomputeDirty();
        }
    }

    /**
     * Keeps every row's field choice restricted to fields no other row is
     * currently using, so the same user field can never be picked twice.
     */
    private void refreshFieldChoices() {
        if (refreshingFieldChoices) {
            // Reentrant call: setAll()/setValue() below temporarily clear a
            // combo box's selection, which fires its value listener - which
            // would otherwise call back into this method and recurse
            // forever. Only the outermost call needs to actually run.
            return;
        }
        refreshingFieldChoices = true;
        try {
            List<String> allFields = new ArrayList<>(conditionFactory.availableFields().keySet());
            for (ConditionRow row : conditionRows) {
                String current = row.fieldComboBox.getValue();
                List<String> usedByOtherRows = conditionRows.stream()
                        .filter(other -> other != row)
                        .map(other -> other.fieldComboBox.getValue())
                        .filter(Objects::nonNull)
                        .toList();
                List<String> available = allFields.stream()
                        .filter(field -> !usedByOtherRows.contains(field))
                        .toList();
                row.fieldComboBox.getItems().setAll(available);
                row.fieldComboBox.setValue(current);
            }
        } finally {
            refreshingFieldChoices = false;
        }
    }

    private List<User> usersForPolicy(String policyId) {
        return policyAssignmentService.getUserIdsForPolicy(policyId).stream()
                .flatMap(userId -> userService.getUser(userId).stream())
                .toList();
    }

    @FXML
    private void onSaveAndClose() {
        if (idField.getText() == null || idField.getText().isBlank()) {
            showValidationAlert("Id is required.");
            return;
        }
        if (nameField.getText() == null || nameField.getText().isBlank()) {
            showValidationAlert("Name is required.");
            return;
        }
        if (conditionRows.isEmpty()) {
            showValidationAlert("At least one condition is required.");
            return;
        }
        Map<String, Map<String, Object>> conditions = buildConditionsOrShowError();
        if (conditions == null) {
            return;
        }
        Policy updatedPolicy = new Policy(idField.getText(), nameField.getText(), conditions);
        navigator.runBusy(() -> policyService.savePolicy(updatedPolicy), () -> {
            setDirty(false);
            navigator.goBack();
        });
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
     * Recomputes whether the form differs from the last-saved state,
     * rather than latching dirty on the first change and never clearing it.
     * e.g.: clicking a combo box without actually changing its selection should not leave Save/Cancel shown.
     * Always dirty while creating a new policy, since there is no saved state to compare against yet.
     */
    private void recomputeDirty() {
        if (originalPolicy == null) {
            setDirty(true);
            return;
        }
        Map<String, Map<String, Object>> currentConditions = buildConditionsQuietly();
        boolean same = Objects.equals(idField.getText(), originalPolicy.id())
                && Objects.equals(nameField.getText(), originalPolicy.name())
                && currentConditions != null
                && conditionsEqual(currentConditions, originalPolicy.conditions());
        setDirty(!same);
    }

    /**
     * Same as {@link #buildConditionsOrShowError()} but silent: used only to compare against the saved state,
     * so an incomplete row just means "not equal to anything saved" rather than something to alert the user about.
     */
    private Map<String, Map<String, Object>> buildConditionsQuietly() {
        Map<String, Map<String, Object>> conditions = new LinkedHashMap<>();
        for (ConditionRow row : conditionRows) {
            String field = row.fieldName();
            String operator = row.operator();
            if (field == null || operator == null) {
                return null;
            }
            Object value;
            try {
                value = row.valueForStorage();
            } catch (NumberFormatException e) {
                return null;
            }
            if (value == null || (value instanceof String text && text.isBlank())) {
                return null;
            }
            conditions.put(field, Map.of(operator, value));
        }
        return conditions;
    }

    private boolean conditionsEqual(Map<String, Map<String, Object>> a, Map<String, Map<String, Object>> b) {
        if (a.size() != b.size()) {
            return false;
        }
        for (Map.Entry<String, Map<String, Object>> entry : a.entrySet()) {
            Map<String, Object> otherOperators = b.get(entry.getKey());
            if (otherOperators == null || otherOperators.size() != entry.getValue().size()) {
                return false;
            }
            for (Map.Entry<String, Object> operatorEntry : entry.getValue().entrySet()) {
                if (!valuesEqual(operatorEntry.getValue(), otherOperators.get(operatorEntry.getKey()))) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean valuesEqual(Object a, Object b) {
        if (a instanceof Number numberA && b instanceof Number numberB) {
            // JSON round-tripping through Jackson may hand back an Integer
            // where the editor produces a Double for the same value - both
            // represent the same condition, so compare numerically.
            return numberA.doubleValue() == numberB.doubleValue();
        }
        return Objects.equals(a, b);
    }

    private void setDirty(boolean value) {
        this.dirty = value;
        cancelButton.setVisible(value);
        cancelButton.setManaged(value);
        saveButton.setVisible(value);
        saveButton.setManaged(value);
    }

    private void clearConditionRows() {
        conditionRows.clear();
        conditionRowsContainer.getChildren().clear();
    }

    /**
     * Builds the conditions map from the current rows, or shows an error
     * and returns {@code null} if any row is incomplete or invalid.
     */
    private Map<String, Map<String, Object>> buildConditionsOrShowError() {
        Map<String, Map<String, Object>> conditions = new LinkedHashMap<>();
        for (ConditionRow row : conditionRows) {
            String field = row.fieldName();
            String operator = row.operator();
            if (field == null || operator == null) {
                showValidationAlert("Every condition needs a field and an operator selected.");
                return null;
            }
            Object value;
            try {
                value = row.valueForStorage();
            } catch (NumberFormatException e) {
                showValidationAlert("Field \"" + field + "\" needs a numeric value.");
                return null;
            }
            if (value == null || (value instanceof String text && text.isBlank())) {
                showValidationAlert("Field \"" + field + "\" needs a value.");
                return null;
            }
            conditions.put(field, Map.of(operator, value));
        }
        return conditions;
    }

    private void showValidationAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message, ButtonType.OK);
        alert.setTitle("Invalid policy");
        alert.setHeaderText(null);
        alert.showAndWait();
    }

    /**
     * One condition row: a user field, the operator asserted against it,
     * and a value editor whose control matches the field's type.
     */
    private class ConditionRow {
        private static final double FIELD_WIDTH = 160;
        private static final double OPERATOR_WIDTH = 130;
        private static final double VALUE_WIDTH = 220;

        private final ComboBox<String> fieldComboBox = new ComboBox<>();
        private final ComboBox<String> operatorComboBox = new ComboBox<>();
        private final TextField textValueField = new TextField();
        private final ComboBox<String> booleanValueComboBox =
                new ComboBox<>(FXCollections.observableArrayList("true", "false"));
        private final DatePicker dateValuePicker = new DatePicker();
        private final StackPane valueContainer = new StackPane(textValueField);
        private final Button removeButton = new Button("✕");
        private final HBox root = new HBox(8, fieldComboBox, operatorComboBox, valueContainer, removeButton);

        ConditionRow() {
            fieldComboBox.setPrefWidth(FIELD_WIDTH);
            fieldComboBox.setMinWidth(FIELD_WIDTH);
            fieldComboBox.setMaxWidth(FIELD_WIDTH);
            operatorComboBox.setPrefWidth(OPERATOR_WIDTH);
            operatorComboBox.setMinWidth(OPERATOR_WIDTH);
            operatorComboBox.setMaxWidth(OPERATOR_WIDTH);
            valueContainer.setPrefWidth(VALUE_WIDTH);
            valueContainer.setMinWidth(VALUE_WIDTH);
            valueContainer.setMaxWidth(VALUE_WIDTH);
            textValueField.setPrefWidth(VALUE_WIDTH);
            booleanValueComboBox.setPrefWidth(VALUE_WIDTH);
            booleanValueComboBox.setMinWidth(VALUE_WIDTH);
            booleanValueComboBox.setMaxWidth(VALUE_WIDTH);
            dateValuePicker.setPrefWidth(VALUE_WIDTH);
            dateValuePicker.setMinWidth(VALUE_WIDTH);
            dateValuePicker.setMaxWidth(VALUE_WIDTH);

            InvalidationListener recomputeDirtyListener = observable -> {
                if (!populating) {
                    recomputeDirty();
                }
            };
            fieldComboBox.valueProperty().addListener((observable, oldValue, newValue) -> {
                if (refreshingFieldChoices) {
                    // Selection was cleared/restored by refreshFieldChoices()
                    // itself while rebuilding the item list - not a real
                    // user choice, so there is nothing to react to.
                    return;
                }
                updateOperatorChoicesAndValueEditor();
                refreshFieldChoices();
                if (!populating) {
                    recomputeDirty();
                }
            });
            operatorComboBox.valueProperty().addListener(recomputeDirtyListener);
            textValueField.textProperty().addListener(recomputeDirtyListener);
            booleanValueComboBox.valueProperty().addListener(recomputeDirtyListener);
            dateValuePicker.valueProperty().addListener(recomputeDirtyListener);
            removeButton.setOnAction(event -> removeConditionRow(this));
        }

        private void updateOperatorChoicesAndValueEditor() {
            FieldType type = fieldType();
            String previousOperator = operatorComboBox.getValue();
            List<String> operators = type == null ? List.of() : type.supportedOperators();
            operatorComboBox.getItems().setAll(operators);
            operatorComboBox.setValue(previousOperator != null && operators.contains(previousOperator)
                    ? previousOperator
                    : operators.isEmpty() ? null : operators.get(0));
            valueContainer.getChildren().setAll(valueEditorFor(type));
        }

        private Node valueEditorFor(FieldType type) {
            if (type == FieldType.BOOLEAN) {
                return booleanValueComboBox;
            }
            if (type == FieldType.DATE) {
                return dateValuePicker;
            }
            return textValueField;
        }

        private FieldType fieldType() {
            String field = fieldComboBox.getValue();
            return field == null ? null : conditionFactory.availableFields().get(field);
        }

        String fieldName() {
            return fieldComboBox.getValue();
        }

        String operator() {
            return operatorComboBox.getValue();
        }

        Object valueForStorage() {
            FieldType type = fieldType();
            if (type == null) {
                return null;
            }
            return switch (type) {
                case STRING -> textValueField.getText();
                case NUMBER -> Double.parseDouble(textValueField.getText());
                case BOOLEAN -> Boolean.parseBoolean(booleanValueComboBox.getValue());
                case DATE -> dateValuePicker.getValue() == null ? null : dateValuePicker.getValue().toString();
            };
        }

        void populate(String field, String operator, Object value) {
            fieldComboBox.setValue(field);
            operatorComboBox.setValue(operator);
            FieldType type = fieldType();
            if (type == FieldType.BOOLEAN) {
                booleanValueComboBox.setValue(String.valueOf(value));
            } else if (type == FieldType.DATE) {
                dateValuePicker.setValue(LocalDate.parse(String.valueOf(value)));
            } else if (type == FieldType.NUMBER) {
                textValueField.setText(String.valueOf(value));
            } else {
                textValueField.setText(String.valueOf(value));
            }
        }
    }
}
