package com.hpe.morpheus.coffeeclub.service;

import com.hpe.morpheus.coffeeclub.dto.CoworkerBalance;
import com.hpe.morpheus.coffeeclub.entity.CoffeeOrder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Turns raw order history into the lifetime fairness figure for each coworker:
 * {@code netDifference = SUM(total_paid_today) - SUM(price)}.
 *
 * <p>A negative balance means the coworker has consumed more than they have paid for, so they are
 * "in debt" to the group and are a candidate to pay next.</p>
 */
@Component
public class BalanceCalculator {

    /**
     * @param history every order row ever recorded
     * @return balances keyed by {@link Names#key(String)}, ordered alphabetically by name
     */
    public Map<String, CoworkerBalance> calculate(List<CoffeeOrder> history) {
        Map<String, BigDecimal> paid = new LinkedHashMap<>();
        Map<String, BigDecimal> consumed = new LinkedHashMap<>();
        Map<String, String> displayNames = new LinkedHashMap<>();

        for (CoffeeOrder order : history) {
            String key = Names.key(order.getName());
            displayNames.putIfAbsent(key, Names.normalise(order.getName()));
            paid.merge(key, nullSafe(order.getTotalPaidToday()), BigDecimal::add);
            consumed.merge(key, nullSafe(order.getPrice()), BigDecimal::add);
        }

        List<String> keysAlphabetically = new ArrayList<>(displayNames.keySet());
        keysAlphabetically.sort(Comparator.comparing(displayNames::get, String.CASE_INSENSITIVE_ORDER));

        Map<String, CoworkerBalance> balances = new LinkedHashMap<>();
        for (String key : keysAlphabetically) {
            BigDecimal totalPaid = Money.scale(paid.getOrDefault(key, BigDecimal.ZERO));
            BigDecimal totalConsumed = Money.scale(consumed.getOrDefault(key, BigDecimal.ZERO));
            balances.put(key, new CoworkerBalance(
                    displayNames.get(key),
                    totalPaid,
                    totalConsumed,
                    Money.scale(totalPaid.subtract(totalConsumed))));
        }
        return balances;
    }

    private static BigDecimal nullSafe(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
