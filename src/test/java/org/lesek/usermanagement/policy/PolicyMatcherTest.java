package org.lesek.usermanagement.policy;

import org.junit.jupiter.api.Test;
import org.lesek.usermanagement.model.Policy;
import org.lesek.usermanagement.model.User;
import org.lesek.usermanagement.policy.condition.ConditionFactory;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PolicyMatcherTest {

    private final PolicyMatcher matcher = new PolicyMatcher(new ConditionFactory());

    private User jdoe() {
        return new User("jdoe", "John", "Doe", "jdoe@evolveum.com",
                List.of("Software Development", "Support"),
                LocalDate.now().minusYears(10), LocalDate.of(2024, 5, 7));
    }

    @Test
    void matchesUnderagedPolicy() {
        Policy underaged = new Policy("underaged", "Underaged User",
                Map.of("birthDate", Map.of("greaterThan", "2007-01-01")));

        assertTrue(matcher.matches(underaged, jdoe()));
    }

    @Test
    void matchesInternalUserPolicy() {
        Policy internalUser = new Policy("internal-user", "Internal User",
                Map.of("emailAddress", Map.of("equals", "jdoe@evolveum.com")));

        assertTrue(matcher.matches(internalUser, jdoe()));
    }

    @Test
    void doesNotMatchWhenEmailDiffers() {
        Policy internalUser = new Policy("internal-user", "Internal User",
                Map.of("emailAddress", Map.of("equals", "someone@example.com")));

        assertFalse(matcher.matches(internalUser, jdoe()));
    }

    @Test
    void matchesUsingNotEquals() {
        Policy externalUser = new Policy("external-user", "External User",
                Map.of("emailAddress", Map.of("notEquals", "someone@example.com")));

        assertTrue(matcher.matches(externalUser, jdoe()));
    }

    @Test
    void policyWithMultipleConditionsRequiresAllToMatch() {
        Policy combined = new Policy("combined", "Combined", Map.of(
                "emailAddress", Map.of("equals", "jdoe@evolveum.com"),
                "firstName", Map.of("equals", "Jane")));

        assertFalse(matcher.matches(combined, jdoe()));
    }

    @Test
    void policyWithMultipleAssertionsOnSameFieldRequiresAllToMatch() {
        Policy bornInRange = new Policy("born-in-range", "Born In Range", Map.of(
                "birthDate", Map.of("greaterThan", "2000-01-01", "lessThan", "2020-01-01")));

        assertTrue(matcher.matches(bornInRange, jdoe()));
    }

    /**
     * Registered more than 20 years ago, not a member of the CEO organization unit, and has an evolveum.com email address.
     */
    private Policy mammoth() {
        return new Policy("mammoth", "Mammoth", Map.of(
                "registeredOn", Map.of("lessThan", LocalDate.now().minusYears(20).toString()),
                "organizationUnit", Map.of("notContains", "CEO"),
                "emailAddress", Map.of("contains", "@evolveum.com")));
    }

    private User veteranEmployee() {
        return new User("mgorski", "Marek", "Gorski", "mgorski@evolveum.com",
                List.of("Software Development"),
                LocalDate.of(1980, 5, 20), LocalDate.now().minusYears(21));
    }

    @Test
    void matchesMammothPolicy() {
        assertTrue(matcher.matches(mammoth(), veteranEmployee()));
    }

    @Test
    void mammothDoesNotMatchRecentlyRegisteredUser() {
        User recentlyRegistered = new User("mgorski", "Marek", "Gorski", "mgorski@evolveum.com",
                List.of("Software Development"), LocalDate.of(1980, 5, 20), LocalDate.now().minusYears(1));

        assertFalse(matcher.matches(mammoth(), recentlyRegistered));
    }

    @Test
    void mammothDoesNotMatchCeoOrganizationUnit() {
        User ceo = new User("mgorski", "Marek", "Gorski", "mgorski@evolveum.com",
                List.of("CEO"), LocalDate.of(1980, 5, 20), LocalDate.now().minusYears(21));

        assertFalse(matcher.matches(mammoth(), ceo));
    }

    @Test
    void mammothDoesNotMatchNonEvolveumEmail() {
        User outsider = new User("mgorski", "Marek", "Gorski", "mgorski@example.com",
                List.of("Software Development"), LocalDate.of(1980, 5, 20), LocalDate.now().minusYears(21));

        assertFalse(matcher.matches(mammoth(), outsider));
    }
}
