package com.sandeep.collegeerpsystem.service;

import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.sandeep.collegeerpsystem.entity.Fee;
import com.sandeep.collegeerpsystem.entity.Student;
import com.sandeep.collegeerpsystem.entity.RazorpayOrder;
import com.sandeep.collegeerpsystem.repository.FeeRepository;
import com.sandeep.collegeerpsystem.repository.StudentRepository;
import com.sandeep.collegeerpsystem.repository.RazorpayOrderRepository;
import org.json.JSONObject;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

@Service
@ConditionalOnExpression(
    "'${razorpay.key.id:}'.trim().length() > 0 && " +
    "'${razorpay.key.secret:}'.trim().length() > 0"
)
public class RazorpayOrderService {

    private final RazorpayClient razorpayClient;
    private final FeeRepository feeRepository;
    private final StudentRepository studentRepository;
    private final RazorpayOrderRepository razorpayOrderRepository;

    public RazorpayOrderService(
            RazorpayClient razorpayClient,
            FeeRepository feeRepository,
            StudentRepository studentRepository,
            RazorpayOrderRepository razorpayOrderRepository) {
        this.razorpayClient = razorpayClient;
        this.feeRepository = feeRepository;
        this.studentRepository = studentRepository;
        this.razorpayOrderRepository = razorpayOrderRepository;
    }

    @Transactional
    public RazorpayOrder createOrder(Integer userId, Integer feeId)
            throws RazorpayException {

        if (userId == null || feeId == null) {
            throw new IllegalArgumentException("Student login or fee details are invalid.");
        }

        Student student = studentRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Student account not found."));

        Fee fee = feeRepository.findById(feeId)
                .orElseThrow(() -> new IllegalArgumentException("Fee record not found."));

        if (!student.getStudentId().equals(fee.getStudentId())) {
            throw new IllegalArgumentException("You are not authorized to pay this fee.");
        }

        BigDecimal balance = fee.getBalance().setScale(2, RoundingMode.UNNECESSARY);

        if (balance.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("This fee has no outstanding balance.");
        }

        long amountInPaise;
        try {
            amountInPaise = balance.movePointRight(2).longValueExact();
        } catch (ArithmeticException ex) {
            throw new IllegalArgumentException("Invalid fee amount.", ex);
        }

        String receipt = "fee-" + feeId + "-" +
                UUID.randomUUID().toString().replace("-", "").substring(0, 16);

        JSONObject request = new JSONObject();
        request.put("amount", amountInPaise);
        request.put("currency", "INR");
        request.put("receipt", receipt);

        JSONObject notes = new JSONObject();
        notes.put("student_id", student.getStudentId());
        notes.put("fee_id", fee.getFeeId());
        request.put("notes", notes);

        Order razorpayResponse = razorpayClient.orders.create(request);
        String orderId = razorpayResponse.get("id");

        if (orderId == null || orderId.isBlank()) {
            throw new IllegalStateException("Razorpay did not return a valid order ID.");
        }

        RazorpayOrder savedOrder = new RazorpayOrder();
        savedOrder.setOrderId(orderId);
        savedOrder.setStudentId(student.getStudentId());
        savedOrder.setFeeId(fee.getFeeId());
        savedOrder.setAmount(balance);
        savedOrder.setCurrency("INR");
        savedOrder.setPaymentStatus("CREATED");
        savedOrder.setSignatureVerified(false);

        return razorpayOrderRepository.save(savedOrder);
    }
}