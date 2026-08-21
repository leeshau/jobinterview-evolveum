package org.lesek.usermanagement.repository;

import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Loads the organization units once at startup from a {@link JsonFileStore}.
 * Read-only by design - the list is edited by hand in its JSON file, not
 * through the application (@implNote can be implemented later on, not needed for now).
 */
@Repository
public class OrganizationUnitRepository implements IOrganizationUnitRepository {

    private final List<String> organizationUnits;

    public OrganizationUnitRepository(JsonFileStore<String> store) {
        this.organizationUnits = List.copyOf(store.load());
    }

    @Override
    public List<String> findAll() {
        return organizationUnits;
    }
}
