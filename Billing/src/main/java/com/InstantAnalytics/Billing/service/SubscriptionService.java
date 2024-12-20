// Backend Implementation for Billing Application

package com.InstantAnalytics.Billing.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.InstantAnalytics.Billing.model.Coupon;
import com.InstantAnalytics.Billing.model.Payment;
import com.InstantAnalytics.Billing.model.Subscription;
import com.InstantAnalytics.Billing.repository.CouponRepository;
import com.InstantAnalytics.Billing.repository.PaymentRepository;
import com.InstantAnalytics.Billing.repository.SubscriptionRepository;
import com.stripe.Stripe;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;

import jakarta.annotation.PostConstruct;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@Service
public class SubscriptionService {

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @Value("${stripe.api.key}")
    private String stripeApiKey;
    
    @Value("${frontend.url}")
    private String frontendUrl;
    
    
    @PostConstruct
    public void init() {
        Stripe.apiKey = stripeApiKey;
    }

    // Fetch subscription details by user ID
    public Subscription getSubscriptionByUserId(Long userId) {
        return subscriptionRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Subscription not found for user ID: " + userId));
    }

    // Create a Stripe Checkout session
    public Map<String, String> createCheckoutSession(Long userId, String couponCode) {
        try {
            Subscription subscription = getSubscriptionByUserId(userId);
            Long discountAmount = 0L;

            if (couponCode != null && !couponCode.isEmpty()) {
                discountAmount = validateCoupon(couponCode);
                System.out.println("Coupon applied: " + couponCode + " Discount: " + discountAmount);
            }

            Long totalAmount = 8700L - discountAmount; // Default amount minus discount

            SessionCreateParams.LineItem lineItem = SessionCreateParams.LineItem.builder()
                    .setPriceData(
                            SessionCreateParams.LineItem.PriceData.builder()
                                    .setCurrency("usd")
                                    .setUnitAmount(totalAmount)
                                    .setProductData(
                                            SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                    .setName("Monthly Subscription")
                                                    .build()
                                    )
                                    .build()
                    )
                    .setQuantity(1L)
                    .build();

            SessionCreateParams params = SessionCreateParams.builder()
                    .addLineItem(lineItem)
                    .setMode(SessionCreateParams.Mode.PAYMENT)
                    .setSuccessUrl(frontendUrl+"/success?session_id={CHECKOUT_SESSION_ID}&user_id=" + userId)
                    .setCancelUrl(frontendUrl+"/cancel&user_id=" + userId)
                    .putMetadata("userId", userId.toString())
                    .build();


            Session session = Session.create(params);

            Map<String, String> response = new HashMap<>();
            response.put("url", session.getUrl());
            return response;
        } catch (Exception e) {
            throw new RuntimeException("Failed to create Stripe Checkout session", e);
        }
    }




    // Handle payment success
    @Autowired
    private PaymentRepository paymentRepository;

    public void handlePaymentSuccess(String sessionId) {
        try {
            System.out.println("Starting handlePaymentSuccess for session ID: " + sessionId);

            // Retrieve the session details from Stripe
            Session session = Session.retrieve(sessionId);
            System.out.println("Retrieved session from Stripe: " + session);

            String userIdStr = session.getMetadata().get("userId");
            System.out.println("User ID from metadata: " + userIdStr);

            if (userIdStr == null) {
                throw new RuntimeException("User ID not found in session metadata");
            }

            Long userId = Long.parseLong(userIdStr);

            // Check for duplicate payment
            String transactionId = session.getId();
            if (paymentRepository.findByTransactionId(transactionId).isPresent()) {
                System.out.println("Duplicate payment detected for transaction ID: " + transactionId);
                return; // Skip processing duplicate payments
            }

            System.out.println("Processing payment for transaction ID: " + transactionId);

            // Update subscription
            Subscription subscription = getSubscriptionByUserId(userId);
            subscription.setStatus("Active");
            if (subscription.getEndDate() == null || subscription.getEndDate().isBefore(LocalDate.now())) {
                subscription.setStartDate(LocalDate.now());
                subscription.setEndDate(LocalDate.now().plusMonths(1));
            } else {
                subscription.setEndDate(subscription.getEndDate().plusMonths(1));
            }
            subscriptionRepository.save(subscription);

            // Insert payment record
            Payment payment = new Payment();
            payment.setUserId(userId);
            payment.setAmountPaid(session.getAmountTotal());
            payment.setTransactionId(transactionId);
            payment.setPaymentDate(LocalDate.now());
            paymentRepository.save(payment);

            System.out.println("Payment successfully recorded for transaction ID: " + transactionId);
        } catch (Exception e) {
            System.err.println("Error in handlePaymentSuccess: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to handle payment success", e);
        }
    }



    // Update subscription status by user ID
    public void updateSubscriptionStatus(Long userId, String status) {
        // Fetch the subscription by userId
        Subscription subscription = subscriptionRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Subscription not found for user ID: " + userId));

        // Update the subscription status
        subscription.setStatus(status);

        // Adjust dates if status is "Active"
        if ("Active".equalsIgnoreCase(status)) {
            if (subscription.getEndDate() == null || subscription.getEndDate().isBefore(LocalDate.now())) {
                subscription.setStartDate(LocalDate.now());
                subscription.setEndDate(LocalDate.now().plusMonths(1)); // New subscription
            } else {
                subscription.setEndDate(subscription.getEndDate().plusMonths(1)); // Renewal
            }
        }

        // Save the updated subscription
        subscriptionRepository.save(subscription);
    }
    
    @Autowired
    private CouponRepository couponRepository;

    public Long validateCoupon(String couponCode) {
        Coupon coupon = couponRepository.findByCode(couponCode)
                .orElseThrow(() -> new RuntimeException("Invalid coupon code"));
        return coupon.getAmount();
    }
}
