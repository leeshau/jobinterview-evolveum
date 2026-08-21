package org.lesek.usermanagement.repository;

/**
 * Implies that this repository handles data from JSON and can load from / save to it.
 */
public interface IPersistingRepository {
    void persist();
}
