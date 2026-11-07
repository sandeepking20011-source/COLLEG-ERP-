package com.sandeep.collegeerpsystem.repository;

import com.sandeep.collegeerpsystem.entity.RazorpayOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface RazorpayOrderRepository extends JpaRepository<RazorpayOrder, Long> {

    Optional<RazorpayOrder> findByOrderId(String orderId);

    Optional<RazorpayOrder> findByPaymentId(String paymentId);

    boolean existsByPaymentId(String paymentId);
}