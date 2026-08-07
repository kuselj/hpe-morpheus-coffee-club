package com.hpe.morpheus.coffeeclub.config;

import com.hpe.morpheus.coffeeclub.entity.CoffeeOrder;
import com.hpe.morpheus.coffeeclub.repository.CoffeeOrderRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@DisplayName("Seed data")
class DataInitializerTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 8, 7);

    @Autowired
    private CoffeeOrderRepository repository;

    private DataInitializer initializer() {
        Clock clock = Clock.fixed(TODAY.atStartOfDay(ZoneOffset.UTC).toInstant(), ZoneOffset.UTC);
        return new DataInitializer(repository, clock);
    }

    @Test
    @DisplayName("a blank database is seeded with Bob and Jim dated yesterday")
    void seedsBlankDatabase() {
        initializer().run(null);

        List<CoffeeOrder> seeded = repository.findAllNewestFirst();
        assertThat(seeded).hasSize(2);
        assertThat(seeded).allSatisfy(order -> {
            assertThat(order.getOrderDate()).isEqualTo(TODAY.minusDays(1));
            assertThat(order.getPrice()).isEqualByComparingTo("0.00");
            assertThat(order.getTotalPaidToday()).isEqualByComparingTo("0.00");
            assertThat(order.getIsRemoved()).isEqualTo("N");
        });
        assertThat(seeded)
                .extracting(CoffeeOrder::getName, CoffeeOrder::getDrink)
                .containsExactlyInAnyOrder(
                        org.assertj.core.groups.Tuple.tuple("Bob", "Cappuccino"),
                        org.assertj.core.groups.Tuple.tuple("Jim", "Black Coffee"));
    }

    @Test
    @DisplayName("an existing database is left untouched, so restarts do not duplicate history")
    void doesNotReseedAnExistingDatabase() {
        repository.saveAndFlush(new CoffeeOrder(TODAY.minusDays(5), "Ana", "Latte",
                new BigDecimal("3.50"), new BigDecimal("3.50"), "N"));

        initializer().run(null);

        assertThat(repository.findAll())
                .singleElement()
                .extracting(CoffeeOrder::getName)
                .isEqualTo("Ana");
    }
}
