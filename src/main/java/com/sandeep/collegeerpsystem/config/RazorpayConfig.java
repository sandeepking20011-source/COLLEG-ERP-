package com.sandeep.collegeerpsystem.config;

import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.core.env.Environment;

@Configuration
public class RazorpayConfig {

    @Bean
    @ConditionalOnExpression(
        "'${razorpay.key.id:}'.trim().length() > 0 && " +
        "'${razorpay.key.secret:}'.trim().length() > 0"
    )
    public RazorpayClient razorpayClient(Environment environment)
            throws RazorpayException {

        String keyId = environment.getProperty("razorpay.key.id", "").trim();
        String keySecret = environment.getProperty("razorpay.key.secret", "").trim();

        if (keyId.isBlank() || keySecret.isBlank()) {
            throw new IllegalStateException("Razorpay credentials are missing.");
        }

        return new RazorpayClient(keyId, keySecret);
    }
}