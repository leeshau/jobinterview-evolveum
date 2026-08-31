package org.lesek.usermanagement.ui.pages.policy;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.RequiredTextField;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.util.ListModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.lesek.usermanagement.model.Policy;
import org.lesek.usermanagement.policy.condition.ConditionFactory;
import org.lesek.usermanagement.policy.condition.FieldType;
import org.lesek.usermanagement.service.PolicyAssignmentService;
import org.lesek.usermanagement.service.PolicyService;
import org.lesek.usermanagement.ui.pages.BasePage;
import org.lesek.usermanagement.ui.pages.ErrorHighlightBehavior;
import org.lesek.usermanagement.ui.pages.user.UserDetailPage;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Create/edit screen for a single policy - equivalent of the JavaFX
 * {@code PolicyDetailController}. Conditions are edited as rows (a field, an
 * operator and a value); as in the original FXML editor, a field already
 * used by one row cannot be picked again in another, so at most one row per
 * field can be created here (multi-operator-per-field policies still need a
 * hand edit of policies.json, as documented in DOCUMENTATION.md).
 */
public class PolicyDetailPage extends BasePage {

    @SpringBean
    private PolicyService policyService;

    @SpringBean
    private PolicyAssignmentService policyAssignmentService;

    @SpringBean
    private ConditionFactory conditionFactory;

    private final boolean editMode;
    private final List<ConditionRow> rows = new ArrayList<>();
    private final WebMarkupContainer rowsContainer;
    private final TextField<String> idField;

    public PolicyDetailPage() {
        this(new PageParameters());
    }

    public PolicyDetailPage(PageParameters parameters) {
        setScreenClass("screen-policy");
        String id = parameters.get("id").toOptionalString();
        this.editMode = id != null;

        FormModel formModel = new FormModel();
        if (editMode) {
            Policy policy = policyService.getPolicy(id)
                    .orElseThrow(() -> new IllegalArgumentException("Unknown policy: " + id));
            formModel.id = policy.id();
            formModel.name = policy.name();
            for (Map.Entry<String, Map<String, Object>> entry : policy.conditions().entrySet()) {
                // Mirrors the JavaFX editor's limitation: only the first operator of a
                // field's assertions is representable by a single row.
                entry.getValue().entrySet().stream().findFirst().ifPresent(op ->
                        rows.add(new ConditionRow(entry.getKey(), op.getKey(), String.valueOf(op.getValue()))));
            }
        }
        if (rows.isEmpty()) {
            rows.add(new ConditionRow(null, null, ""));
        }

        add(new Label("heading", editMode ? "Edit policy: " + id : "Create policy"));

        FeedbackPanel feedbackPanel = new FeedbackPanel("feedback");
        add(feedbackPanel);

        Form<FormModel> form = new Form<>("form", new CompoundPropertyModel<>(formModel)) {
            @Override
            protected void onSubmit() {
                handleSubmit(getModelObject());
            }
        };
        add(form);

        idField = new RequiredTextField<>("id");
        idField.setEnabled(!editMode);
        form.add(idField);
        form.add(new RequiredTextField<>("name"));
        form.visitChildren(FormComponent.class, (component, visit) -> component.add(new ErrorHighlightBehavior()));

        rowsContainer = new WebMarkupContainer("rowsContainer");
        form.add(rowsContainer.setOutputMarkupId(true));
        rowsContainer.add(buildRowsListView());

        form.add(new AjaxLink<Void>("addRow") {
            @Override
            public void onClick(AjaxRequestTarget target) {
                rows.add(new ConditionRow(null, null, ""));
                rowsContainer.replace(buildRowsListView());
                target.add(rowsContainer);
            }
        });

        form.add(new Link<Void>("cancel") {
            @Override
            public void onClick() {
                setResponsePage(PolicyListPage.class);
            }
        });

        WebMarkupContainer usersSection = new WebMarkupContainer("usersSection");
        usersSection.setVisible(editMode);
        add(usersSection);
        List<String> userIds = editMode ? policyAssignmentService.getUserIdsForPolicy(id) : List.of();
        usersSection.add(new ListView<>("userRow", userIds) {
            @Override
            protected void populateItem(ListItem<String> item) {
                String username = item.getModelObject();
                PageParameters params = new PageParameters().add("username", username);
                BookmarkablePageLink<Void> link = new BookmarkablePageLink<>("userLink", UserDetailPage.class, params);
                link.add(new Label("username", username));
                item.add(link);
            }
        });
    }

    private ListView<ConditionRow> buildRowsListView() {
        ListView<ConditionRow> listView = new ListView<>("conditionRow", new ListModel<>(rows)) {
            @Override
            protected void populateItem(ListItem<ConditionRow> item) {
                ConditionRow row = item.getModelObject();

                DropDownChoice<String> fieldChoice = new DropDownChoice<>("field",
                        new PropertyModel<>(row, "fieldName"), availableFieldChoicesFor(row), new ChoiceRenderer<>());
                fieldChoice.setNullValid(true);
                fieldChoice.add(new AjaxFormComponentUpdatingBehavior("change") {
                    @Override
                    protected void onUpdate(AjaxRequestTarget target) {
                        row.operator = null;
                        rowsContainer.replace(buildRowsListView());
                        target.add(rowsContainer);
                    }
                });
                item.add(fieldChoice);

                DropDownChoice<String> operatorChoice = new DropDownChoice<>("operator",
                        new PropertyModel<>(row, "operator"), availableOperatorChoicesFor(row), new ChoiceRenderer<>());
                operatorChoice.setNullValid(true);
                item.add(operatorChoice);

                item.add(new TextField<>("value", new PropertyModel<>(row, "value")));

                item.add(new AjaxLink<Void>("removeRow") {
                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        rows.remove(row);
                        if (rows.isEmpty()) {
                            rows.add(new ConditionRow(null, null, ""));
                        }
                        rowsContainer.replace(buildRowsListView());
                        target.add(rowsContainer);
                    }
                });
            }
        };
        listView.setOutputMarkupId(true);
        return listView;
    }

    private List<String> availableFieldChoicesFor(ConditionRow row) {
        List<String> usedByOthers = rows.stream()
                .filter(r -> r != row && r.fieldName != null)
                .map(r -> r.fieldName)
                .toList();
        return conditionFactory.availableFields().keySet().stream()
                .filter(field -> !usedByOthers.contains(field))
                .toList();
    }

    private List<String> availableOperatorChoicesFor(ConditionRow row) {
        if (row.fieldName == null) {
            return List.of();
        }
        FieldType type = conditionFactory.availableFields().get(row.fieldName);
        return type == null ? List.of() : type.supportedOperators();
    }

    private void handleSubmit(FormModel model) {
        Map<String, Map<String, Object>> conditions = new LinkedHashMap<>();
        for (ConditionRow row : rows) {
            if (row.fieldName == null && row.operator == null && (row.value == null || row.value.isBlank())) {
                continue;
            }
            if (row.fieldName == null || row.operator == null || row.value == null || row.value.isBlank()) {
                error("Every condition row needs a field, an operator and a value.");
                return;
            }
            conditions.put(row.fieldName, Map.of(row.operator, row.value));
        }
        if (conditions.isEmpty()) {
            error("At least one condition is required.");
            return;
        }
        if (!editMode && policyService.getPolicy(model.id).isPresent()) {
            idField.error("A policy with id '" + model.id + "' already exists.");
            return;
        }

        Policy policy = new Policy(model.id, model.name, conditions);
        policyService.savePolicy(policy);
        setResponsePage(PolicyListPage.class);
    }

    public static class FormModel implements Serializable {
        public String id = "";
        public String name = "";
    }

    public static class ConditionRow implements Serializable {
        public String fieldName;
        public String operator;
        public String value;

        public ConditionRow(String fieldName, String operator, String value) {
            this.fieldName = fieldName;
            this.operator = operator;
            this.value = value;
        }
    }
}
