package com.InstantAnalytics.Billing.repository;

//Step 3: Create Repositories



import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.InstantAnalytics.Billing.model.Subscription;
import com.InstantAnalytics.Billing.model.User;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

 @Query("SELECT s FROM Subscription s WHERE s.endDate < :currentDate AND s.status = 'Active'")
 List<Subscription> findExpiredSubscriptions(@Param("currentDate") LocalDate currentDate);

 Optional<Subscription> findByUserId(Long userId);
}
