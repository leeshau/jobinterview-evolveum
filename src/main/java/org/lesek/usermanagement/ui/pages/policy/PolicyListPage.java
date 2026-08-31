package org.lesek.usermanagement.ui.pages.policy;

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
import org.lesek.usermanagement.model.Policy;
import org.lesek.usermanagement.service.PolicyService;

import java.util.Comparator;
import java.util.List;

/**
 * Lists all policies; equivalent of the JavaFX {@code PolicyListController}.
 */
public class PolicyListPage extends BasePage {

    @SpringBean
    private PolicyService policyService;

    public PolicyListPage() {
        setScreenClass("screen-policy");
        add(new BookmarkablePageLink<>("createPolicy", PolicyDetailPage.class));

        List<Policy> policies = policyService.getAllPolicies().stream()
                .sorted(Comparator.comparing(Policy::id))
                .toList();

        add(new ListView<>("policyRow", policies) {
            @Override
            protected void populateItem(ListItem<Policy> item) {
                Policy policy = item.getModelObject();
                item.add(new Label("id", policy.id()));
                item.add(new Label("name", policy.name()));
                item.add(new Label("conditionCount", policy.conditions().size() + " field(s)"));

                PageParameters editParams = new PageParameters().add("id", policy.id());
                item.add(new BookmarkablePageLink<>("edit", PolicyDetailPage.class, editParams));

                WebMarkupContainer deleteModal = new WebMarkupContainer("deleteModal");
                item.add(deleteModal.setOutputMarkupId(true));
                deleteModal.add(new Label("modalPolicyId", policy.id()));
                deleteModal.add(new Link<Void>("confirmDelete") {
                    @Override
                    public void onClick() {
                        policyService.deletePolicy(policy.id());
                        setResponsePage(PolicyListPage.class);
                    }
                });

                item.add(new WebMarkupContainer("deleteTrigger")
                        .add(AttributeModifier.replace("data-bs-target", "#" + deleteModal.getMarkupId())));
            }
        });
    }
}
