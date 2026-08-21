package org.lesek.usermanagement.ui;

import org.lesek.usermanagement.policy.condition.ConditionFactory;
import org.lesek.usermanagement.service.OrganizationUnitService;
import org.lesek.usermanagement.service.PolicyAssignmentService;
import org.lesek.usermanagement.service.PolicyEvaluationService;
import org.lesek.usermanagement.service.PolicyService;
import org.lesek.usermanagement.service.UserService;

/**
 * The backend services FXML controllers may need, bundled together so
 * {@link org.lesek.usermanagement.ui.navigation.SceneNavigator} can inject
 * them into any controller without knowing about individual services.
 */
public record AppContext(UserService userService, PolicyService policyService,
                          PolicyEvaluationService policyEvaluationService,
                          PolicyAssignmentService policyAssignmentService,
                          ConditionFactory conditionFactory,
                          OrganizationUnitService organizationUnitService) {
}
