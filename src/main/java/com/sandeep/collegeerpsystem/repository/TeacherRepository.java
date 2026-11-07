package com.sandeep.collegeerpsystem.repository;

import com.sandeep.collegeerpsystem.entity.Teacher;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface TeacherRepository extends JpaRepository<Teacher, Integer> {

    Optional<Teacher> findByUserId(Integer userId);

    Optional<Teacher> findByEmployeeCode(String employeeCode);
}
