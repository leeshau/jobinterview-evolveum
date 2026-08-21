package org.lesek.usermanagement.repository;

import org.lesek.usermanagement.model.User;

import java.util.List;
import java.util.Optional;

public interface IUserRepository extends IPersistingRepository {

    List<User> findAll();

    Optional<User> findByName(String name);

    void save(User user);

    void deleteByName(String name);

}
