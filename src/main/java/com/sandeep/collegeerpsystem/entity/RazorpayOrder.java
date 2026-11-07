package com.sandeep.collegeerpsystem.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "razorpay_orders")
public class RazorpayOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false, unique = true, length = 100)
    private String orderId;

    @Column(name = "payment_id", unique = true, length = 100)
    private String paymentId;

    @Column(name = "payment_signature", length = 255)
    private String paymentSignature;

    @Column(name = "student_id", nullable = false)
    private Integer studentId;

    @Column(name = "fee_id", nullable = false)
    private Integer feeId;

    @Column(name = "amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency = "INR";

    @Column(name = "payment_status", nullable = false, length = 20)
    private String paymentStatus = "CREATED";

    @Column(name = "signature_verified", nullable = false)
    private boolean signatureVerified = false;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    public Long getId() { return id; }
    public String getOrderId() { return orderId; }
    public String getPaymentId() { return paymentId; }
    public String getPaymentSignature() { return paymentSignature; }
    public Integer getStudentId() { return studentId; }
    public Integer getFeeId() { return feeId; }
    public BigDecimal getAmount() { return amount; }
    public String getCurrency() { return currency; }
    public String getPaymentStatus() { return paymentStatus; }
    public boolean isSignatureVerified() { return signatureVerified; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getVerifiedAt() { return verifiedAt; }

    public void setOrderId(String orderId) { this.orderId = orderId; }
    public void setPaymentId(String paymentId) { this.paymentId = paymentId; }
    public void setPaymentSignature(String paymentSignature) { this.paymentSignature = paymentSignature; }
    public void setStudentId(Integer studentId) { this.studentId = studentId; }
    public void setFeeId(Integer feeId) { this.feeId = feeId; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public void setCurrency(String currency) { this.currency = currency; }
    public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }
    public void setSignatureVerified(boolean signatureVerified) { this.signatureVerified = signatureVerified; }
    public void setVerifiedAt(LocalDateTime verifiedAt) { this.verifiedAt = verifiedAt; }
}