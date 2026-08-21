package org.lesek.usermanagement.repository;

import org.lesek.usermanagement.model.Policy;
import org.springframework.stereotype.Repository;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Keeps policies in memory, backed by a {@link JsonFileStore} for persistence across application restarts.
 */
@Repository
public class PolicyRepository implements IPolicyRepository {

    private final JsonFileStore<Policy> store;
    private final Map<String, Policy> policiesById = new LinkedHashMap<>();

    public PolicyRepository(JsonFileStore<Policy> store) {
        this.store = store;
        store.load().forEach(policy -> policiesById.put(policy.id(), policy));
    }

    @Override
    public List<Policy> findAll() {
        return List.copyOf(policiesById.values());
    }

    @Override
    public Optional<Policy> findById(String id) {
        return Optional.ofNullable(policiesById.get(id));
    }

    @Override
    public void save(Policy policy) {
        policiesById.put(policy.id(), policy);
        persist();
    }

    @Override
    public void deleteById(String id) {
        policiesById.remove(id);
        persist();
    }

    @Override
    public void persist() {
        store.save(List.copyOf(policiesById.values()));
    }
}
