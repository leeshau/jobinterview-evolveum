package org.lesek.usermanagement.repository;

import org.lesek.usermanagement.model.User;
import org.springframework.stereotype.Repository;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Keeps users in memory, backed by a {@link JsonFileStore} for persistence across application restarts.
 */
@Repository
public class UserRepository implements IUserRepository {

    private final JsonFileStore<User> store;
    private final Map<String, User> usersByName = new LinkedHashMap<>();

    public UserRepository(JsonFileStore<User> store) {
        this.store = store;
        store.load().forEach(user -> usersByName.put(user.username(), user));
    }

    @Override
    public List<User> findAll() {
        return List.copyOf(usersByName.values());
    }

    @Override
    public Optional<User> findByName(String name) {
        return Optional.ofNullable(usersByName.get(name));
    }

    @Override
    public void save(User user) {
        usersByName.put(user.username(), user);
        persist();
    }

    @Override
    public void deleteByName(String name) {
        usersByName.remove(name);
        persist();
    }

    @Override
    public void persist() {
        store.save(List.copyOf(usersByName.values()));
    }
}
