package com.sandeep.collegeerpsystem.repository;

import com.sandeep.collegeerpsystem.entity.Fee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;

import jakarta.persistence.LockModeType;
import java.util.Optional;

public interface FeeRepository extends JpaRepository<Fee, Integer> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT f FROM Fee f WHERE f.feeId = :feeId")
    Optional<Fee> findByIdForUpdate(@Param("feeId") Integer feeId);

    java.util.List<Fee> findByStudentId(Integer studentId);
}
