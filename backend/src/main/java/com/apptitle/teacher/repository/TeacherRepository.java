package com.apptitle.teacher.repository;

import com.apptitle.teacher.entity.Teacher;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TeacherRepository extends JpaRepository<Teacher, UUID> {

    Optional<Teacher> findByUserId(UUID userId);
}
