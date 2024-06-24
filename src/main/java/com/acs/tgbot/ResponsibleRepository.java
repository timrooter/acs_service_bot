package com.acs.tgbot;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ResponsibleRepository extends JpaRepository<Responsible, Long> {
    Responsible findByCustomer(String customer);
}


