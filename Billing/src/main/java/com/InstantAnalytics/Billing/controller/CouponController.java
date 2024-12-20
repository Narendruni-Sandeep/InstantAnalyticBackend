package com.InstantAnalytics.Billing.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.InstantAnalytics.Billing.service.SubscriptionService;

@RestController
@RequestMapping("/api/coupons")
public class CouponController {

    @Autowired
    private SubscriptionService subscriptionService;

    @PostMapping("/validate")
    public ResponseEntity<Map<String, Object>> validateCoupon(@RequestBody Map<String, String> payload) {
        String couponCode = payload.get("couponCode");
        try {
            Long discountAmount = subscriptionService.validateCoupon(couponCode);
            Map<String, Object> response = new HashMap<>();
            response.put("discount", discountAmount);
            response.put("message", "Coupon applied successfully.");
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(400).body(Map.of("message", e.getMessage()));
        }
    }
}

