package org.lesek.usermanagement.service;

import org.lesek.usermanagement.repository.IOrganizationUnitRepository;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Use-case facade for the fixed list of organization units users can belong
 * to. Read-only - the list itself is maintained in its JSON file, not
 * through the application (@implNote can be implemented later on, not needed for now).
 */
@Service
public class OrganizationUnitService {

    private final IOrganizationUnitRepository organizationUnitRepository;

    public OrganizationUnitService(IOrganizationUnitRepository organizationUnitRepository) {
        this.organizationUnitRepository = organizationUnitRepository;
    }

    public List<String> getAllOrganizationUnits() {
        return organizationUnitRepository.findAll();
    }
}
