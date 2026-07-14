package com.getquer.tasktracker.Repositories;

import com.getquer.tasktracker.Entities.DepartmentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DepartmentRepository extends JpaRepository<DepartmentEntity,Long> {

    Optional<DepartmentEntity> findByName(String name);
}
