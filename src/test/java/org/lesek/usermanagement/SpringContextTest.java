package org.lesek.usermanagement;

import org.junit.jupiter.api.Test;
import org.lesek.usermanagement.policy.PolicyMatcher;
import org.lesek.usermanagement.repository.IPolicyRepository;
import org.lesek.usermanagement.repository.IUserRepository;
import org.lesek.usermanagement.service.PolicyEvaluationService;
import org.lesek.usermanagement.service.PolicyService;
import org.lesek.usermanagement.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(classes = UserManagementApplication.class)
class SpringContextTest {

    @Autowired
    private IUserRepository userRepository;

    @Autowired
    private IPolicyRepository policyRepository;

    @Autowired
    private PolicyMatcher policyMatcher;

    @Autowired
    private PolicyEvaluationService policyEvaluationService;

    @Autowired
    private UserService userService;

    @Autowired
    private PolicyService policyService;

    @Test
    void contextWiresAllBeansViaConstructorInjection() {
        assertNotNull(userRepository);
        assertNotNull(policyRepository);
        assertNotNull(policyMatcher);
        assertNotNull(policyEvaluationService);
        assertNotNull(userService);
        assertNotNull(policyService);
    }
}
