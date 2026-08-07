package com.hpe.morpheus.coffeeclub.controller;

import com.hpe.morpheus.coffeeclub.dto.CoworkerBalance;
import com.hpe.morpheus.coffeeclub.dto.GroupOrderRequest;
import com.hpe.morpheus.coffeeclub.dto.GroupOrderResponse;
import com.hpe.morpheus.coffeeclub.dto.PrepopulateResponse;
import com.hpe.morpheus.coffeeclub.service.CoffeeClubService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class CoffeeOrderController {

    private final CoffeeClubService service;

    public CoffeeOrderController(CoffeeClubService service) {
        this.service = service;
    }

    /** Initial state of the group order table, plus lifetime balances for the live payer preview. */
    @GetMapping("/prepopulate")
    public PrepopulateResponse prepopulate() {
        return service.prepopulate();
    }

    /** Lifetime fairness balances for every coworker on record. */
    @GetMapping("/balances")
    public List<CoworkerBalance> balances() {
        return service.balances();
    }

    /** Validates and saves today's group order, returning the confirmed payer and total. */
    @PostMapping
    public ResponseEntity<GroupOrderResponse> submit(@Valid @RequestBody GroupOrderRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.submitGroupOrder(request));
    }
}
