package org.lesek.usermanagement.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.lesek.usermanagement.model.Policy;
import org.lesek.usermanagement.model.User;
import org.lesek.usermanagement.policy.PolicyMatcher;
import org.lesek.usermanagement.policy.condition.ConditionFactory;
import org.lesek.usermanagement.repository.IPolicyRepository;
import org.lesek.usermanagement.repository.IUserRepository;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PolicyEvaluationServiceTest {

    private IUserRepository userRepository;
    private IPolicyRepository policyRepository;
    private PolicyEvaluationService evaluationService;

    @BeforeEach
    void setUp() {
        userRepository = new InMemoryTestUserRepository();
        policyRepository = new InMemoryTestPolicyRepository();
        evaluationService = new PolicyEvaluationService(
                policyRepository, new PolicyMatcher(new ConditionFactory()));
    }

    private User jdoe() {
        return new User("jdoe", "John", "Doe", "jdoe@evolveum.com",
                List.of("Software Development", "Support"),
                LocalDate.now().minusYears(10), LocalDate.of(2024, 5, 7));
    }

    @Test
    void computesAllApplicablePoliciesForUser() {
        policyRepository.save(new Policy("underaged", "Underaged User",
                Map.of("birthDate", Map.of("greaterThan", "2007-01-01"))));
        policyRepository.save(new Policy("internal-user", "Internal User",
                Map.of("emailAddress", Map.of("equals", "jdoe@evolveum.com"))));
        policyRepository.save(new Policy("named-john", "Named John",
                Map.of("firstName", Map.of("equals", "John"))));

        List<String> applicablePolicyIds = evaluationService.applicablePolicyIds(jdoe());

        assertEquals(3, applicablePolicyIds.size());
        assertTrue(applicablePolicyIds.containsAll(
                List.of("underaged", "internal-user", "named-john")));
    }

    @Test
    void computesOnlyMatchingPolicies() {
        policyRepository.save(new Policy("internal-user", "Internal User",
                Map.of("emailAddress", Map.of("equals", "jdoe@evolveum.com"))));
        policyRepository.save(new Policy("named-jane", "Named Jane",
                Map.of("firstName", Map.of("equals", "Jane"))));

        List<String> applicablePolicyIds = evaluationService.applicablePolicyIds(jdoe());

        assertEquals(List.of("internal-user"), applicablePolicyIds);
    }

    @Test
    void reflectsPoliciesAddedAfterTheFactWithoutAnyRecompute() {
        userRepository.save(jdoe());
        List<String> beforePolicy = evaluationService.applicablePolicyIds(jdoe());
        assertEquals(List.of(), beforePolicy);

        policyRepository.save(new Policy("internal-user", "Internal User",
                Map.of("emailAddress", Map.of("equals", "jdoe@evolveum.com"))));

        List<String> afterPolicy = evaluationService.applicablePolicyIds(jdoe());
        assertEquals(List.of("internal-user"), afterPolicy);
    }

    private static class InMemoryTestUserRepository implements IUserRepository {
        private final Map<String, User> users = new LinkedHashMap<>();

        @Override
        public List<User> findAll() {
            return List.copyOf(users.values());
        }

        @Override
        public Optional<User> findByName(String name) {
            return Optional.ofNullable(users.get(name));
        }

        @Override
        public void save(User user) {
            users.put(user.username(), user);
        }

        @Override
        public void deleteByName(String name) {
            users.remove(name);
        }

        @Override
        public void persist() {
            // not implemented for tests
        }
    }

    private static class InMemoryTestPolicyRepository implements IPolicyRepository {
        private final Map<String, Policy> policies = new LinkedHashMap<>();

        @Override
        public List<Policy> findAll() {
            return List.copyOf(policies.values());
        }

        @Override
        public Optional<Policy> findById(String id) {
            return Optional.ofNullable(policies.get(id));
        }

        @Override
        public void save(Policy policy) {
            policies.put(policy.id(), policy);
        }

        @Override
        public void deleteById(String id) {
            policies.remove(id);
        }

        @Override
        public void persist() {
            // not implemented for tests
        }
    }
}
