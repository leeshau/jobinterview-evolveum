package org.lesek.usermanagement.service;

import org.lesek.usermanagement.model.Policy;
import org.lesek.usermanagement.model.User;
import org.lesek.usermanagement.policy.PolicyMatcher;
import org.lesek.usermanagement.repository.IPolicyRepository;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Determines which policies currently apply to a user.
 */
@Service
public class PolicyEvaluationService {

    private final IPolicyRepository policyRepository;
    private final PolicyMatcher policyMatcher;

    public PolicyEvaluationService(IPolicyRepository policyRepository, PolicyMatcher policyMatcher) {
        this.policyRepository = policyRepository;
        this.policyMatcher = policyMatcher;
    }

    public List<String> applicablePolicyIds(User user) {
        return policyRepository.findAll().stream()
                .filter(policy -> policyMatcher.matches(policy, user))
                .map(Policy::id)
                .toList();
    }
}
