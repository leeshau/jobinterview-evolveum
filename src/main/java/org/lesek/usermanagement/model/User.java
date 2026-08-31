package org.lesek.usermanagement.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;

/**
 * An immutable user. The {@code username} acts as the natural identifier.
 * Which policies apply to a user is not stored here - it is computed on
 * demand by {@link org.lesek.usermanagement.service.PolicyEvaluationService}.
 */
public record User(
        String username,
        String firstName,
        String lastName,
        String emailAddress,
        List<String> organizationUnit,
        LocalDate birthDate,
        LocalDate registeredOn) implements Serializable {

    public User {
        organizationUnit = organizationUnit == null ? List.of() : List.copyOf(organizationUnit);
    }

    /**
     * @return Human-readable full name for front-end.
     */
    @JsonIgnore
    public String getFullName() {
        return String.join(" ", List.of(firstName, lastName));
    }
}
