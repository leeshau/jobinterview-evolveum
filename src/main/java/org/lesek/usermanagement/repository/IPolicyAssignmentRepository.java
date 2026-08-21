package org.lesek.usermanagement.repository;

import java.util.List;

/**
 * Persisted snapshot of which policies apply to which users - a
 * non-exclusive many-to-many relation.
 * e.g.: a user can have any number of policies at once.
 */
public interface IPolicyAssignmentRepository extends IPersistingRepository {

    List<String> findPolicyIdsForUser(String userId);

    List<String> findUserIdsForPolicy(String policyId);

    void saveForUser(String userId, List<String> policyIds);

    void deleteForUser(String userId);
}
