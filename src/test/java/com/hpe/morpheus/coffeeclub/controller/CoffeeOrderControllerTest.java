package com.hpe.morpheus.coffeeclub.controller;

import com.hpe.morpheus.coffeeclub.dto.CoworkerBalance;
import com.hpe.morpheus.coffeeclub.dto.GroupOrderResponse;
import com.hpe.morpheus.coffeeclub.dto.PrepopulateResponse;
import com.hpe.morpheus.coffeeclub.dto.PrepopulatedLine;
import com.hpe.morpheus.coffeeclub.exception.OrderValidationException;
import com.hpe.morpheus.coffeeclub.service.CoffeeClubService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CoffeeOrderController.class)
@DisplayName("Coffee order API")
class CoffeeOrderControllerTest {

    private static final String VALID_LINE =
            "{\"name\":\"Bob\",\"drink\":\"Cappuccino\",\"price\":3.50,\"isRemoved\":false}";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CoffeeClubService service;

    private static String body(String... lines) {
        return "{\"lines\":[" + String.join(",", lines) + "]}";
    }

    @Test
    @DisplayName("GET /api/orders/prepopulate returns the pre-populated rows and balances")
    void prepopulateReturnsRows() throws Exception {
        given(service.prepopulate()).willReturn(new PrepopulateResponse(
                List.of(new PrepopulatedLine("Bob", "Cappuccino", new BigDecimal("3.50"))),
                List.of(new CoworkerBalance("Bob", new BigDecimal("3.50"),
                        new BigDecimal("3.50"), new BigDecimal("0.00")))));

        mockMvc.perform(get("/api/orders/prepopulate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lines[0].name").value("Bob"))
                .andExpect(jsonPath("$.lines[0].drink").value("Cappuccino"))
                .andExpect(jsonPath("$.lines[0].price").value(3.50))
                .andExpect(jsonPath("$.balances[0].netDifference").value(0.00));
    }

    @Test
    @DisplayName("GET /api/orders/balances returns lifetime figures")
    void balancesReturnsLifetimeFigures() throws Exception {
        given(service.balances()).willReturn(List.of(new CoworkerBalance(
                "Ana", new BigDecimal("8.50"), new BigDecimal("3.50"), new BigDecimal("5.00"))));

        mockMvc.perform(get("/api/orders/balances"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Ana"))
                .andExpect(jsonPath("$[0].netDifference").value(5.00));
    }

    @Test
    @DisplayName("POST /api/orders saves a valid order and returns 201 with the payer and total")
    void submitAcceptsValidOrder() throws Exception {
        given(service.submitGroupOrder(any())).willReturn(new GroupOrderResponse(
                LocalDate.of(2026, 8, 7), "Bob", new BigDecimal("3.50"), 1, List.of()));

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(VALID_LINE)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.payer").value("Bob"))
                .andExpect(jsonPath("$.total").value(3.50))
                .andExpect(jsonPath("$.orderDate").value("2026-08-07"));
    }

    @Test
    @DisplayName("an empty table is rejected with the Add Person guidance")
    void rejectsEmptyTable() throws Exception {
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"lines\":[]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        "At least 1 person is required. Click the 'Add Person' button to add an individual."));

        verify(service, never()).submitGroupOrder(any());
    }

    @Test
    @DisplayName("a blank name is rejected and reported against its row")
    void rejectsBlankName() throws Exception {
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("{\"name\":\"\",\"drink\":\"Latte\",\"price\":3.00,\"isRemoved\":false}")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[*].field", everyItem(is("name"))))
                .andExpect(jsonPath("$.fieldErrors[*].lineIndex", hasItem(0)))
                .andExpect(jsonPath("$.fieldErrors[*].message", hasItem("Name is required.")));
    }

    @Test
    @DisplayName("a blank drink is rejected")
    void rejectsBlankDrink() throws Exception {
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("{\"name\":\"Bob\",\"drink\":\"  \",\"price\":3.00,\"isRemoved\":false}")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[*].field", hasItem("drink")))
                .andExpect(jsonPath("$.fieldErrors[*].message", hasItem("Drink is required.")));
    }

    @Test
    @DisplayName("a missing price is rejected")
    void rejectsMissingPrice() throws Exception {
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("{\"name\":\"Bob\",\"drink\":\"Latte\",\"isRemoved\":false}")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[*].field", hasItem("price")))
                .andExpect(jsonPath("$.fieldErrors[*].message", hasItem("Price is required.")));
    }

    @Test
    @DisplayName("a negative price is rejected")
    void rejectsNegativePrice() throws Exception {
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("{\"name\":\"Bob\",\"drink\":\"Latte\",\"price\":-1.00,\"isRemoved\":false}")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[*].message", hasItem("Price cannot be negative.")));
    }

    @Test
    @DisplayName("a price with more than two decimal places is rejected")
    void rejectsTooManyDecimals() throws Exception {
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("{\"name\":\"Bob\",\"drink\":\"Latte\",\"price\":3.555,\"isRemoved\":false}")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[*].message",
                        hasItem("Price must have at most 2 decimal places.")));
    }

    @Test
    @DisplayName("an implausibly large price is rejected")
    void rejectsExcessivePrice() throws Exception {
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("{\"name\":\"Bob\",\"drink\":\"Latte\",\"price\":1000.00,\"isRemoved\":false}")))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("a name containing digits or symbols is rejected")
    void rejectsUnsafeName() throws Exception {
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("{\"name\":\"<script>\",\"drink\":\"Latte\",\"price\":3.00,\"isRemoved\":false}")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[*].field", hasItem("name")))
                .andExpect(jsonPath("$.fieldErrors[*].message", hasItem(
                        "Name may only contain letters, spaces, apostrophes, hyphens and periods.")));
    }

    @Test
    @DisplayName("business rule failures come back as 400 with the service's message")
    void surfacesBusinessRuleFailures() throws Exception {
        willThrow(new OrderValidationException("Each person may only be listed once."))
                .given(service).submitGroupOrder(any());

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(VALID_LINE)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Each person may only be listed once."));
    }

    @Test
    @DisplayName("an unknown API path is a 404, not a 500")
    void unknownPathIsNotFound() throws Exception {
        mockMvc.perform(get("/api/orders/does-not-exist.json"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("the wrong HTTP method is a 405, not a 500")
    void wrongMethodIsNotAllowed() throws Exception {
        mockMvc.perform(post("/api/orders/balances")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(VALID_LINE)))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    @DisplayName("a non-JSON content type is a 415, not a 500")
    void wrongContentTypeIsUnsupported() throws Exception {
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content(body(VALID_LINE)))
                .andExpect(status().isUnsupportedMediaType());
    }

    @Test
    @DisplayName("a malformed body is rejected without leaking internals")
    void rejectsMalformedJson() throws Exception {
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ not json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        "The request could not be read. Please check the values entered."));
    }
}
