package com.getquer.tasktracker.Repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DepartmentRepository extends JpaRepository<DepartmentRepository,Long> {

    Optional<DepartmentRepository> findByName(String name);
}
