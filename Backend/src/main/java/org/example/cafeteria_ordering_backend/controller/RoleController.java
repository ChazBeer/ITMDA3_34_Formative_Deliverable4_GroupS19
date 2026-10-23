package org.example.cafeteria_ordering_backend.controller;

import lombok.RequiredArgsConstructor;
import org.example.cafeteria_ordering_backend.entity.Role;
import org.example.cafeteria_ordering_backend.service.interfaces.RoleService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// Handles API requests related to user roles.
@RestController
@RequestMapping("/api/roles")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;

    // Retrieve all available roles. Only administrators can access this endpoint.
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Role>> getAllRoles () {
        return ResponseEntity.ok(roleService.getAllRoles());
    }
}