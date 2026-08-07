package com.hpe.morpheus.coffeeclub.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Confirmation of a saved group order.
 *
 * @param orderDate    date the order was recorded against
 * @param payer        coworker selected to pay for the group
 * @param total        total cost of the group order
 * @param savedLines   number of rows written to the database
 * @param balances     refreshed lifetime balances for every coworker after the save
 */
public record GroupOrderResponse(LocalDate orderDate,
                                 String payer,
                                 BigDecimal total,
                                 int savedLines,
                                 List<CoworkerBalance> balances) {
}
