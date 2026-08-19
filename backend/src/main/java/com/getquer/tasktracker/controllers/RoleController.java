package com.getquer.tasktracker.controllers;

import com.getquer.tasktracker.Repositories.UserRepository;
import com.getquer.tasktracker.responseDTO.UserDTO;
import com.getquer.tasktracker.service.UserService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/roles")
public class RoleController {
    private final UserService userService;
    private final UserRepository userRepository;

    public RoleController(UserService userService, UserRepository userRepository) {
        this.userService = userService;
        this.userRepository = userRepository;
    }

    // Получить всех пользователей для таблицы админа
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<Page<UserDTO>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(userService.getAllUsers(page,size));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<UserDTO> upgradeRole(@PathVariable("id") Long id,
                                               @RequestParam("newRole") String newRole){

        UserDTO updatedUser = userService.upgradeRoleUser(id,newRole);
        return ResponseEntity.ok(updatedUser);
    }
}
