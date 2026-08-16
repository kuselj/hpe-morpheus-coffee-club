package com.hpe.morpheus.coffeeclub.service;

import com.hpe.morpheus.coffeeclub.dto.CoworkerBalance;
import com.hpe.morpheus.coffeeclub.dto.GroupOrderRequest;
import com.hpe.morpheus.coffeeclub.dto.GroupOrderResponse;
import com.hpe.morpheus.coffeeclub.dto.OrderLineRequest;
import com.hpe.morpheus.coffeeclub.dto.PrepopulateResponse;
import com.hpe.morpheus.coffeeclub.dto.PrepopulatedLine;
import com.hpe.morpheus.coffeeclub.entity.CoffeeOrder;
import com.hpe.morpheus.coffeeclub.exception.OrderValidationException;
import com.hpe.morpheus.coffeeclub.repository.CoffeeOrderRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@DisplayName("Coffee club service")
class CoffeeClubServiceTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 8, 7);
    private static final LocalDate YESTERDAY = TODAY.minusDays(1);
    private static final LocalDate TWO_DAYS_AGO = TODAY.minusDays(2);

    @Autowired
    private CoffeeOrderRepository repository;

    private CoffeeClubService serviceOn(LocalDate date) {
        Clock clock = Clock.fixed(date.atStartOfDay(ZoneOffset.UTC).toInstant(), ZoneOffset.UTC);
        return new CoffeeClubService(repository, new BalanceCalculator(), new PayerSelector(), clock);
    }

    private CoffeeClubService service() {
        return serviceOn(TODAY);
    }

    private void given(LocalDate date, String name, String drink, String price, String paid, String removed) {
        repository.saveAndFlush(new CoffeeOrder(date, name, drink,
                new BigDecimal(price), new BigDecimal(paid), removed));
    }

    private static OrderLineRequest line(String name, String drink, String price, boolean removed) {
        return new OrderLineRequest(name, drink, new BigDecimal(price), removed);
    }

    private static GroupOrderRequest request(OrderLineRequest... lines) {
        return new GroupOrderRequest(List.of(lines));
    }

    @Nested
    @DisplayName("Pre-populating the group order table")
    class Prepopulate {

        @Test
        @DisplayName("an empty database produces no rows, so only the header shows")
        void emptyDatabaseGivesNoRows() {
            PrepopulateResponse response = service().prepopulate();

            assertThat(response.lines()).isEmpty();
            assertThat(response.balances()).isEmpty();
        }

        @Test
        @DisplayName("everyone still in the club is listed, sorted alphabetically by name")
        void listsEveryActiveCoworkerSortedByName() {
            given(YESTERDAY, "Zara", "Flat White", "4.00", "0.00", "N");
            given(YESTERDAY, "Ana", "Latte", "3.50", "0.00", "N");
            given(YESTERDAY, "bob", "Cappuccino", "3.00", "10.50", "N");
            // Sat out the last round but was never removed, so still a member.
            given(TWO_DAYS_AGO, "Quinn", "Espresso", "2.00", "0.00", "N");

            PrepopulateResponse response = service().prepopulate();

            assertThat(response.lines())
                    .extracting(PrepopulatedLine::name)
                    .containsExactly("Ana", "bob", "Quinn", "Zara");
        }

        @Test
        @DisplayName("a removal on an earlier date still keeps that person off the roster")
        void removalOnAnEarlierDateStillApplies() {
            given(TWO_DAYS_AGO, "Gone", "Espresso", "2.00", "0.00", "N");
            given(YESTERDAY, "Gone", "Espresso", "0.00", "0.00", "Y");
            given(YESTERDAY, "Ana", "Latte", "3.50", "0.00", "N");

            assertThat(service().prepopulate().lines())
                    .extracting(PrepopulatedLine::name)
                    .containsExactly("Ana");
        }

        @Test
        @DisplayName("the drink comes from the most recent order for that person")
        void carriesOverTheLatestDrink() {
            given(TWO_DAYS_AGO, "Bob", "Espresso", "2.00", "0.00", "N");
            given(YESTERDAY, "Bob", "Cappuccino", "3.25", "3.25", "N");

            assertThat(service().prepopulate().lines())
                    .singleElement()
                    .extracting(PrepopulatedLine::drink)
                    .isEqualTo("Cappuccino");
        }

        @Test
        @DisplayName("the price is the most recent price above zero, not the most recent price")
        void fallsBackToTheLastPriceAboveZero() {
            given(TWO_DAYS_AGO, "Bob", "Cappuccino", "3.25", "0.00", "N");
            given(YESTERDAY, "Bob", "Cappuccino", "0.00", "0.00", "N");

            List<PrepopulatedLine> lines = service().prepopulate().lines();
            assertThat(lines).hasSize(1);
            assertThat(lines.getFirst().price()).isEqualByComparingTo("3.25");
        }

        @Test
        @DisplayName("someone with no price above zero anywhere in history defaults to 0.00")
        void defaultsToZeroWhenNoPriceEverRecorded() {
            given(YESTERDAY, "Bob", "Cappuccino", "0.00", "0.00", "N");

            List<PrepopulatedLine> lines = service().prepopulate().lines();
            assertThat(lines).hasSize(1);
            assertThat(lines.getFirst().price()).isEqualByComparingTo("0.00");
        }

        @Test
        @DisplayName("a person removed on the last order never pre-populates again")
        void excludesRemovedCoworkers() {
            given(YESTERDAY, "Ana", "Latte", "3.50", "0.00", "N");
            given(YESTERDAY, "Bob", "Cappuccino", "0.00", "0.00", "Y");

            assertThat(service().prepopulate().lines())
                    .extracting(PrepopulatedLine::name)
                    .containsExactly("Ana");
        }

        @Test
        @DisplayName("balances for removed coworkers are still returned so re-adding them keeps their history")
        void keepsBalancesForRemovedCoworkers() {
            given(YESTERDAY, "Ana", "Latte", "3.50", "0.00", "N");
            given(YESTERDAY, "Bob", "Cappuccino", "0.00", "0.00", "Y");

            assertThat(service().prepopulate().balances())
                    .extracting(CoworkerBalance::name)
                    .containsExactly("Ana", "Bob");
        }
    }

    @Nested
    @DisplayName("Submitting a group order")
    class Submit {

        @Test
        @DisplayName("the payer carries the whole group total and everyone else carries zero")
        void payerCarriesTheGroupTotal() {
            given(YESTERDAY, "Bob", "Cappuccino", "0.00", "0.00", "N");
            given(YESTERDAY, "Jim", "Black Coffee", "0.00", "0.00", "N");

            GroupOrderResponse response = service().submitGroupOrder(request(
                    line("Bob", "Cappuccino", "3.50", false),
                    line("Jim", "Black Coffee", "2.25", false)));

            assertThat(response.payer()).isEqualTo("Bob");
            assertThat(response.total()).isEqualByComparingTo("5.75");
            assertThat(response.orderDate()).isEqualTo(TODAY);
            assertThat(response.savedLines()).isEqualTo(2);

            List<CoffeeOrder> saved = repository.findByOrderDateOrderByIdAsc(TODAY);
            assertThat(saved).extracting(CoffeeOrder::getName).containsExactly("Bob", "Jim");
            assertThat(saved.get(0).getTotalPaidToday()).isEqualByComparingTo("5.75");
            assertThat(saved.get(1).getTotalPaidToday()).isEqualByComparingTo("0.00");
        }

        @Test
        @DisplayName("a row flagged for removal is stored with is_removed 'Y' and a price of 0.00")
        void storesRemovalTombstone() {
            GroupOrderResponse response = service().submitGroupOrder(request(
                    line("Bob", "Cappuccino", "3.50", false),
                    line("Jim", "Black Coffee", "2.25", true)));

            CoffeeOrder jim = repository.findByOrderDateOrderByIdAsc(TODAY).get(1);
            assertThat(jim.getIsRemoved()).isEqualTo("Y");
            assertThat(jim.getPrice()).isEqualByComparingTo("0.00");
            // A removed row never contributes to the group total.
            assertThat(response.total()).isEqualByComparingTo("3.50");
        }

        @Test
        @DisplayName("people not ordering today are still stored so the roster carries forward")
        void keepsNonParticipantsOnTheRoster() {
            service().submitGroupOrder(request(
                    line("Bob", "Cappuccino", "3.50", false),
                    line("Jim", "Black Coffee", "0.00", false)));

            assertThat(repository.findByOrderDateOrderByIdAsc(TODAY))
                    .extracting(CoffeeOrder::getName)
                    .containsExactly("Bob", "Jim");
        }

        @Test
        @DisplayName("names and drinks are trimmed before they are stored")
        void trimsInput() {
            service().submitGroupOrder(request(line("  Bob  ", "  Cappuccino  ", "3.50", false)));

            CoffeeOrder saved = repository.findByOrderDateOrderByIdAsc(TODAY).getFirst();
            assertThat(saved.getName()).isEqualTo("Bob");
            assertThat(saved.getDrink()).isEqualTo("Cappuccino");
        }

        @Test
        @DisplayName("the same person listed twice is rejected")
        void rejectsDuplicateNames() {
            assertThatThrownBy(() -> service().submitGroupOrder(request(
                    line("Bob", "Cappuccino", "3.50", false),
                    line("bob", "Latte", "4.00", false))))
                    .isInstanceOf(OrderValidationException.class)
                    .hasMessageContaining("only be listed once");
        }

        @Test
        @DisplayName("an order in which nobody is ordering is rejected")
        void rejectsAnOrderWithNoParticipants() {
            assertThatThrownBy(() -> service().submitGroupOrder(request(
                    line("Bob", "Cappuccino", "0.00", false),
                    line("Jim", "Black Coffee", "2.00", true))))
                    .isInstanceOf(OrderValidationException.class)
                    .hasMessageContaining("At least one person must be ordering today");
        }

        @Test
        @DisplayName("an empty table is rejected with the Add Person guidance")
        void rejectsAnEmptyTable() {
            assertThatThrownBy(() -> service().submitGroupOrder(new GroupOrderRequest(List.of())))
                    .isInstanceOf(OrderValidationException.class)
                    .hasMessage(CoffeeClubService.NO_LINES_MESSAGE);
        }

        @Test
        @DisplayName("submitting twice on the same day replaces the day rather than double-counting it")
        void resubmittingReplacesToday() {
            service().submitGroupOrder(request(
                    line("Bob", "Cappuccino", "3.50", false),
                    line("Jim", "Black Coffee", "2.25", false)));

            service().submitGroupOrder(request(
                    line("Bob", "Cappuccino", "4.00", false),
                    line("Jim", "Black Coffee", "2.00", false)));

            List<CoffeeOrder> saved = repository.findByOrderDateOrderByIdAsc(TODAY);
            assertThat(saved).hasSize(2);
            assertThat(saved.getFirst().getTotalPaidToday()).isEqualByComparingTo("6.00");

            CoworkerBalance bob = service().balances().stream()
                    .filter(balance -> balance.name().equals("Bob"))
                    .findFirst()
                    .orElseThrow();
            assertThat(bob.totalPaid()).isEqualByComparingTo("6.00");
            assertThat(bob.totalConsumed()).isEqualByComparingTo("4.00");
        }

        @Test
        @DisplayName("a newly added person with no history pays first")
        void newcomerWithoutHistoryPaysFirst() {
            given(YESTERDAY, "Bob", "Cappuccino", "0.00", "5.00", "N");

            GroupOrderResponse response = service().submitGroupOrder(request(
                    line("Bob", "Cappuccino", "3.50", false),
                    line("Nia", "Macchiato", "3.00", false)));

            assertThat(response.payer()).isEqualTo("Nia");
        }

        @Test
        @DisplayName("a removed coworker who is re-added keeps their original lifetime balance")
        void reAddedCoworkerKeepsHistory() {
            given(TWO_DAYS_AGO, "Ana", "Latte", "6.00", "0.00", "N");
            given(YESTERDAY, "Ana", "Latte", "0.00", "0.00", "Y");
            given(YESTERDAY, "Bob", "Cappuccino", "0.00", "0.00", "N");

            // Ana is back on the list; she is 6.00 in debt so she pays even though Bob is at 0.00.
            GroupOrderResponse response = service().submitGroupOrder(request(
                    line("Bob", "Cappuccino", "3.50", false),
                    line("Ana", "Latte", "3.00", false)));

            assertThat(response.payer()).isEqualTo("Ana");
        }

        @Test
        @DisplayName("payment rotates fairly across consecutive days")
        void rotatesFairlyOverSuccessiveDays() {
            LocalDate dayOne = LocalDate.of(2026, 9, 1);

            // Day 1: everyone starts level, so the first row (Ana) pays 3.50 + 3.00 + 2.00 = 8.50.
            GroupOrderResponse first = serviceOn(dayOne).submitGroupOrder(request(
                    line("Ana", "Latte", "3.50", false),
                    line("Bob", "Cappuccino", "3.00", false),
                    line("Jim", "Black Coffee", "2.00", false)));
            assertThat(first.payer()).isEqualTo("Ana");
            assertThat(first.total()).isEqualByComparingTo("8.50");

            // Day 2: Ana is +5.00, Bob is -3.00, Jim is -2.00, so Bob pays.
            GroupOrderResponse second = serviceOn(dayOne.plusDays(1)).submitGroupOrder(request(
                    line("Ana", "Latte", "3.50", false),
                    line("Bob", "Cappuccino", "3.00", false),
                    line("Jim", "Black Coffee", "2.00", false)));
            assertThat(second.payer()).isEqualTo("Bob");

            // Day 3: Ana is +1.50, Bob is +2.50, Jim is -4.00, so Jim pays.
            GroupOrderResponse third = serviceOn(dayOne.plusDays(2)).submitGroupOrder(request(
                    line("Ana", "Latte", "3.50", false),
                    line("Bob", "Cappuccino", "3.00", false),
                    line("Jim", "Black Coffee", "2.00", false)));
            assertThat(third.payer()).isEqualTo("Jim");

            // After a full rotation everyone has paid exactly once and nobody is more than one
            // round out of pocket.
            assertThat(serviceOn(dayOne.plusDays(3)).balances())
                    .allSatisfy(balance -> assertThat(balance.totalPaid()).isEqualByComparingTo("8.50"));
        }

        @Test
        @DisplayName("the group total ignores rows flagged for removal and rows priced at zero")
        void totalsOnlyWhatWasOrdered() {
            GroupOrderResponse response = service().submitGroupOrder(request(
                    line("Ana", "Latte", "3.50", false),
                    line("Bob", "Cappuccino", "4.25", true),
                    line("Jim", "Black Coffee", "0.00", false)));

            assertThat(response.total()).isEqualByComparingTo("3.50");
        }
    }
}
