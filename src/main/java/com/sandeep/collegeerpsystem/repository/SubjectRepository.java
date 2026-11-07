package com.sandeep.collegeerpsystem.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sandeep.collegeerpsystem.entity.Subject;

public interface SubjectRepository extends JpaRepository<Subject, Long> {
}
