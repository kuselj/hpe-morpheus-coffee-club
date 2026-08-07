package com.hpe.morpheus.coffeeclub.service;

import com.hpe.morpheus.coffeeclub.dto.CoworkerBalance;
import com.hpe.morpheus.coffeeclub.entity.CoffeeOrder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Lifetime balance calculation")
class BalanceCalculatorTest {

    private static final LocalDate DAY_1 = LocalDate.of(2026, 8, 3);
    private static final LocalDate DAY_2 = LocalDate.of(2026, 8, 4);

    private final BalanceCalculator calculator = new BalanceCalculator();

    private static CoffeeOrder row(LocalDate date, String name, String price, String paid) {
        return new CoffeeOrder(date, name, "Drink", new BigDecimal(price), new BigDecimal(paid), "N");
    }

    @Test
    @DisplayName("an empty history produces no balances")
    void emptyHistory() {
        assertThat(calculator.calculate(List.of())).isEmpty();
    }

    @Test
    @DisplayName("net difference is total paid minus total consumed")
    void netDifferenceIsPaidMinusConsumed() {
        Map<String, CoworkerBalance> balances = calculator.calculate(List.of(
                row(DAY_1, "Bob", "3.50", "9.00"),
                row(DAY_1, "Jim", "2.00", "0.00"),
                row(DAY_1, "Ana", "3.50", "0.00"),
                row(DAY_2, "Bob", "3.50", "0.00"),
                row(DAY_2, "Jim", "2.00", "9.00"),
                row(DAY_2, "Ana", "3.50", "0.00")));

        assertThat(balances.get("bob").totalPaid()).isEqualByComparingTo("9.00");
        assertThat(balances.get("bob").totalConsumed()).isEqualByComparingTo("7.00");
        assertThat(balances.get("bob").netDifference()).isEqualByComparingTo("2.00");

        assertThat(balances.get("jim").netDifference()).isEqualByComparingTo("5.00");

        // Ana has never paid but has consumed 7.00, so she is furthest "in debt" to the group.
        assertThat(balances.get("ana").totalPaid()).isEqualByComparingTo("0.00");
        assertThat(balances.get("ana").netDifference()).isEqualByComparingTo("-7.00");
    }

    @Test
    @DisplayName("the same person under different casing is a single balance")
    void mergesNamesCaseInsensitively() {
        Map<String, CoworkerBalance> balances = calculator.calculate(List.of(
                row(DAY_1, "Bob", "3.00", "6.00"),
                row(DAY_2, "  bob ", "3.00", "0.00")));

        assertThat(balances).hasSize(1);
        assertThat(balances.get("bob").totalConsumed()).isEqualByComparingTo("6.00");
        assertThat(balances.get("bob").netDifference()).isEqualByComparingTo("0.00");
    }

    @Test
    @DisplayName("balances come back sorted alphabetically and scaled to two decimals")
    void sortsAlphabeticallyAndScales() {
        Map<String, CoworkerBalance> balances = calculator.calculate(List.of(
                row(DAY_1, "zoe", "1.5", "0"),
                row(DAY_1, "Ana", "1.5", "0"),
                row(DAY_1, "bob", "1.5", "4.5")));

        assertThat(balances.values())
                .extracting(CoworkerBalance::name)
                .containsExactly("Ana", "bob", "zoe");
        assertThat(balances.get("zoe").netDifference().scale()).isEqualTo(2);
        assertThat(balances.get("zoe").netDifference()).isEqualByComparingTo("-1.50");
    }

    @Test
    @DisplayName("the display name is taken from the most recent row for that person")
    void keepsMostRecentSpelling() {
        // findAllNewestFirst() supplies the newest row first, so that spelling wins.
        Map<String, CoworkerBalance> balances = calculator.calculate(List.of(
                row(DAY_2, "BOB", "1.00", "0.00"),
                row(DAY_1, "bob", "1.00", "0.00")));

        assertThat(balances.get("bob").name()).isEqualTo("BOB");
    }
}
