package com.acs.tgbot;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ModeratorRepository extends JpaRepository<Moderator, Long> {
    Moderator findByTelegramId(String telegramId);
}