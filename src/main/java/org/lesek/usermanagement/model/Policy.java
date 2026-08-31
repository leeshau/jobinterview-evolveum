package org.lesek.usermanagement.model;

import org.lesek.usermanagement.policy.condition.ICondition;

import java.io.Serializable;
import java.util.Map;

/**
 * An immutable policy definition. Which users a policy applies to is expressed purely in terms of {@link User} fields.
 * A condition key is the username of a user field, and its value is one or more operator/value assertions against that field.
 * e.g.: {@code conditions: {birthDate: {greaterThan: "2007-01-01"}}}.
 * They are turned into {@link ICondition} instances by a {@link org.lesek.usermanagement.policy.condition.ConditionFactory}.
 */
public record Policy(String id, String name, Map<String, Map<String, Object>> conditions) implements Serializable {

    public Policy {
        conditions = conditions == null ? Map.of() : Map.copyOf(conditions);
    }
}
