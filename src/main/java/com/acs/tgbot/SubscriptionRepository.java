package com.acs.tgbot;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {
    List<Subscription> findByCustomer(String customer);
    Subscription findByCustomerAndTelegramId(String customer, String telegramId);
}
