package com.sandeep.collegeerpsystem.repository;

import com.sandeep.collegeerpsystem.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface StudentRepository extends JpaRepository<Student, Integer> {
    Optional<Student> findByUserId(Integer userId);
}