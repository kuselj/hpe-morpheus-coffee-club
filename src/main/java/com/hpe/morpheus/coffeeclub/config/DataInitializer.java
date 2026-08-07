package com.hpe.morpheus.coffeeclub.config;

import com.hpe.morpheus.coffeeclub.entity.CoffeeOrder;
import com.hpe.morpheus.coffeeclub.repository.CoffeeOrderRepository;
import com.hpe.morpheus.coffeeclub.service.Money;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

/**
 * Seeds a blank database on first startup so the group order page has something to pre-populate
 * from. Both seed rows are dated yesterday with a price of {@code 0.00}, which leaves every
 * coworker on a net difference of {@code 0.00} and hands the first round to whoever appears first
 * alphabetically among today's participants.
 */
@Component
public class DataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final CoffeeOrderRepository repository;
    private final Clock clock;

    public DataInitializer(CoffeeOrderRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (repository.count() > 0) {
            log.info("Coffee club database already contains {} row(s); skipping seed data.", repository.count());
            return;
        }

        LocalDate yesterday = LocalDate.now(clock).minusDays(1);
        repository.saveAll(List.of(
                new CoffeeOrder(yesterday, "Bob", "Cappuccino", Money.ZERO, Money.ZERO, "N"),
                new CoffeeOrder(yesterday, "Jim", "Black Coffee", Money.ZERO, Money.ZERO, "N")));

        log.info("Seeded blank coffee club database with 2 default records dated {}.", yesterday);
    }
}
