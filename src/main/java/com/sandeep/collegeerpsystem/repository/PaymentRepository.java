package com.sandeep.collegeerpsystem.repository;

import com.sandeep.collegeerpsystem.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    boolean existsByTransactionId(String transactionId);
}