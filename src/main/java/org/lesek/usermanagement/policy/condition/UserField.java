package org.lesek.usermanagement.policy.condition;

import org.lesek.usermanagement.model.User;

import java.util.function.Function;

/**
 * Describes one assertable {@link User} field and its value type (which decides which operators apply).
 */
record UserField(FieldType type, Function<User, Object> accessor) {
}
