package com.getquer.tasktracker.service;

import com.getquer.tasktracker.responseDTO.AssigneeDTO;
import com.getquer.tasktracker.responseDTO.UserDTO;
import com.getquer.tasktracker.Entities.DepartmentEntity;
import com.getquer.tasktracker.Entities.UserEntity;
import com.getquer.tasktracker.Repositories.DepartmentRepository;
import com.getquer.tasktracker.Repositories.UserRepository;
import com.getquer.tasktracker.requestDTO.SignupRequest;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.*;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class UserService implements UserDetailsService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private  final DepartmentRepository departmentRepository;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, DepartmentRepository departmentRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.departmentRepository = departmentRepository;
    }

    @Transactional
    public UserDTO upgradeRoleUser(Long id,String newRole){
        UserEntity user = userRepository.findById(id).orElseThrow(()-> new EntityNotFoundException("User not founded"));
        user.setRole(newRole);
        userRepository.save(user);
        return mapToDTO(user);
    }

    public Page<UserDTO> getAllUsers(int page,int size){
        Pageable pageable = PageRequest.of(page,size, Sort.by("id").descending());
        Page<Long> idPages = userRepository.findAllUsersIds(pageable);
        return convertToUserPage(idPages,pageable);
    }

    private UserDTO mapToDTO(UserEntity userEntity){
        return new UserDTO(
                userEntity.getId(),
                userEntity.getUsername(),
                userEntity.getRole(),
                userEntity.getDepartment() != null ? userEntity.getDepartment().getName() : "Без отдела",
                userEntity.getSeniority()

        );
    }

    private Page<UserDTO> convertToUserPage(Page<Long> idPage, Pageable pageable){
        if (idPage.isEmpty())
            return Page.empty(pageable);

        List<Long> ids = idPage.getContent();
        List<UserEntity> users = userRepository.findAllUsersWithIds(ids);

        List<UserDTO> dtos = users.stream()
                .sorted((u1,u2)->u2.getId().compareTo(u1.getId()))
                .map(this::mapToDTO)
                .toList();

        return new PageImpl<>(dtos,pageable,idPage.getTotalElements());
    }

    public Page<UserDTO> findAllUsersByDepartment(String managerUsername, int page, int size){
        UserEntity manager = userRepository.findByUsername(managerUsername)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        if (manager.getDepartment() == null){
            throw new RuntimeException("Manager has no department assigned");
        }
        Long departmentId = manager.getDepartment().getId();
        Pageable pageable = PageRequest.of(page,size, Sort.by("id").descending());
        Page<Long> idPage = userRepository.findAllUsersByDepartmentId(departmentId,pageable);
        return convertToUserPage(idPage,pageable);
    }

    public List<AssigneeDTO> getAssignableUsers(String managerUsername) {
        UserEntity manager = userRepository.findByUsername(managerUsername)
                .orElseThrow(() ->
                        new EntityNotFoundException("Manager not found")
                );

        if (manager.getDepartment() == null) {
            return List.of();
        }

        return userRepository
                .findAllByDepartment_IdAndRoleOrderByUsernameAsc(
                        manager.getDepartment().getId(),
                        "USER"
                )
                .stream()
                .map(user -> new AssigneeDTO(
                        user.getId(),
                        user.getUsername()
                ))
                .toList();
    }

    @Transactional
    public void registerUser(SignupRequest request) {
        if (userRepository.findByUsername(request.username()).isPresent()){
            throw new IllegalArgumentException("Пользователь с таким username существует");
        }
        DepartmentEntity department = departmentRepository.findByName(request.department())
                .orElseThrow(()-> new IllegalArgumentException("Департамент не найден "+ request.department()));

        UserEntity user = new UserEntity();
        user.setUsername(request.username());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRole("USER"); //новые юзеры всегда USER
        user.setDepartment(department);
        user.setSeniority(request.seniority());

        userRepository.save(user);
    }
    public UserDTO getCurrentUser(String username) {
        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + username));
        return mapToDTO(user);
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Пользователь не найден: " + username));
    }
}
