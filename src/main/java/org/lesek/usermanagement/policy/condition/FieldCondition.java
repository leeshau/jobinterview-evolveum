package org.lesek.usermanagement.policy.condition;

import org.lesek.usermanagement.model.User;

import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Matches users whose given field satisfies every assertion declared for it
 * (e.g. {@code birthDate: {greaterThan: "2007-01-01"}}).
 */
public class FieldCondition implements ICondition {

    private final Function<User, Object> accessor;
    private final List<Predicate<Object>> assertions;

    public FieldCondition(Function<User, Object> accessor, List<Predicate<Object>> assertions) {
        this.accessor = accessor;
        this.assertions = List.copyOf(assertions);
    }

    @Override
    public boolean matches(User user) {
        Object value = accessor.apply(user);
        if (value == null) {
            return false;
        }
        return assertions.stream().allMatch(assertion -> assertion.test(value));
    }
}
