package com.InstantAnalytics.Billing.controller;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.InstantAnalytics.Billing.service.SubscriptionService;
import com.InstantAnalytics.Billing.model.Subscription;
import java.util.Map;



@RestController
@RequestMapping("/api/subscriptions")
public class SubscriptionController {

    @Autowired
    private SubscriptionService subscriptionService;

    // API to fetch subscription details
    @GetMapping("/{userId}")
    public ResponseEntity<Subscription> getSubscription(@PathVariable Long userId) {
        Subscription subscription = subscriptionService.getSubscriptionByUserId(userId);
        return ResponseEntity.ok(subscription);
    }

    // API to create Stripe Checkout session
    @PostMapping("/create-checkout-session")
    public ResponseEntity<Map<String, String>> createCheckoutSession(@RequestBody Map<String, Object> payload) {
        try {
            Long userId = Long.valueOf(payload.get("userId").toString());
            String couponCode = payload.get("couponCode") != null ? payload.get("couponCode").toString() : ""; // Extract coupon code or set default
            System.out.println("Received userId: " + userId); // Print userId for debugging
            System.out.println("Received couponCode: " + couponCode); // Print couponCode for debugging

            Map<String, String> response = subscriptionService.createCheckoutSession(userId, couponCode);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.out.println("Error creating checkout session: " + e.getMessage()); // Print error message
            e.printStackTrace(); // Print the full stack trace
            return ResponseEntity.badRequest().body(null);
        }
    }



    // API to handle payment success
    @PostMapping("/payment-success")
    public ResponseEntity<String> handlePaymentSuccess(@RequestBody Map<String, String> payload) {
        String sessionId = payload.get("sessionId");
        subscriptionService.handlePaymentSuccess(sessionId);
        return ResponseEntity.ok("Subscription updated successfully");
    }
}