package org.lesek.usermanagement.policy.condition;

import java.time.LocalDate;
import java.util.List;
import java.util.function.Predicate;

/**
 * The value types a user field can have for policy matching purposes.
 * This class defines for what object type what assertion can be made.
 */
public enum FieldType {

    STRING(List.of(Constants.EQUALS, Constants.NOT_EQUALS, Constants.CONTAINS, Constants.NOT_CONTAINS)) {
        @Override
        Predicate<Object> buildAssertion(String operator, Object rawValue) {
            String expected = (String) rawValue;
            return switch (operator) {
                case Constants.EQUALS -> expected::equals;
                case Constants.NOT_EQUALS -> actual -> !expected.equals(actual);
                case Constants.CONTAINS -> actual -> ((String) actual).contains(expected);
                case Constants.NOT_CONTAINS -> actual -> !((String) actual).contains(expected);
                default -> throw UnsupportedOperatorException(operator);
            };
        }
    },

    NUMBER(List.of(Constants.EQUALS, Constants.GREATER_THAN, Constants.LESS_THAN)) {
        @Override
        Predicate<Object> buildAssertion(String operator, Object rawValue) {
            double expected = ((Number) rawValue).doubleValue();
            return switch (operator) {
                case Constants.EQUALS -> actual -> ((Number) actual).doubleValue() == expected;
                case Constants.GREATER_THAN -> actual -> ((Number) actual).doubleValue() > expected;
                case Constants.LESS_THAN -> actual -> ((Number) actual).doubleValue() < expected;
                default -> throw UnsupportedOperatorException(operator);
            };
        }
    },

    DATE(List.of(Constants.EQUALS, Constants.GREATER_THAN, Constants.LESS_THAN)) {
        @Override
        Predicate<Object> buildAssertion(String operator, Object rawValue) {
            LocalDate expected = LocalDate.parse((String) rawValue);
            return switch (operator) {
                case Constants.EQUALS -> expected::equals;
                case Constants.GREATER_THAN -> actual -> ((LocalDate) actual).isAfter(expected);
                case Constants.LESS_THAN -> actual -> ((LocalDate) actual).isBefore(expected);
                default -> throw UnsupportedOperatorException(operator);
            };
        }
    },

    BOOLEAN(List.of(Constants.EQUALS)) {
        @Override
        Predicate<Object> buildAssertion(String operator, Object rawValue) {
            if (!Constants.EQUALS.equals(operator)) {
                throw UnsupportedOperatorException(operator);
            }
            boolean expected = (Boolean) rawValue;
            return actual -> expected == (Boolean) actual;
        }
    };

    private final List<String> supportedOperators;

    FieldType(List<String> supportedOperators) {
        this.supportedOperators = supportedOperators;
    }

    public List<String> supportedOperators() {
        return supportedOperators;
    }

    abstract Predicate<Object> buildAssertion(String operator, Object rawValue);

    private static IllegalArgumentException UnsupportedOperatorException(String operator) {
        return new IllegalArgumentException("Unsupported operator: " + operator);
    }

    private static class Constants {
        public static final String EQUALS = "equals";
        public static final String NOT_EQUALS = "notEquals";
        public static final String CONTAINS = "contains";
        public static final String NOT_CONTAINS = "notContains";
        public static final String GREATER_THAN = "greaterThan";
        public static final String LESS_THAN = "lessThan";
    }
}
