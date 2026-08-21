package org.lesek.usermanagement.repository;

import org.lesek.usermanagement.model.Policy;

import java.util.List;
import java.util.Optional;

public interface IPolicyRepository extends IPersistingRepository {

    List<Policy> findAll();

    Optional<Policy> findById(String id);

    void save(Policy policy);

    void deleteById(String id);
}
