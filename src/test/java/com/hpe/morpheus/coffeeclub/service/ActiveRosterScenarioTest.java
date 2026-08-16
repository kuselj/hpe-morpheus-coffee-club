package com.hpe.morpheus.coffeeclub.service;

import com.hpe.morpheus.coffeeclub.dto.PrepopulatedLine;
import com.hpe.morpheus.coffeeclub.repository.CoffeeOrderRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.jdbc.Sql;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The group order page shows everyone still in the club, which is not the same as everyone on the
 * most recent order date.
 *
 * <p>These scenarios run the real {@code data.sql} seed and then load a history on top of it, the
 * way a fresh environment would be populated: the application starts and seeds itself, and earlier
 * records are imported afterwards. Nobody in that history is flagged removed, so all of them are
 * still members and must be carried forward even though they are absent from the latest date.</p>
 */
@DataJpaTest
@DisplayName("Active roster carried forward from history")
class ActiveRosterScenarioTest {

    private static final String SEED = "/data.sql";
    private static final String HISTORY_WITHOUT_SEEDED_PEOPLE =
            "/test_HistoricCoffeeOrdersExcludingInitialDbSeedDataPersons.sql";
    private static final String HISTORY_WITH_SEEDED_PEOPLE =
            "/test_HistoricCoffeeOrdersIncludingInitialDbSeedDataPersons.sql";

    @Autowired
    private CoffeeOrderRepository repository;

    private CoffeeClubService service() {
        return new CoffeeClubService(repository, new BalanceCalculator(), new PayerSelector(),
                Clock.systemDefaultZone());
    }

    private Map<String, PrepopulatedLine> rosterByName() {
        return service().prepopulate().lines().stream()
                .collect(Collectors.toMap(PrepopulatedLine::name, Function.identity()));
    }

    private List<String> rosterNames() {
        return service().prepopulate().lines().stream().map(PrepopulatedLine::name).toList();
    }

    @Test
    @Sql({SEED, HISTORY_WITHOUT_SEEDED_PEOPLE})
    @DisplayName("history that never mentions the seeded pair still keeps everyone on the roster")
    void carriesForwardActivePeopleMissingFromTheLatestOrder() {
        // Only Bob and Jim sit on the most recent date; the other five are older but never removed.
        assertThat(rosterNames())
                .containsExactly("Angie", "Beth", "Bob", "Caroline", "Don", "Jim", "Trevor");
    }

    @Test
    @Sql({SEED, HISTORY_WITHOUT_SEEDED_PEOPLE})
    @DisplayName("each carried-forward person keeps their own drink and last real price")
    void carriesForwardDrinkAndPrice() {
        Map<String, PrepopulatedLine> roster = rosterByName();

        assertThat(roster.get("Angie").drink()).isEqualTo("Iced Tea");
        assertThat(roster.get("Angie").price()).isEqualByComparingTo("3.15");
        assertThat(roster.get("Beth").drink()).isEqualTo("Caramel Macchiato");
        assertThat(roster.get("Beth").price()).isEqualByComparingTo("4.99");
        assertThat(roster.get("Caroline").price()).isEqualByComparingTo("5.00");
        assertThat(roster.get("Don").price()).isEqualByComparingTo("4.85");
        assertThat(roster.get("Trevor").price()).isEqualByComparingTo("4.15");

        // The seeded pair have never ordered at a price above zero, so they stay at 0.00.
        assertThat(roster.get("Bob").drink()).isEqualTo("Cappuccino");
        assertThat(roster.get("Bob").price()).isEqualByComparingTo("0.00");
        assertThat(roster.get("Jim").price()).isEqualByComparingTo("0.00");
    }

    @Test
    @Sql({SEED, HISTORY_WITH_SEEDED_PEOPLE})
    @DisplayName("when the history also covers the seeded pair, their last real price is picked up")
    void seededPeopleTakePricesFromTheImportedHistory() {
        Map<String, PrepopulatedLine> roster = rosterByName();

        assertThat(roster).hasSize(7);
        // Drink comes from their most recent record (the seed); price from the last one above zero.
        assertThat(roster.get("Bob").drink()).isEqualTo("Cappuccino");
        assertThat(roster.get("Bob").price()).isEqualByComparingTo("4.50");
        assertThat(roster.get("Jim").drink()).isEqualTo("Black Coffee");
        assertThat(roster.get("Jim").price()).isEqualByComparingTo("3.00");
    }

    @Test
    @Sql({SEED, HISTORY_WITH_SEEDED_PEOPLE})
    @DisplayName("balances cover everyone in the history, not just the latest order")
    void balancesCoverTheWholeClub() {
        assertThat(service().balances())
                .extracting(balance -> balance.name())
                .containsExactly("Angie", "Beth", "Bob", "Caroline", "Don", "Jim", "Trevor");
    }

    @Test
    @Sql({SEED, HISTORY_WITHOUT_SEEDED_PEOPLE})
    @DisplayName("someone whose most recent record removes them is left off, however old it is")
    void removalStillWinsRegardlessOfAge() {
        // Don is removed on a date later than the import but earlier than the seed.
        repository.saveAndFlush(new com.hpe.morpheus.coffeeclub.entity.CoffeeOrder(
                java.time.LocalDate.now().minusDays(3), "Don", "Macchiato",
                new BigDecimal("0.00"), new BigDecimal("0.00"), "Y"));

        assertThat(rosterNames())
                .containsExactly("Angie", "Beth", "Bob", "Caroline", "Jim", "Trevor")
                .doesNotContain("Don");
    }

    @Test
    @Sql({SEED, HISTORY_WITHOUT_SEEDED_PEOPLE})
    @DisplayName("re-adding someone after a removal puts them back on the roster")
    void reAddingAfterRemovalRestoresThem() {
        repository.saveAndFlush(new com.hpe.morpheus.coffeeclub.entity.CoffeeOrder(
                java.time.LocalDate.now().minusDays(3), "Don", "Macchiato",
                new BigDecimal("0.00"), new BigDecimal("0.00"), "Y"));
        repository.saveAndFlush(new com.hpe.morpheus.coffeeclub.entity.CoffeeOrder(
                java.time.LocalDate.now().minusDays(2), "Don", "Flat White",
                new BigDecimal("4.20"), new BigDecimal("0.00"), "N"));

        Map<String, PrepopulatedLine> roster = rosterByName();
        assertThat(roster).containsKey("Don");
        assertThat(roster.get("Don").drink()).isEqualTo("Flat White");
        assertThat(roster.get("Don").price()).isEqualByComparingTo("4.20");
    }
}
