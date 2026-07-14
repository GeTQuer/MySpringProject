package com.getquer.tasktracker.service;

import com.getquer.tasktracker.DTOs.UserDTO;
import com.getquer.tasktracker.Entities.DepartmentEntity;
import com.getquer.tasktracker.Entities.UserEntity;
import com.getquer.tasktracker.Repositories.DepartmentRepository;
import com.getquer.tasktracker.Repositories.UserRepository;
import com.getquer.tasktracker.request.SignupRequest;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class UserService implements UserDetailsService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private  final DepartmentRepository departmentRepository;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, DepartmentRepository departmentRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.departmentRepository = departmentRepository;
    }

    public UserDTO upgradeRoleUser(Long id,String newRole){
        UserEntity user = userRepository.findById(id).orElseThrow(()-> new EntityNotFoundException("User not founded"));
        user.setRole(newRole);
        userRepository.save(user);
        return mapToDTO(user);
    }

    public List<UserDTO> getAllUsers(){
        List<UserEntity> users = userRepository.findAll();
        List<UserDTO> usersDTO = new ArrayList<>();
        for (int i = 0; i < users.size(); i++){
            usersDTO.add(mapToDTO(users.get(i)));
        }
        return usersDTO;
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

    public void registerUser(SignupRequest request) {
        if (userRepository.findByUsername(request.username()).isPresent()){
            throw new IllegalArgumentException("Пользователь с таким username существует");
        }
        DepartmentEntity department = departmentRepository.findByName(request.department())
                .orElseThrow(()-> new IllegalArgumentException("Департамент не найден "+ request.department() ));

        UserEntity user = new UserEntity();
        user.setUsername(request.username());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRole("USER"); //новые юзеры всегда USER
        user.setDepartment(department);
        user.setSeniority(request.seniority());

        userRepository.save(user);
    }
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Пользователь не найден: " + username));
    }
}
