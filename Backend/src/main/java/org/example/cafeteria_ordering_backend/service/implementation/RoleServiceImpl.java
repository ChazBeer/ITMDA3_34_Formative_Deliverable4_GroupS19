package org.example.cafeteria_ordering_backend.service.implementation;

import org.example.cafeteria_ordering_backend.entity.Role;
import org.example.cafeteria_ordering_backend.repository.RoleRepository;
import org.example.cafeteria_ordering_backend.service.interfaces.RoleService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class RoleServiceImpl implements RoleService {

    private final RoleRepository roleRepository;

    // Injects the repository used to retrieve role information.
    public RoleServiceImpl (RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    // Retrieves all roles from the database without modifying any data.
    @Override
    @Transactional(readOnly = true)
    public List<Role> getAllRoles () {
        return roleRepository.findAll();
    }
}