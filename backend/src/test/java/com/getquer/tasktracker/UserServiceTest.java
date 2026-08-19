package com.getquer.tasktracker;

import com.getquer.tasktracker.Entities.DepartmentEntity;
import com.getquer.tasktracker.Entities.UserEntity;
import com.getquer.tasktracker.Grades.Seniority;
import com.getquer.tasktracker.Repositories.DepartmentRepository;
import com.getquer.tasktracker.Repositories.UserRepository;
import com.getquer.tasktracker.responseDTO.UserDTO;
import com.getquer.tasktracker.service.UserService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private DepartmentRepository departmentRepository;

    @InjectMocks
    private UserService userService;

    @Test
    void getCurrentUser_ShouldReturnActualProfile() {
        DepartmentEntity department = new DepartmentEntity();
        department.setId(1L);
        department.setName("Development");

        UserEntity user = new UserEntity();
        user.setId(10L);
        user.setUsername("manager");
        user.setRole("MANAGER");
        user.setDepartment(department);
        user.setSeniority(Seniority.MIDDLE);

        when(userRepository.findByUsername("manager")).thenReturn(Optional.of(user));

        UserDTO result = userService.getCurrentUser("manager");

        assertAll(
                () -> assertEquals(10L, result.id()),
                () -> assertEquals("manager", result.username()),
                () -> assertEquals("MANAGER", result.role()),
                () -> assertEquals("Development", result.department()),
                () -> assertEquals(Seniority.MIDDLE, result.seniority())
        );
        verify(userRepository).findByUsername("manager");
    }

    @Test
    void getCurrentUser_ShouldThrow_WhenUserDoesNotExist() {
        when(userRepository.findByUsername("missing")).thenReturn(Optional.empty());

        assertThrows(
                EntityNotFoundException.class,
                () -> userService.getCurrentUser("missing")
        );
    }

    @Test
    void getAllUsers_ShouldReturnPageInDescendingIdOrder() {
        Pageable pageable = PageRequest.of(0, 3, Sort.by("id").descending());
        Page<Long> ids = new PageImpl<>(List.of(3L, 2L, 1L), pageable, 3);

        UserEntity first = createUser(1L, "first");
        UserEntity second = createUser(2L, "second");
        UserEntity third = createUser(3L, "third");

        when(userRepository.findAllUsersIds(pageable)).thenReturn(ids);
        when(userRepository.findAllUsersWithIds(ids.getContent()))
                .thenReturn(List.of(first, third, second));

        Page<UserDTO> result = userService.getAllUsers(0, 3);

        assertIterableEquals(
                List.of(3L, 2L, 1L),
                result.getContent().stream().map(UserDTO::id).toList()
        );
    }

    private UserEntity createUser(Long id, String username) {
        UserEntity user = new UserEntity();
        user.setId(id);
        user.setUsername(username);
        user.setRole("USER");
        return user;
    }
}
