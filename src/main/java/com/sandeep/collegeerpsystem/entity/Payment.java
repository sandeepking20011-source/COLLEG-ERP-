package com.sandeep.collegeerpsystem.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "payment_id")
    private Long paymentId;

    @Column(name = "student_id", nullable = false)
    private Integer studentId;

    @Column(name = "fee_id")
    private Integer feeId;

    @Column(name = "amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(name = "payment_date")
    private LocalDateTime paymentDate;

    @Column(name = "payment_method", length = 50)
    private String paymentMethod;

    @Column(name = "transaction_id", length = 100, unique = true)
    private String transactionId;

    @Column(name = "payment_status", length = 30)
    private String paymentStatus;

    @Column(name = "remarks", length = 255)
    private String remarks;

    public Long getPaymentId() { return paymentId; }
    public Integer getStudentId() { return studentId; }
    public Integer getFeeId() { return feeId; }
    public BigDecimal getAmount() { return amount; }
    public LocalDateTime getPaymentDate() { return paymentDate; }
    public String getPaymentMethod() { return paymentMethod; }
    public String getTransactionId() { return transactionId; }
    public String getPaymentStatus() { return paymentStatus; }
    public String getRemarks() { return remarks; }

    public void setStudentId(Integer studentId) { this.studentId = studentId; }
    public void setFeeId(Integer feeId) { this.feeId = feeId; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public void setPaymentDate(LocalDateTime paymentDate) { this.paymentDate = paymentDate; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }
    public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }
    public void setRemarks(String remarks) { this.remarks = remarks; }
}