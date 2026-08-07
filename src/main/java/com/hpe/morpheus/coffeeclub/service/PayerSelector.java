package com.hpe.morpheus.coffeeclub.service;

import com.hpe.morpheus.coffeeclub.dto.CoworkerBalance;
import com.hpe.morpheus.coffeeclub.dto.OrderLineRequest;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Decides who buys the round.
 *
 * <p>The first coworker in the submitted table order who has the lowest lifetime net difference
 * ({@code total paid - total consumed}) and who is actually participating today pays for the
 * group. Participating means the row is not flagged for removal and today's price is greater than
 * zero. Someone with no history at all starts on a net difference of {@code 0.00}.</p>
 */
@Component
public class PayerSelector {

    /**
     * @param lines    the group order rows in display order; order breaks ties
     * @param balances lifetime balances keyed by {@link Names#key(String)}
     * @return the participating coworker who should pay, or empty when nobody is ordering today
     */
    public Optional<String> selectPayer(List<OrderLineRequest> lines, Map<String, CoworkerBalance> balances) {
        String payer = null;
        BigDecimal lowestNet = null;

        for (OrderLineRequest line : lines) {
            if (!isParticipating(line)) {
                continue;
            }
            BigDecimal net = netDifferenceOf(line.name(), balances);
            // Strictly lower only, so the earliest row wins any tie.
            if (lowestNet == null || net.compareTo(lowestNet) < 0) {
                lowestNet = net;
                payer = Names.normalise(line.name());
            }
        }
        return Optional.ofNullable(payer);
    }

    /** A row counts towards today's round when it is not being removed and has a price above zero. */
    public boolean isParticipating(OrderLineRequest line) {
        return !Boolean.TRUE.equals(line.isRemoved()) && Money.isPositive(line.price());
    }

    private BigDecimal netDifferenceOf(String name, Map<String, CoworkerBalance> balances) {
        CoworkerBalance balance = balances.get(Names.key(name));
        return balance == null ? Money.ZERO : balance.netDifference();
    }
}
