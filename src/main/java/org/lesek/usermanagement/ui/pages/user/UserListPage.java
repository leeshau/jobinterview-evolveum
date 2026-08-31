package org.lesek.usermanagement.ui.pages.user;

import org.apache.wicket.AttributeModifier;
import org.lesek.usermanagement.ui.pages.BasePage;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.lesek.usermanagement.model.User;
import org.lesek.usermanagement.service.PolicyAssignmentService;
import org.lesek.usermanagement.service.UserService;

import java.util.Comparator;
import java.util.List;

/**
 * Lists all users; equivalent of the JavaFX {@code UserListController}.
 * Each row can be opened for edit or deleted, and shows the policies
 * currently applicable to that user (from {@link PolicyAssignmentService}'s cache).
 */
public class UserListPage extends BasePage {

    @SpringBean
    private UserService userService;

    @SpringBean
    private PolicyAssignmentService policyAssignmentService;

    public UserListPage() {
        setScreenClass("screen-user");
        add(new BookmarkablePageLink<>("createUser", UserDetailPage.class));

        List<User> users = userService.getAllUsers().stream()
                .sorted(Comparator.comparing(User::username))
                .toList();

        add(new ListView<>("userRow", users) {
            @Override
            protected void populateItem(ListItem<User> item) {
                User user = item.getModelObject();
                item.add(new Label("username", user.username()));
                item.add(new Label("fullName", user.getFullName()));
                item.add(new Label("emailAddress", user.emailAddress()));
                item.add(new Label("organizationUnit", String.join(", ", user.organizationUnit())));

                List<String> policyIds = policyAssignmentService.getPolicyIdsForUser(user.username());
                item.add(new ListView<>("policyPill", policyIds) {
                    @Override
                    protected void populateItem(ListItem<String> pillItem) {
                        pillItem.add(new Label("policyId", pillItem.getModelObject()));
                    }
                });

                PageParameters editParams = new PageParameters().add("username", user.username());
                item.add(new BookmarkablePageLink<>("edit", UserDetailPage.class, editParams));

                WebMarkupContainer deleteModal = new WebMarkupContainer("deleteModal");
                item.add(deleteModal.setOutputMarkupId(true));
                deleteModal.add(new Label("modalUsername", user.username()));
                deleteModal.add(new Link<Void>("confirmDelete") {
                    @Override
                    public void onClick() {
                        userService.deleteUser(user.username());
                        setResponsePage(UserListPage.class);
                    }
                });

                item.add(new WebMarkupContainer("deleteTrigger")
                        .add(AttributeModifier.replace("data-bs-target", "#" + deleteModal.getMarkupId())));
            }
        });
    }
}
