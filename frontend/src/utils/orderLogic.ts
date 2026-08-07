import type { CoworkerBalance, ErrorsByRowId, OrderRow, RowErrors } from '../types';

/** Shown in the Payer field until every row passes basic validation. */
export const PAYER_PLACEHOLDER = 'TBD (Until all fields filled)';

export const NO_ROWS_MESSAGE =
  "At least 1 person is required. Click the 'Add Person' button to add an individual.";

export const NO_PARTICIPANTS_MESSAGE =
  'At least one person must be ordering today. Enter a price greater than 0 for someone.';

const NAME_PATTERN = /^\p{L}[\p{L} .'-]*$/u;
const PRICE_PATTERN = /^\d{1,3}(\.\d{1,2})?$/;

const MAX_NAME_LENGTH = 60;
const MAX_DRINK_LENGTH = 80;

/** Matches the backend's case-insensitive, whitespace-collapsing name matching. */
export function nameKey(name: string): string {
  return name.trim().replace(/\s+/g, ' ').toLowerCase();
}

/** Formats a number as a plain 0.00 string. */
export function formatMoney(value: number): string {
  return (Number.isFinite(value) ? value : 0).toFixed(2);
}

/** Parses a price field. Returns null when the text is not a valid 0.00-style amount. */
export function parsePrice(text: string): number | null {
  const trimmed = text.trim();
  if (!PRICE_PATTERN.test(trimmed)) {
    return null;
  }
  const value = Number(trimmed);
  return Number.isFinite(value) ? value : null;
}

/** Money is compared in whole cents so that floating point never decides who pays. */
function toCents(value: number): number {
  return Math.round(value * 100);
}

let rowCounter = 0;

export function createRow(overrides: Partial<OrderRow> = {}): OrderRow {
  rowCounter += 1;
  return {
    id: `row-${rowCounter}`,
    name: '',
    drink: '',
    price: '0.00',
    isRemoved: false,
    priceBeforeRemoval: '0.00',
    ...overrides,
  };
}

/** Per-row validation mirroring the backend's bean validation rules. */
export function validateRow(row: OrderRow): RowErrors {
  const errors: RowErrors = {};

  const name = row.name.trim();
  if (name.length === 0) {
    errors.name = 'Name is required.';
  } else if (name.length > MAX_NAME_LENGTH) {
    errors.name = `Name must be ${MAX_NAME_LENGTH} characters or fewer.`;
  } else if (!NAME_PATTERN.test(name)) {
    errors.name = 'Letters, spaces, apostrophes, hyphens and periods only.';
  }

  const drink = row.drink.trim();
  if (drink.length === 0) {
    errors.drink = 'Drink is required.';
  } else if (drink.length > MAX_DRINK_LENGTH) {
    errors.drink = `Drink must be ${MAX_DRINK_LENGTH} characters or fewer.`;
  }

  if (parsePrice(row.price) === null) {
    errors.price = 'Enter an amount such as 3.50 (0 to 999.99).';
  }

  return errors;
}

/** Validates every row and flags anyone listed twice. */
export function validateRows(rows: OrderRow[]): ErrorsByRowId {
  const errors: ErrorsByRowId = {};
  const seen = new Map<string, string>();

  for (const row of rows) {
    const rowErrors = validateRow(row);

    const key = nameKey(row.name);
    if (key.length > 0) {
      if (seen.has(key)) {
        rowErrors.name = 'This person is already listed above.';
      } else {
        seen.set(key, row.id);
      }
    }

    if (Object.keys(rowErrors).length > 0) {
      errors[row.id] = rowErrors;
    }
  }

  return errors;
}

/** A row counts towards today's round when it is not being removed and its price is above zero. */
export function isParticipating(row: OrderRow): boolean {
  if (row.isRemoved) {
    return false;
  }
  const price = parsePrice(row.price);
  return price !== null && price > 0;
}

/** Total cost of the round: every row that is not flagged for removal. */
export function calculateTotal(rows: OrderRow[]): number {
  const cents = rows.reduce((sum, row) => {
    if (row.isRemoved) {
      return sum;
    }
    return sum + toCents(parsePrice(row.price) ?? 0);
  }, 0);
  return cents / 100;
}

/**
 * Live preview of the payer, using the same rule as the backend: among everyone participating
 * today, the lowest lifetime net difference pays, and the earliest row in the table wins a tie.
 * Someone with no history starts at 0.00. The backend recomputes this authoritatively on submit.
 */
export function resolvePayer(rows: OrderRow[], balances: Map<string, number>): string | null {
  let payer: string | null = null;
  let lowestCents: number | null = null;

  for (const row of rows) {
    if (!isParticipating(row)) {
      continue;
    }
    const netCents = toCents(balances.get(nameKey(row.name)) ?? 0);
    if (lowestCents === null || netCents < lowestCents) {
      lowestCents = netCents;
      payer = row.name.trim().replace(/\s+/g, ' ');
    }
  }

  return payer;
}

export function toBalanceMap(balances: CoworkerBalance[]): Map<string, number> {
  return new Map(balances.map((balance) => [nameKey(balance.name), balance.netDifference]));
}
