package org.lesek.usermanagement.service;

import org.lesek.usermanagement.model.User;
import org.lesek.usermanagement.repository.IPolicyAssignmentRepository;
import org.lesek.usermanagement.repository.IUserRepository;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Persists a snapshot of {@link PolicyEvaluationService}'s results per user, so the policies applicable to a user (and,
 * in reverse, the users a given policy applies to) can be displayed without recomputing them on every read.
 * The snapshot is recomputed whenever a user or policy changes.
 */
@Service
public class PolicyAssignmentService {

    private final IPolicyAssignmentRepository policyAssignmentRepository;
    private final PolicyEvaluationService policyEvaluationService;
    private final IUserRepository userRepository;

    public PolicyAssignmentService(IPolicyAssignmentRepository policyAssignmentRepository,
                                    PolicyEvaluationService policyEvaluationService,
                                    IUserRepository userRepository) {
        this.policyAssignmentRepository = policyAssignmentRepository;
        this.policyEvaluationService = policyEvaluationService;
        this.userRepository = userRepository;
    }

    public List<String> getPolicyIdsForUser(String userId) {
        return policyAssignmentRepository.findPolicyIdsForUser(userId);
    }

    public List<String> getUserIdsForPolicy(String policyId) {
        return policyAssignmentRepository.findUserIdsForPolicy(policyId);
    }

    /**
     * Recomputes and persists the policy assignment for a single user.
     * Called whenever that user is added or modified.
     */
    public void recomputeForUser(User user) {
        policyAssignmentRepository.saveForUser(user.username(), policyEvaluationService.applicablePolicyIds(user));
    }

    public void removeUser(String userId) {
        policyAssignmentRepository.deleteForUser(userId);
    }

    /**
     * Recomputes and persists the policy assignment for every known user.
     * Called whenever a policy is added, modified or removed, since any of those can change which users it applies to.
     * Deleting a policy this way also purges it from every user it used to be assigned to.
     */
    public void recomputeForAllUsers() {
        userRepository.findAll().forEach(this::recomputeForUser);
    }
}
