package com.sandeep.collegeerpsystem.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "fees")
public class Fee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "fee_id")
    private Integer feeId;

    @Column(name = "student_id", nullable = false)
    private Integer studentId;

    @Column(name = "fee_type", nullable = false, length = 50)
    private String feeType;

    @Column(name = "amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(name = "paid_amount", precision = 10, scale = 2)
    private BigDecimal paidAmount;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "payment_date")
    private LocalDate paymentDate;

    @Column(name = "payment_status")
    private String paymentStatus;

    @Column(name = "transaction_id", length = 100)
    private String transactionId;

    public Integer getFeeId() { return feeId; }
    public Integer getStudentId() { return studentId; }
    public String getFeeType() { return feeType; }
    public BigDecimal getAmount() { return amount; }
    public BigDecimal getPaidAmount() { return paidAmount; }
    public LocalDate getDueDate() { return dueDate; }
    public LocalDate getPaymentDate() { return paymentDate; }
    public String getPaymentStatus() { return paymentStatus; }
    public String getTransactionId() { return transactionId; }

    public BigDecimal getBalance() {
        BigDecimal total = amount == null ? BigDecimal.ZERO : amount;
        BigDecimal paid = paidAmount == null ? BigDecimal.ZERO : paidAmount;
        return total.subtract(paid).max(BigDecimal.ZERO);
    }

    public void setStudentId(Integer studentId) { this.studentId = studentId; }
    public void setFeeType(String feeType) { this.feeType = feeType; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public void setPaidAmount(BigDecimal paidAmount) { this.paidAmount = paidAmount; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }
    public void setPaymentDate(LocalDate paymentDate) { this.paymentDate = paymentDate; }
    public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }
}