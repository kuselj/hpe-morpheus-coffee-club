package com.hpe.morpheus.coffeeclub;

import com.hpe.morpheus.coffeeclub.entity.CoffeeOrder;
import com.hpe.morpheus.coffeeclub.repository.CoffeeOrderRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end check over the real application context: seeded data, pre-population, submission and
 * the effect of a submission on the next day's pre-population.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional // each test rolls back to the seeded state so the tests stay independent
// The shared test config leaves data.sql out so the @DataJpaTest slices start from an empty table;
// this test wants the real seed, so it opts back in.
@TestPropertySource(properties = "spring.sql.init.data-locations=classpath:data.sql")
@DisplayName("Coffee club end to end")
class CoffeeClubIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CoffeeOrderRepository repository;

    @Test
    @DisplayName("a blank database is seeded with Bob and Jim dated yesterday")
    void seedsBobAndJim() {
        List<CoffeeOrder> seeded = repository.findAllNewestFirst();

        assertThat(seeded).hasSizeGreaterThanOrEqualTo(2);
        assertThat(seeded).anySatisfy(order -> {
            assertThat(order.getName()).isEqualTo("Bob");
            assertThat(order.getDrink()).isEqualTo("Cappuccino");
            assertThat(order.getPrice()).isEqualByComparingTo("0.00");
            assertThat(order.getTotalPaidToday()).isEqualByComparingTo("0.00");
            assertThat(order.getIsRemoved()).isEqualTo("N");
            assertThat(order.getOrderDate()).isEqualTo(LocalDate.now().minusDays(1));
        });
        assertThat(seeded).anySatisfy(order -> {
            assertThat(order.getName()).isEqualTo("Jim");
            assertThat(order.getDrink()).isEqualTo("Black Coffee");
        });
    }

    @Test
    @DisplayName("the page pre-populates from the seed data, submits, and reflects the new order")
    void fullRoundTrip() throws Exception {
        mockMvc.perform(get("/api/orders/prepopulate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lines[0].name").value("Bob"))
                .andExpect(jsonPath("$.lines[0].drink").value("Cappuccino"))
                .andExpect(jsonPath("$.lines[0].price").value(0.00))
                .andExpect(jsonPath("$.lines[1].name").value("Jim"));

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"lines":[
                                  {"name":"Bob","drink":"Cappuccino","price":3.50,"isRemoved":false},
                                  {"name":"Jim","drink":"Black Coffee","price":2.25,"isRemoved":false},
                                  {"name":"Nia","drink":"Macchiato","price":4.00,"isRemoved":false}
                                ]}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.payer").value("Bob"))
                .andExpect(jsonPath("$.total").value(9.75));

        // The next visit carries the new roster, drinks and prices forward.
        mockMvc.perform(get("/api/orders/prepopulate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lines.length()").value(3))
                .andExpect(jsonPath("$.lines[0].name").value("Bob"))
                .andExpect(jsonPath("$.lines[1].name").value("Jim"))
                .andExpect(jsonPath("$.lines[2].name").value("Nia"))
                .andExpect(jsonPath("$.lines[2].price").value(4.00));

        // Bob paid the whole round, so he is now well ahead and will not pay next.
        // Balances come back alphabetically: Bob, Jim, Nia.
        mockMvc.perform(get("/api/orders/balances"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Bob"))
                .andExpect(jsonPath("$[0].totalPaid").value(9.75))
                .andExpect(jsonPath("$[0].totalConsumed").value(3.50))
                .andExpect(jsonPath("$[0].netDifference").value(6.25))
                .andExpect(jsonPath("$[1].name").value("Jim"))
                .andExpect(jsonPath("$[1].netDifference").value(-2.25));
    }

    @Test
    @DisplayName("removing someone drops them from the next pre-population but keeps their history")
    void removalDropsFromNextPrepopulation() throws Exception {
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"lines":[
                                  {"name":"Bob","drink":"Cappuccino","price":3.50,"isRemoved":false},
                                  {"name":"Jim","drink":"Black Coffee","price":2.25,"isRemoved":true}
                                ]}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.total").value(3.50));

        mockMvc.perform(get("/api/orders/prepopulate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lines.length()").value(1))
                .andExpect(jsonPath("$.lines[0].name").value("Bob"))
                .andExpect(jsonPath("$.balances[?(@.name == 'Jim')]").exists());
    }
}
