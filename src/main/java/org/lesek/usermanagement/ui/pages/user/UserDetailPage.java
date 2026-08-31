package org.lesek.usermanagement.ui.pages.user;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.CheckBoxMultipleChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.RequiredTextField;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.validation.validator.EmailAddressValidator;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.lesek.usermanagement.model.User;
import org.lesek.usermanagement.service.OrganizationUnitService;
import org.lesek.usermanagement.service.PolicyAssignmentService;
import org.lesek.usermanagement.service.UserService;
import org.lesek.usermanagement.ui.pages.BasePage;
import org.lesek.usermanagement.ui.pages.ErrorHighlightBehavior;
import org.lesek.usermanagement.ui.pages.policy.PolicyDetailPage;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * Create/edit screen for a single user - equivalent of the JavaFX
 * {@code UserDetailController}. Enforces the same validation rules
 * (documented in DOCUMENTATION.md) that the FXML form did: required fields,
 * a valid email, no future dates, {@code registeredOn} not before
 * {@code birthDate}, and no duplicate username on create.
 */
public class UserDetailPage extends BasePage {

    @SpringBean
    private UserService userService;

    @SpringBean
    private OrganizationUnitService organizationUnitService;

    @SpringBean
    private PolicyAssignmentService policyAssignmentService;

    private final boolean editMode;

    private TextField<String> usernameField;
    private TextField<String> birthDateField;
    private TextField<String> registeredOnField;

    public UserDetailPage() {
        this(new PageParameters());
    }

    public UserDetailPage(PageParameters parameters) {
        setScreenClass("screen-user");
        String username = parameters.get("username").toOptionalString();
        this.editMode = username != null;

        FormModel formModel = new FormModel();
        if (editMode) {
            User user = userService.getUser(username)
                    .orElseThrow(() -> new IllegalArgumentException("Unknown user: " + username));
            formModel.username = user.username();
            formModel.firstName = user.firstName();
            formModel.lastName = user.lastName();
            formModel.emailAddress = user.emailAddress();
            formModel.organizationUnit = new ArrayList<>(user.organizationUnit());
            formModel.birthDate = user.birthDate() == null ? "" : user.birthDate().toString();
            formModel.registeredOn = user.registeredOn() == null ? "" : user.registeredOn().toString();
        } else {
            formModel.registeredOn = LocalDate.now().toString();
        }

        add(new Label("heading", editMode ? "Edit user: " + username : "Create user"));

        FeedbackPanel feedbackPanel = new FeedbackPanel("feedback");
        add(feedbackPanel);

        Form<FormModel> form = new Form<>("form", new CompoundPropertyModel<>(formModel)) {
            @Override
            protected void onSubmit() {
                handleSubmit(getModelObject());
            }
        };
        add(form);

        usernameField = new RequiredTextField<>("username");
        usernameField.setEnabled(!editMode);
        form.add(usernameField);

        form.add(new RequiredTextField<>("firstName"));
        form.add(new RequiredTextField<>("lastName"));

        TextField<String> emailField = new RequiredTextField<>("emailAddress");
        emailField.add(EmailAddressValidator.getInstance());
        form.add(emailField);

        CheckBoxMultipleChoice<String> organizationUnitField = new CheckBoxMultipleChoice<>("organizationUnit",
                organizationUnitService.getAllOrganizationUnits());
        organizationUnitField.setPrefix("<div class=\"form-check\">");
        organizationUnitField.setSuffix("</div>");
        form.add(organizationUnitField);

        birthDateField = new RequiredTextField<>("birthDate");
        birthDateField.add(AttributeModifier.replace("type", "date"));
        form.add(birthDateField);

        registeredOnField = new RequiredTextField<>("registeredOn");
        registeredOnField.add(AttributeModifier.replace("type", "date"));
        form.add(registeredOnField);

        form.visitChildren(FormComponent.class, (component, visit) -> component.add(new ErrorHighlightBehavior()));

        form.add(new Link<Void>("cancel") {
            @Override
            public void onClick() {
                setResponsePage(UserListPage.class);
            }
        });

        WebMarkupContainer policiesSection = new WebMarkupContainer("policiesSection");
        policiesSection.setVisible(editMode);
        add(policiesSection);
        List<String> policyIds = editMode ? policyAssignmentService.getPolicyIdsForUser(username) : List.of();
        policiesSection.add(new Label("policiesHint", policyIds.isEmpty()
                ? "There is no matching policy for this user right now."
                : "Policies currently applicable to this user."));
        policiesSection.add(new ListView<>("policyRow", policyIds) {
            @Override
            protected void populateItem(ListItem<String> item) {
                String policyId = item.getModelObject();
                PageParameters params = new PageParameters().add("id", policyId);
                BookmarkablePageLink<Void> link = new BookmarkablePageLink<>("policyLink", PolicyDetailPage.class, params);
                link.add(new Label("policyId", policyId));
                item.add(link);
            }
        });
    }

    private void handleSubmit(FormModel model) {
        LocalDate birthDate;
        LocalDate registeredOn;
        try {
            birthDate = LocalDate.parse(model.birthDate);
        } catch (DateTimeParseException e) {
            birthDateField.error("Birth date must be a valid date (yyyy-MM-dd).");
            return;
        }
        try {
            registeredOn = LocalDate.parse(model.registeredOn);
        } catch (DateTimeParseException e) {
            registeredOnField.error("Registered on must be a valid date (yyyy-MM-dd).");
            return;
        }

        LocalDate today = LocalDate.now();
        if (birthDate.isAfter(today)) {
            birthDateField.error("Birth date cannot be in the future.");
            return;
        }
        if (registeredOn.isAfter(today)) {
            registeredOnField.error("Registered on cannot be in the future.");
            return;
        }
        if (registeredOn.isBefore(birthDate)) {
            registeredOnField.error("Registered on cannot be before birth date.");
            return;
        }
        if (!editMode && userService.getUser(model.username).isPresent()) {
            usernameField.error("A user with username '" + model.username + "' already exists.");
            return;
        }

        User user = new User(model.username, model.firstName, model.lastName, model.emailAddress,
                model.organizationUnit, birthDate, registeredOn);
        userService.saveUser(user);
        setResponsePage(UserListPage.class);
    }

    /**
     * Plain, serializable backing bean for the form's text/choice fields
     * (dates kept as raw strings so an invalid date is reported as a
     * validation error rather than a hard conversion failure).
     */
    public static class FormModel implements Serializable {
        public String username = "";
        public String firstName = "";
        public String lastName = "";
        public String emailAddress = "";
        public List<String> organizationUnit = new ArrayList<>();
        public String birthDate = "";
        public String registeredOn = "";
    }
}
