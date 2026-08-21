package org.lesek.usermanagement.policy;

import org.lesek.usermanagement.model.Policy;
import org.lesek.usermanagement.model.User;
import org.lesek.usermanagement.policy.condition.ICondition;
import org.lesek.usermanagement.policy.condition.ConditionFactory;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Decides whether a policy applies to a user.
 * A policy applies when the user satisfies every condition it declares.
 */
@Component
public class PolicyMatcher {

    private final ConditionFactory conditionFactory;

    public PolicyMatcher(ConditionFactory conditionFactory) {
        this.conditionFactory = conditionFactory;
    }

    public boolean matches(Policy policy, User user) {
        return policy.conditions().entrySet().stream()
                .map(entry -> conditionFactory.create(entry.getKey(), entry.getValue()))
                .allMatch(condition -> condition.matches(user));
    }
}
