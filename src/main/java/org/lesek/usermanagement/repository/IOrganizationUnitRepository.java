package org.lesek.usermanagement.repository;

import java.util.List;

/**
 * The organization units a user can belong to - a fixed reference list maintained in its JSON file directly,
 * not through the application (@implNote can be implemented later on, not needed for now).
 */
public interface IOrganizationUnitRepository {

    List<String> findAll();
}
