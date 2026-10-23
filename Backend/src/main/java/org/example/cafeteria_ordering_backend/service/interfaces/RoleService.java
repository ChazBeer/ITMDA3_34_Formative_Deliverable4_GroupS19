package org.example.cafeteria_ordering_backend.service.interfaces;

import  org.example.cafeteria_ordering_backend.entity.Role;

import java.util.List;

public interface RoleService {

    // Retrieves all available user roles.
    List<Role> getAllRoles ();
}