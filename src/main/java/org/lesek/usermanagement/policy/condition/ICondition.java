package org.lesek.usermanagement.policy.condition;

import org.lesek.usermanagement.model.User;

/**
 * A single rule a policy can be made of.
 * Implementations decide whether a given user satisfies the rule.
 */
public interface ICondition {

    boolean matches(User user);
}
