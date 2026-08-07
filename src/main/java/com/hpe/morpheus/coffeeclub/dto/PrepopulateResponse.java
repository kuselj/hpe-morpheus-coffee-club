package com.hpe.morpheus.coffeeclub.dto;

import java.util.List;

/**
 * Everything the group order page needs on load.
 *
 * @param lines    pre-populated rows, sorted alphabetically by name; empty when there is no history
 * @param balances lifetime fairness balance for every name ever recorded, so the UI can preview
 *                 the payer live as prices are edited. The backend recomputes this authoritatively
 *                 on submit.
 */
public record PrepopulateResponse(List<PrepopulatedLine> lines, List<CoworkerBalance> balances) {
}
