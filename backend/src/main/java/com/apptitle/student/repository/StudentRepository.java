package com.apptitle.student.repository;

import com.apptitle.student.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface StudentRepository extends JpaRepository<Student, UUID> {

    Optional<Student> findByUserId(UUID userId);

    boolean existsByLrn(String lrn);
}
