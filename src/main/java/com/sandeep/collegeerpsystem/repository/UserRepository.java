package com.sandeep.collegeerpsystem.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sandeep.collegeerpsystem.entity.User;

// NOTE: User entity ka @Id type Integer hona chahiye (warna Long karo)
public interface UserRepository extends JpaRepository<User, Long> {

    User findByUsername(String username);

    User findByEmail(String email);
}
