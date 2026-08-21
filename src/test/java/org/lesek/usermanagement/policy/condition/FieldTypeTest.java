package org.lesek.usermanagement.policy.condition;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FieldTypeTest {

    @Test
    void stringEqualsAndNotEquals() {
        Predicate<Object> equals = FieldType.STRING.buildAssertion("equals", "Doe");
        assertTrue(equals.test("Doe"));
        assertFalse(equals.test("Smith"));

        Predicate<Object> notEquals = FieldType.STRING.buildAssertion("notEquals", "Doe");
        assertFalse(notEquals.test("Doe"));
        assertTrue(notEquals.test("Smith"));
    }

    @Test
    void stringContains() {
        Predicate<Object> contains = FieldType.STRING.buildAssertion("contains", "evolveum.com");
        assertTrue(contains.test("jdoe@evolveum.com"));
        assertFalse(contains.test("jdoe@example.com"));
    }

    @Test
    void stringNotContains() {
        Predicate<Object> notContains = FieldType.STRING.buildAssertion("notContains", "CEO");
        assertTrue(notContains.test("Software Development,Support"));
        assertFalse(notContains.test("CEO,Support"));
    }

    @Test
    void numberComparisons() {
        assertTrue(FieldType.NUMBER.buildAssertion("equals", 18).test(18));
        assertTrue(FieldType.NUMBER.buildAssertion("greaterThan", 18).test(19));
        assertFalse(FieldType.NUMBER.buildAssertion("greaterThan", 18).test(18));
        assertTrue(FieldType.NUMBER.buildAssertion("lessThan", 18).test(17));
        assertFalse(FieldType.NUMBER.buildAssertion("lessThan", 18).test(18));
    }

    @Test
    void dateComparisons() {
        LocalDate reference = LocalDate.of(2007, 1, 1);
        assertTrue(FieldType.DATE.buildAssertion("equals", "2007-01-01").test(reference));
        assertTrue(FieldType.DATE.buildAssertion("greaterThan", "2007-01-01").test(reference.plusDays(1)));
        assertFalse(FieldType.DATE.buildAssertion("greaterThan", "2007-01-01").test(reference));
        assertTrue(FieldType.DATE.buildAssertion("lessThan", "2007-01-01").test(reference.minusDays(1)));
        assertFalse(FieldType.DATE.buildAssertion("lessThan", "2007-01-01").test(reference));
    }

    @Test
    void booleanEquals() {
        assertTrue(FieldType.BOOLEAN.buildAssertion("equals", true).test(true));
        assertFalse(FieldType.BOOLEAN.buildAssertion("equals", true).test(false));
        assertTrue(FieldType.BOOLEAN.buildAssertion("equals", false).test(false));
    }

    @Test
    void unsupportedOperatorThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> FieldType.STRING.buildAssertion("greaterThan", "Doe"));
        assertThrows(IllegalArgumentException.class,
                () -> FieldType.BOOLEAN.buildAssertion("notEquals", true));
    }
}
