package com.hpe.morpheus.coffeeclub.service;

import com.hpe.morpheus.coffeeclub.dto.CoworkerBalance;
import com.hpe.morpheus.coffeeclub.dto.OrderLineRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Payer selection")
class PayerSelectorTest {

    private final PayerSelector selector = new PayerSelector();

    private static OrderLineRequest line(String name, String price, boolean removed) {
        return new OrderLineRequest(name, "Drink", new BigDecimal(price), removed);
    }

    private static Map<String, CoworkerBalance> balances(Object... nameThenNet) {
        Map<String, CoworkerBalance> map = new LinkedHashMap<>();
        for (int i = 0; i < nameThenNet.length; i += 2) {
            String name = (String) nameThenNet[i];
            BigDecimal net = new BigDecimal((String) nameThenNet[i + 1]);
            map.put(Names.key(name), new CoworkerBalance(name, BigDecimal.ZERO, net.negate(), net));
        }
        return map;
    }

    @Test
    @DisplayName("the participant with the lowest net difference pays")
    void picksLowestNetDifference() {
        List<OrderLineRequest> lines = List.of(
                line("Ana", "3.50", false),
                line("Bob", "4.00", false),
                line("Jim", "2.00", false));

        assertThat(selector.selectPayer(lines, balances("Ana", "2.00", "Bob", "-5.00", "Jim", "1.00")))
                .contains("Bob");
    }

    @Test
    @DisplayName("a tie on the lowest net difference goes to the first row in the table")
    void tiesGoToTheFirstRowInTheList() {
        List<OrderLineRequest> lines = List.of(
                line("Ana", "3.50", false),
                line("Bob", "4.00", false),
                line("Jim", "2.00", false));

        assertThat(selector.selectPayer(lines, balances("Ana", "0.00", "Bob", "0.00", "Jim", "0.00")))
                .contains("Ana");
    }

    @Test
    @DisplayName("someone not ordering today (price 0) is skipped even if they owe the most")
    void skipsNonParticipants() {
        List<OrderLineRequest> lines = List.of(
                line("Ana", "0.00", false),
                line("Bob", "4.00", false));

        assertThat(selector.selectPayer(lines, balances("Ana", "-99.00", "Bob", "0.00")))
                .contains("Bob");
    }

    @Test
    @DisplayName("a row flagged for removal is skipped")
    void skipsRemovedRows() {
        List<OrderLineRequest> lines = List.of(
                line("Ana", "3.00", true),
                line("Bob", "4.00", false));

        assertThat(selector.selectPayer(lines, balances("Ana", "-99.00", "Bob", "0.00")))
                .contains("Bob");
    }

    @Test
    @DisplayName("a brand new coworker starts on a net difference of zero")
    void unknownCoworkerStartsAtZero() {
        List<OrderLineRequest> lines = List.of(
                line("Bob", "4.00", false),
                line("Newcomer", "3.00", false));

        // Bob is ahead by 1.00, so the newcomer's implicit 0.00 is lower and they pay.
        assertThat(selector.selectPayer(lines, balances("Bob", "1.00"))).contains("Newcomer");
    }

    @Test
    @DisplayName("nobody pays when nobody is ordering")
    void noParticipantsMeansNoPayer() {
        List<OrderLineRequest> lines = List.of(
                line("Ana", "0.00", false),
                line("Bob", "0.00", true));

        assertThat(selector.selectPayer(lines, balances("Ana", "0.00", "Bob", "0.00"))).isEmpty();
    }

    @Test
    @DisplayName("an empty table has no payer")
    void emptyTableHasNoPayer() {
        assertThat(selector.selectPayer(List.of(), Map.of())).isEmpty();
    }

    @Test
    @DisplayName("history is matched case-insensitively when picking the payer")
    void matchesHistoryCaseInsensitively() {
        List<OrderLineRequest> lines = List.of(
                line("bob", "4.00", false),
                line("Ana", "3.00", false));

        assertThat(selector.selectPayer(lines, balances("Bob", "-10.00", "Ana", "0.00"))).contains("bob");
    }

    @Test
    @DisplayName("participation requires a positive price and no removal flag")
    void participationRules() {
        assertThat(selector.isParticipating(line("Bob", "0.01", false))).isTrue();
        assertThat(selector.isParticipating(line("Bob", "0.00", false))).isFalse();
        assertThat(selector.isParticipating(line("Bob", "5.00", true))).isFalse();
    }
}
