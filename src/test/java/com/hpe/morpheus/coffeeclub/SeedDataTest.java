package com.hpe.morpheus.coffeeclub;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Exercises {@code schema.sql} and {@code data.sql} directly against a throwaway H2 database.
 *
 * <p>Running the real scripts rather than a Spring slice is what makes this worth having: it proves
 * the two files agree on column names, and it can run the seed repeatedly to show that a restart
 * cannot duplicate or resurrect rows — something the application itself only does once per start.</p>
 */
@DisplayName("Seed data scripts")
class SeedDataTest {

    private static final String SCHEMA = "schema.sql";
    private static final String DATA = "data.sql";

    /** A fresh in-memory database per test; it lives as long as the returned connection. */
    private Connection freshDatabase() throws SQLException {
        return DriverManager.getConnection("jdbc:h2:mem:seed-" + UUID.randomUUID(), "sa", "");
    }

    private void run(Connection connection, String script) {
        ScriptUtils.executeSqlScript(connection, new ClassPathResource(script));
    }

    private List<String> namesIn(Connection connection) throws SQLException {
        List<String> names = new ArrayList<>();
        try (Statement statement = connection.createStatement();
             ResultSet rows = statement.executeQuery(
                     "SELECT name FROM hpe_morpheus_coffee_club ORDER BY name")) {
            while (rows.next()) {
                names.add(rows.getString(1));
            }
        }
        return names;
    }

    private long countIn(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet rows = statement.executeQuery("SELECT COUNT(*) FROM hpe_morpheus_coffee_club")) {
            rows.next();
            return rows.getLong(1);
        }
    }

    @Test
    @DisplayName("a blank database is seeded with Bob and Jim, dated yesterday and priced 0.00")
    void seedsTwoCoworkers() throws SQLException {
        try (Connection connection = freshDatabase()) {
            run(connection, SCHEMA);
            run(connection, DATA);

            try (Statement statement = connection.createStatement();
                 ResultSet rows = statement.executeQuery(
                         "SELECT order_date, name, drink, price, total_paid_today, is_removed "
                                 + "FROM hpe_morpheus_coffee_club ORDER BY name")) {

                assertThat(rows.next()).isTrue();
                assertThat(rows.getDate("order_date").toLocalDate()).isEqualTo(LocalDate.now().minusDays(1));
                assertThat(rows.getString("name")).isEqualTo("Bob");
                assertThat(rows.getString("drink")).isEqualTo("Cappuccino");
                assertThat(rows.getBigDecimal("price")).isEqualByComparingTo("0.00");
                assertThat(rows.getBigDecimal("total_paid_today")).isEqualByComparingTo("0.00");
                assertThat(rows.getString("is_removed")).isEqualTo("N");

                assertThat(rows.next()).isTrue();
                assertThat(rows.getString("name")).isEqualTo("Jim");
                assertThat(rows.getString("drink")).isEqualTo("Black Coffee");

                assertThat(rows.next()).isFalse();
            }
        }
    }

    @Test
    @DisplayName("re-running the seed on every startup never duplicates it")
    void seedIsIdempotent() throws SQLException {
        try (Connection connection = freshDatabase()) {
            run(connection, SCHEMA);
            run(connection, DATA);
            run(connection, DATA);
            run(connection, DATA);

            assertThat(countIn(connection)).isEqualTo(2);
        }
    }

    @Test
    @DisplayName("a database that already holds history is left untouched")
    void doesNotSeedOverExistingData() throws SQLException {
        try (Connection connection = freshDatabase()) {
            run(connection, SCHEMA);
            try (Statement statement = connection.createStatement()) {
                statement.execute(
                        "INSERT INTO hpe_morpheus_coffee_club "
                                + "(order_date, name, drink, price, total_paid_today, is_removed) "
                                + "VALUES (CURRENT_DATE, 'Ana', 'Latte', 3.50, 3.50, 'N')");
            }

            run(connection, DATA);

            assertThat(namesIn(connection)).containsExactly("Ana");
        }
    }

    @Test
    @DisplayName("someone removed from the club is not resurrected by a later startup")
    void doesNotResurrectARemovedCoworker() throws SQLException {
        try (Connection connection = freshDatabase()) {
            run(connection, SCHEMA);
            run(connection, DATA);
            try (Statement statement = connection.createStatement()) {
                statement.execute("DELETE FROM hpe_morpheus_coffee_club WHERE name = 'Bob'");
            }

            run(connection, DATA);

            assertThat(namesIn(connection)).containsExactly("Jim");
        }
    }

    @Test
    @DisplayName("schema.sql creates the columns in the agreed order")
    void schemaKeepsTheAgreedColumnOrder() throws SQLException {
        try (Connection connection = freshDatabase()) {
            run(connection, SCHEMA);

            List<String> columns = new ArrayList<>();
            try (Statement statement = connection.createStatement();
                 ResultSet rows = statement.executeQuery(
                         "SELECT column_name FROM information_schema.columns "
                                 + "WHERE table_name = 'HPE_MORPHEUS_COFFEE_CLUB' ORDER BY ordinal_position")) {
                while (rows.next()) {
                    columns.add(rows.getString(1));
                }
            }

            assertThat(columns).containsExactly(
                    "ID", "ORDER_DATE", "NAME", "DRINK", "PRICE", "TOTAL_PAID_TODAY", "IS_REMOVED");
        }
    }
}
