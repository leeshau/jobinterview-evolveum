package org.lesek.usermanagement.service;

import org.lesek.usermanagement.model.Policy;
import org.lesek.usermanagement.repository.IPolicyRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Use-case facade for managing policies.
 */
@Service
public class PolicyService {

    private final IPolicyRepository policyRepository;
    private final PolicyAssignmentService policyAssignmentService;

    public PolicyService(IPolicyRepository policyRepository, PolicyAssignmentService policyAssignmentService) {
        this.policyRepository = policyRepository;
        this.policyAssignmentService = policyAssignmentService;
    }

    public List<Policy> getAllPolicies() {
        return policyRepository.findAll();
    }

    public Optional<Policy> getPolicy(String id) {
        return policyRepository.findById(id);
    }

    public void savePolicy(Policy policy) {
        policyRepository.save(policy);
        policyAssignmentService.recomputeForAllUsers();
    }

    public void deletePolicy(String id) {
        policyRepository.deleteById(id);
        policyAssignmentService.recomputeForAllUsers();
    }
}
