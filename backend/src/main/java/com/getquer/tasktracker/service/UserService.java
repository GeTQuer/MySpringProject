package com.getquer.tasktracker.service;

import com.getquer.tasktracker.DTOs.UserDTO;
import com.getquer.tasktracker.Entities.UserEntity;
import com.getquer.tasktracker.Repositories.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class UserService {
    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
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
                userEntity.getRole()
        );
    }



}
