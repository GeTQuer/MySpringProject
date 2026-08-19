package com.getquer.tasktracker.Repositories;

import com.getquer.tasktracker.Entities.UserEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, Long> {
    
    Optional<UserEntity> findByUsername(String username);

    @Query(
            value = "Select u.id FROM UserEntity u JOIN u.department d WHERE d.id = :id and u.role = 'USER'",
            countQuery = "SELECT COUNT(u) FROM UserEntity u JOIN u.department d WHERE d.id = :id and u.role = 'USER'"
    )
    Page<Long> findAllUsersByDepartmentId(@Param("id") Long id, Pageable pageable);

    @Query("SELECT u FROM UserEntity u LEFT JOIN FETCH u.department WHERE u.id IN :ids")
    List<UserEntity> findAllUsersWithIds(@Param("ids") List<Long> ids);

    @Query(
            value = "SELECT u.id FROM UserEntity u",
            countQuery = "SELECT  COUNT(*) FROM UserEntity u"
    )
    Page<Long> findAllUsersIds(Pageable pageable);

    List<UserEntity> findAllByDepartment_IdAndRoleOrderByUsernameAsc(
            Long departmentId,
            String role
    );

}
