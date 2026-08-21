package org.lesek.usermanagement.policy.condition;

import org.lesek.usermanagement.model.User;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Builds an {@link ICondition} for a single user field from the operator(s)
 * declared against it in a policy, e.g. {@code birthDate: {greaterThan: "2007-01-01"}}.
 * <p>
 * New assertable fields can be plugged in via {@link #registerField} dynamically.
 */
@Component
public class ConditionFactory {

    private final Map<String, UserField> fields = new LinkedHashMap<>();

    /**
     * Build on existing user fields, since these are going to be used anyway.
     */
    public ConditionFactory() {
        registerField("username", FieldType.STRING, User::username);
        registerField("firstName", FieldType.STRING, User::firstName);
        registerField("lastName", FieldType.STRING, User::lastName);
        registerField("emailAddress", FieldType.STRING, User::emailAddress);
        registerField("organizationUnit", FieldType.STRING,
                user -> String.join(",", user.organizationUnit()));
        registerField("birthDate", FieldType.DATE, User::birthDate);
        registerField("registeredOn", FieldType.DATE, User::registeredOn);
    }

    public void registerField(String fieldName, FieldType type, Function<User, Object> accessor) {
        fields.put(fieldName, new UserField(type, accessor));
    }

    /**
     * The user fields policies can be conditioned on, and their type -
     * used to build a policy condition editor without exposing the accessors themselves.
     */
    public Map<String, FieldType> availableFields() {
        Map<String, FieldType> result = new LinkedHashMap<>();
        fields.forEach((fieldName, field) -> result.put(fieldName, field.type()));
        return result;
    }

    public ICondition create(String fieldName, Map<String, Object> operators) {
        UserField field = fields.get(fieldName);
        if (field == null) {
            throw new IllegalArgumentException("Unknown policy field: " + fieldName);
        }
        List<Predicate<Object>> assertions = operators.entrySet().stream()
                .map(entry -> field.type().buildAssertion(entry.getKey(), entry.getValue()))
                .toList();
        return new FieldCondition(field.accessor(), assertions);
    }
}
