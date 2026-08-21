package org.lesek.usermanagement.service;

import org.lesek.usermanagement.model.User;
import org.lesek.usermanagement.repository.IUserRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Use-case facade for managing users.
 */
@Service
public class UserService {

    private final IUserRepository userRepository;
    private final PolicyEvaluationService policyEvaluationService;
    private final PolicyAssignmentService policyAssignmentService;

    public UserService(IUserRepository userRepository, PolicyEvaluationService policyEvaluationService,
                        PolicyAssignmentService policyAssignmentService) {
        this.userRepository = userRepository;
        this.policyEvaluationService = policyEvaluationService;
        this.policyAssignmentService = policyAssignmentService;
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public Optional<User> getUser(String name) {
        return userRepository.findByName(name);
    }

    public void saveUser(User user) {
        userRepository.save(user);
        policyAssignmentService.recomputeForUser(user);
    }

    public void deleteUser(String name) {
        userRepository.deleteByName(name);
        policyAssignmentService.removeUser(name);
    }

    /**
     * Policy ids currently applicable to the given user, computed on demand
     * against the current set of policies.
     */
    public List<String> getApplicablePolicies(String name) {
        return userRepository.findByName(name)
                .map(policyEvaluationService::applicablePolicyIds)
                .orElseThrow(() -> new IllegalArgumentException("Unknown user: " + name));
    }
}
