package org.lesek.usermanagement.repository;

import org.springframework.stereotype.Repository;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Keeps the user-to-policies assignment snapshot in memory, backed by a
 * {@link PolicyAssignmentStore} for persistence across application
 * restarts.
 */
@Repository
public class PolicyAssignmentRepository implements IPolicyAssignmentRepository {

    private final PolicyAssignmentStore store;
    private final Map<String, List<String>> policyIdsByUserId;

    public PolicyAssignmentRepository(PolicyAssignmentStore store) {
        this.store = store;
        this.policyIdsByUserId = new LinkedHashMap<>(store.load());
    }

    @Override
    public List<String> findPolicyIdsForUser(String userId) {
        return List.copyOf(policyIdsByUserId.getOrDefault(userId, List.of()));
    }

    @Override
    public List<String> findUserIdsForPolicy(String policyId) {
        return policyIdsByUserId.entrySet().stream()
                .filter(entry -> entry.getValue().contains(policyId))
                .map(Map.Entry::getKey)
                .toList();
    }

    @Override
    public void saveForUser(String userId, List<String> policyIds) {
        if (policyIds.isEmpty()) {
            policyIdsByUserId.remove(userId);
        } else {
            policyIdsByUserId.put(userId, List.copyOf(policyIds));
        }
        persist();
    }

    @Override
    public void deleteForUser(String userId) {
        policyIdsByUserId.remove(userId);
        persist();
    }

    @Override
    public void persist() {
        store.save(Map.copyOf(policyIdsByUserId));
    }
}
