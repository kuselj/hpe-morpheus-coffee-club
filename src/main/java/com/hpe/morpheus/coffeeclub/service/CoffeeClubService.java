package com.hpe.morpheus.coffeeclub.service;

import com.hpe.morpheus.coffeeclub.dto.ApiErrorResponse.FieldErrorDetail;
import com.hpe.morpheus.coffeeclub.dto.CoworkerBalance;
import com.hpe.morpheus.coffeeclub.dto.GroupOrderRequest;
import com.hpe.morpheus.coffeeclub.dto.GroupOrderResponse;
import com.hpe.morpheus.coffeeclub.dto.OrderLineRequest;
import com.hpe.morpheus.coffeeclub.dto.PrepopulateResponse;
import com.hpe.morpheus.coffeeclub.dto.PrepopulatedLine;
import com.hpe.morpheus.coffeeclub.entity.CoffeeOrder;
import com.hpe.morpheus.coffeeclub.exception.OrderValidationException;
import com.hpe.morpheus.coffeeclub.repository.CoffeeOrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Application logic for the group order page: working out what to pre-populate, who pays, and
 * persisting the day's round.
 */
@Service
public class CoffeeClubService {

    private static final Logger log = LoggerFactory.getLogger(CoffeeClubService.class);

    static final String NO_LINES_MESSAGE =
            "At least 1 person is required. Click the 'Add Person' button to add an individual.";
    static final String NO_PARTICIPANTS_MESSAGE =
            "At least one person must be ordering today. Enter a price greater than 0 for someone, "
                    + "or leave the order unsubmitted.";
    static final String REMOVED = "Y";
    static final String NOT_REMOVED = "N";

    private final CoffeeOrderRepository repository;
    private final BalanceCalculator balanceCalculator;
    private final PayerSelector payerSelector;
    private final Clock clock;

    public CoffeeClubService(CoffeeOrderRepository repository,
                             BalanceCalculator balanceCalculator,
                             PayerSelector payerSelector,
                             Clock clock) {
        this.repository = repository;
        this.balanceCalculator = balanceCalculator;
        this.payerSelector = payerSelector;
        this.clock = clock;
    }

    /**
     * Builds the initial state of the group order table from history.
     *
     * <p>The roster is everyone on the most recent order date who was not flagged for removal.
     * Each person keeps the drink from that order and the most recent price above zero found
     * anywhere in their history (falling back to {@code 0.00}). Rows come back sorted
     * alphabetically by name.</p>
     */
    @Transactional(readOnly = true)
    public PrepopulateResponse prepopulate() {
        List<CoffeeOrder> history = repository.findAllNewestFirst();
        Map<String, CoworkerBalance> balances = balanceCalculator.calculate(history);

        if (history.isEmpty()) {
            return new PrepopulateResponse(List.of(), List.copyOf(balances.values()));
        }

        Map<String, BigDecimal> lastPositivePrices = lastPositivePriceByName(history);

        // The roster is everyone still in the club, not just whoever happened to appear on the most
        // recent date. Someone who skipped the last few rounds — or whose history was loaded into a
        // fresh environment after the seed ran — is still a member until a row marks them removed.
        //
        // `history` is newest first, so the first row seen for a name is that person's most recent
        // record, and that record alone decides whether they are still active.
        Map<String, PrepopulatedLine> roster = new LinkedHashMap<>();
        Set<String> alreadyJudged = new HashSet<>();
        for (CoffeeOrder order : history) {
            String key = Names.key(order.getName());
            if (!alreadyJudged.add(key)) {
                continue;
            }
            if (order.isRemovedFlagSet()) {
                continue;
            }
            roster.put(key, new PrepopulatedLine(
                    Names.normalise(order.getName()),
                    order.getDrink() == null ? "" : order.getDrink().trim(),
                    Money.scale(lastPositivePrices.getOrDefault(key, Money.ZERO))));
        }

        List<PrepopulatedLine> lines = new ArrayList<>(roster.values());
        lines.sort(Comparator.comparing(PrepopulatedLine::name, String.CASE_INSENSITIVE_ORDER));

        return new PrepopulateResponse(lines, List.copyOf(balances.values()));
    }

    /** Lifetime balances for everyone who has ever appeared in an order. */
    @Transactional(readOnly = true)
    public List<CoworkerBalance> balances() {
        return List.copyOf(balanceCalculator.calculate(repository.findAllNewestFirst()).values());
    }

    /**
     * Validates and saves the day's group order.
     *
     * <p>Any round already recorded for today is replaced, so submitting twice on the same day
     * corrects the day rather than double-counting it.</p>
     */
    @Transactional
    public GroupOrderResponse submitGroupOrder(GroupOrderRequest request) {
        List<OrderLineRequest> lines = normalise(request);
        rejectDuplicateNames(lines);

        LocalDate today = LocalDate.now(clock);
        List<CoffeeOrder> history = repository.findAllNewestFirst();

        List<CoffeeOrder> alreadyRecordedToday = history.stream()
                .filter(order -> today.equals(order.getOrderDate()))
                .toList();
        List<CoffeeOrder> priorHistory = history.stream()
                .filter(order -> !today.equals(order.getOrderDate()))
                .toList();

        Map<String, CoworkerBalance> balancesBeforeToday = balanceCalculator.calculate(priorHistory);
        String payer = payerSelector.selectPayer(lines, balancesBeforeToday)
                .orElseThrow(() -> new OrderValidationException(NO_PARTICIPANTS_MESSAGE));

        BigDecimal total = totalOf(lines);
        String payerKey = Names.key(payer);

        List<CoffeeOrder> rows = new ArrayList<>(lines.size());
        for (OrderLineRequest line : lines) {
            boolean isPayer = Names.key(line.name()).equals(payerKey);
            rows.add(new CoffeeOrder(
                    today,
                    line.name(),
                    line.drink(),
                    Money.scale(line.price()),
                    isPayer ? total : Money.ZERO,
                    Boolean.TRUE.equals(line.isRemoved()) ? REMOVED : NOT_REMOVED));
        }

        if (!alreadyRecordedToday.isEmpty()) {
            log.info("Replacing {} existing row(s) already recorded for {}", alreadyRecordedToday.size(), today);
            repository.deleteAll(alreadyRecordedToday);
            repository.flush();
        }
        repository.saveAll(rows);
        repository.flush();

        log.info("Saved group order for {}: {} row(s), payer '{}', total {}", today, rows.size(), payer, total);

        return new GroupOrderResponse(today, payer, total, rows.size(), balances());
    }

    /** Total cost of the round: every row that is not flagged for removal. */
    BigDecimal totalOf(List<OrderLineRequest> lines) {
        BigDecimal total = lines.stream()
                .filter(line -> !Boolean.TRUE.equals(line.isRemoved()))
                .map(line -> line.price() == null ? BigDecimal.ZERO : line.price())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return Money.scale(total);
    }

    /**
     * Trims names and drinks and forces the price of any row flagged for removal to zero, matching
     * the behaviour of the Remove checkbox in the UI.
     */
    private List<OrderLineRequest> normalise(GroupOrderRequest request) {
        if (request == null || request.lines() == null || request.lines().isEmpty()) {
            throw new OrderValidationException(NO_LINES_MESSAGE);
        }
        List<OrderLineRequest> normalised = new ArrayList<>(request.lines().size());
        for (OrderLineRequest line : request.lines()) {
            boolean removed = Boolean.TRUE.equals(line.isRemoved());
            normalised.add(new OrderLineRequest(
                    Names.normalise(line.name()),
                    line.drink() == null ? null : line.drink().trim(),
                    removed ? Money.ZERO : Money.scale(line.price()),
                    removed));
        }
        return normalised;
    }

    private void rejectDuplicateNames(List<OrderLineRequest> lines) {
        Set<String> seen = new LinkedHashSet<>();
        List<FieldErrorDetail> duplicates = new ArrayList<>();
        for (int index = 0; index < lines.size(); index++) {
            String key = Names.key(lines.get(index).name());
            if (!seen.add(key)) {
                duplicates.add(new FieldErrorDetail(index, "name",
                        "'%s' appears more than once. Each person may only be listed once."
                                .formatted(lines.get(index).name())));
            }
        }
        if (!duplicates.isEmpty()) {
            throw new OrderValidationException("Each person may only be listed once.", duplicates);
        }
    }

    /**
     * The most recent price above zero for each person. {@code history} must be ordered newest
     * first, so the first hit for a name wins.
     */
    private Map<String, BigDecimal> lastPositivePriceByName(List<CoffeeOrder> history) {
        Map<String, BigDecimal> prices = new HashMap<>();
        for (CoffeeOrder order : history) {
            if (!Money.isPositive(order.getPrice())) {
                continue;
            }
            prices.putIfAbsent(Names.key(order.getName()), order.getPrice());
        }
        return prices;
    }

    /** Exposed for tests and diagnostics. */
    Optional<LocalDate> latestOrderDate() {
        return repository.findLatestOrderDate();
    }
}
