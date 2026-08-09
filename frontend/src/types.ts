/** Shapes exchanged with the Spring Boot API, plus the editable row model used by the table. */

export interface PrepopulatedLine {
  name: string;
  drink: string;
  price: number;
}

export interface CoworkerBalance {
  name: string;
  totalPaid: number;
  totalConsumed: number;
  netDifference: number;
}

export interface PrepopulateResponse {
  lines: PrepopulatedLine[];
  balances: CoworkerBalance[];
}

export interface OrderLinePayload {
  name: string;
  drink: string;
  price: number;
  isRemoved: boolean;
}

export interface GroupOrderResponse {
  orderDate: string;
  payer: string;
  total: number;
  savedLines: number;
  balances: CoworkerBalance[];
}

export interface FieldErrorDetail {
  lineIndex: number | null;
  field: string | null;
  message: string;
}

export interface ApiErrorResponse {
  timestamp: string;
  status: number;
  message: string;
  fieldErrors: FieldErrorDetail[];
}

/** One editable row of the group order table. */
export interface OrderRow {
  /** Stable client-side key; the database never sees it. */
  id: string;
  name: string;
  drink: string;
  /** Held as text so the field can show "0.00" and be edited freely. */
  price: string;
  isRemoved: boolean;
  /** Field values captured when Remove was ticked, restored if it is unticked again. */
  beforeRemoval: { name: string; drink: string; price: string };
  /**
   * The name and drink this row was pre-populated with, or null for a row added with 'Add Person'
   * that has never been saved. Removing a pre-populated person restores these values so the stored
   * row lines up with their previous order; removing a never-saved row simply discards it.
   */
  original: { name: string; drink: string } | null;
}

/** Per-cell validation messages, keyed by row id. */
export type RowErrors = Partial<Record<'name' | 'drink' | 'price', string>>;
export type ErrorsByRowId = Record<string, RowErrors>;
