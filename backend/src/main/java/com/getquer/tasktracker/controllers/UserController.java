package com.getquer.tasktracker.controllers;


import com.getquer.tasktracker.responseDTO.AssigneeDTO;
import com.getquer.tasktracker.responseDTO.UserDTO;
import com.getquer.tasktracker.service.UserService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }


    @GetMapping("/me")
    public ResponseEntity<UserDTO> getCurrentUser(Authentication authentication) {
        return ResponseEntity.ok(
                userService.getCurrentUser(authentication.getName())
        );
    }

    @PreAuthorize("hasRole('MANAGER')")
    @GetMapping("/department")
    public ResponseEntity<Page<UserDTO>> getAllUsersForManager(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication
    ){
        String username = authentication.getName();
        return ResponseEntity.ok(userService.findAllUsersByDepartment(username,page,size));
    }

    @PreAuthorize("hasRole('MANAGER')")
    @GetMapping("/assignable")
    public ResponseEntity<List<AssigneeDTO>> getAssignableUsers(
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                userService.getAssignableUsers(authentication.getName())
        );
    }

}
