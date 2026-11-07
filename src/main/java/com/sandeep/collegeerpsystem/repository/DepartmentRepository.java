package com.sandeep.collegeerpsystem.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sandeep.collegeerpsystem.entity.Department;

// NOTE: Department entity ka @Id type Integer hona chahiye (warna Long karo)
public interface DepartmentRepository extends JpaRepository<Department, Long> {
}
