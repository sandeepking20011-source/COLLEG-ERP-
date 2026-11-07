package com.sandeep.collegeerpsystem.controller;

import com.razorpay.RazorpayException;
import com.sandeep.collegeerpsystem.entity.RazorpayOrder;
import com.sandeep.collegeerpsystem.service.RazorpayOrderService;

import jakarta.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/student/payments/razorpay")
@ConditionalOnExpression(
    "'${razorpay.key.id:}'.trim().length() > 0 && " +
    "'${razorpay.key.secret:}'.trim().length() > 0"
)
public class RazorpayController {

    private static final Logger log = LoggerFactory.getLogger(RazorpayController.class);

    private final RazorpayOrderService razorpayOrderService;

    @Value("${razorpay.key.id}")
    private String razorpayKeyId;

    public RazorpayController(RazorpayOrderService razorpayOrderService) {
        this.razorpayOrderService = razorpayOrderService;
    }

    @PostMapping("/order")
    public ResponseEntity<Map<String, Object>> createOrder(
            @RequestParam Integer feeId,
            HttpSession session) {

        Map<String, Object> response = new LinkedHashMap<>();

        Object rawUserId = session.getAttribute("loginUserId");
        Object rawRole = session.getAttribute("loginRole");

        if (!(rawUserId instanceof Number) || rawRole == null) {
            response.put("success", false);
            response.put("message", "Please login as a student.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }

        if (!"student".equalsIgnoreCase(rawRole.toString())) {
            response.put("success", false);
            response.put("message", "Only students can make fee payments.");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
        }

        if (feeId == null || feeId <= 0) {
            response.put("success", false);
            response.put("message", "Invalid fee record.");
            return ResponseEntity.badRequest().body(response);
        }

        try {
            Integer userId = ((Number) rawUserId).intValue();

            RazorpayOrder order =
                    razorpayOrderService.createOrder(userId, feeId);

            long amountInPaise = order.getAmount()
                    .setScale(2, RoundingMode.UNNECESSARY)
                    .movePointRight(2)
                    .longValueExact();

            response.put("success", true);
            response.put("keyId", razorpayKeyId.trim());
            response.put("orderId", order.getOrderId());
            response.put("amount", amountInPaise);
            response.put("currency", order.getCurrency());
            response.put("feeId", order.getFeeId());

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException ex) {
            response.put("success", false);
            response.put("message", ex.getMessage());
            return ResponseEntity.badRequest().body(response);

        } catch (RazorpayException ex) {
            log.error("Razorpay order creation failed. Error: {}", ex.getMessage(), ex);
            response.put("success", false);
            response.put("message", "Unable to create payment order. Please try again.");
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(response);

        } catch (Exception ex) {
            log.error("Unexpected error while creating Razorpay order.", ex);
            response.put("success", false);
            response.put("message", "An unexpected error occurred while creating the order.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}