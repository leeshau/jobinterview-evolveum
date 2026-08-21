package org.lesek.usermanagement.config;

import com.fasterxml.jackson.core.type.TypeReference;
import org.lesek.usermanagement.model.Policy;
import org.lesek.usermanagement.model.User;
import org.lesek.usermanagement.repository.JsonFileStore;
import org.lesek.usermanagement.repository.PolicyAssignmentStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Path;

/**
 * {@link JsonFileStore} is generic and needs concrete construction arguments
 * (file path, item type), so it cannot be picked up by component scanning -
 * it is wired here explicitly instead, one bean per stored type.
 */
@Configuration
public class DataStoreConfig {

    @Bean
    public JsonFileStore<User> userJsonFileStore() {
        return new JsonFileStore<>(Path.of("src", "main", "resources", "data", "users.json"), new TypeReference<>() {
        });
    }

    @Bean
    public JsonFileStore<Policy> policyJsonFileStore() {
        return new JsonFileStore<>(Path.of("src", "main", "resources", "data", "policies.json"), new TypeReference<>() {
        });
    }

    @Bean
    public PolicyAssignmentStore policyAssignmentStore() {
        return new PolicyAssignmentStore(
                Path.of("src", "main", "resources", "data", "policy-assignments.json"));
    }

    @Bean
    public JsonFileStore<String> organizationUnitJsonFileStore() {
        return new JsonFileStore<>(
                Path.of("src", "main", "resources", "data", "organization-units.json"), new TypeReference<>() {
        });
    }
}
