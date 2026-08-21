package org.lesek.usermanagement;

import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Boot composition root. Enables component scanning across this
 * package (repositories, services, policy matching, config) so beans are
 * wired via constructor injection instead of being built by hand.
 */
@SpringBootApplication
public class UserManagementApplication {
}
