import { useCallback, useEffect, useMemo, useState } from 'react';
import { ApiError, fetchPrepopulatedOrder, submitGroupOrder } from '../api/coffeeClubApi';
import type { ErrorsByRowId, GroupOrderResponse, OrderLinePayload, OrderRow } from '../types';
import {
  calculateTotal,
  createRow,
  formatMoney,
  isParticipating,
  NO_PARTICIPANTS_MESSAGE,
  NO_ROWS_MESSAGE,
  parsePrice,
  resolvePayer,
  toBalanceMap,
  validateRows,
} from '../utils/orderLogic';
import { AlertBanner } from './AlertBanner';
import { AppHeader } from './AppHeader';
import { OrderTable } from './OrderTable';
import { SummaryPanel } from './SummaryPanel';

/** Guidance shown above the table. Each note leads with the situation, then what to do about it. */
const ORDER_NOTES: ReadonlyArray<{ lead: string; detail: string }> = [
  {
    lead: "If someone isn't ordering today:",
    detail: 'set their Price to 0.',
  },
  {
    lead: 'One group order per day:',
    detail:
      'same-day re-submissions overwrite the previous order of the day (used for corrections, not additional orders).',
  },
];

const CORRECT_FIELDS_MESSAGE = 'Please correct the highlighted fields and try again.';

/** Maps the API's row-indexed field errors onto the client-side row ids. */
function mapServerErrors(error: ApiError, rows: OrderRow[]): ErrorsByRowId {
  const mapped: ErrorsByRowId = {};
  for (const detail of error.fieldErrors) {
    if (detail.lineIndex === null || detail.field === null) {
      continue;
    }
    const row = rows[detail.lineIndex];
    if (!row) {
      continue;
    }
    const field = detail.field as 'name' | 'drink' | 'price';
    if (field !== 'name' && field !== 'drink' && field !== 'price') {
      continue;
    }
    mapped[row.id] = { ...mapped[row.id], [field]: detail.message };
  }
  return mapped;
}

export function GroupOrderPage() {
  const [rows, setRows] = useState<OrderRow[]>([]);
  const [balances, setBalances] = useState<Map<string, number>>(new Map());

  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState<string | null>(null);

  const [submitting, setSubmitting] = useState(false);
  const [submitError, setSubmitError] = useState<string | null>(null);
  const [serverErrors, setServerErrors] = useState<ErrorsByRowId>({});
  const [showValidation, setShowValidation] = useState(false);
  const [confirmation, setConfirmation] = useState<GroupOrderResponse | null>(null);

  const loadOrder = useCallback(async () => {
    setLoading(true);
    setLoadError(null);
    try {
      const response = await fetchPrepopulatedOrder();
      setRows(
        response.lines.map((line) =>
          createRow({
            name: line.name,
            drink: line.drink,
            price: formatMoney(line.price),
            priceBeforeRemoval: formatMoney(line.price),
          }),
        ),
      );
      setBalances(toBalanceMap(response.balances));
    } catch (error) {
      setLoadError(
        error instanceof ApiError ? error.message : 'Could not load the previous order.',
      );
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void loadOrder();
  }, [loadOrder]);

  /* ------------------------------------------------------------------ Editing */

  const clearFeedback = useCallback(() => {
    setSubmitError(null);
    setServerErrors({});
    setConfirmation(null);
  }, []);

  const updateRow = useCallback(
    (id: string, patch: Partial<OrderRow>) => {
      clearFeedback();
      setRows((current) => current.map((row) => (row.id === id ? { ...row, ...patch } : row)));
    },
    [clearFeedback],
  );

  /**
   * Ticking Remove zeroes the price and remembers what it was; unticking restores it.
   */
  const toggleRemoved = useCallback(
    (id: string, isRemoved: boolean) => {
      clearFeedback();
      setRows((current) =>
        current.map((row) => {
          if (row.id !== id) {
            return row;
          }
          return isRemoved
            ? { ...row, isRemoved: true, priceBeforeRemoval: row.price, price: '0.00' }
            : { ...row, isRemoved: false, price: row.priceBeforeRemoval };
        }),
      );
    },
    [clearFeedback],
  );

  /** Normalises a price to 0.00 once the user leaves the field. */
  const handlePriceBlur = useCallback((id: string) => {
    setRows((current) =>
      current.map((row) => {
        if (row.id !== id) {
          return row;
        }
        const parsed = parsePrice(row.price);
        return parsed === null ? row : { ...row, price: formatMoney(parsed) };
      }),
    );
  }, []);

  const addPerson = useCallback(() => {
    clearFeedback();
    setRows((current) => [...current, createRow()]);
  }, [clearFeedback]);

  /* ----------------------------------------------------------------- Derived */

  const total = useMemo(() => calculateTotal(rows), [rows]);
  const validationErrors = useMemo(() => validateRows(rows), [rows]);

  const hasParticipant = useMemo(() => rows.some(isParticipating), [rows]);
  const allFieldsValid = rows.length > 0 && Object.keys(validationErrors).length === 0;

  /**
   * The Payer resolves only once every row passes basic validation and someone is actually
   * ordering. Default values (price 0.00, Remove unticked) count as filled.
   */
  const payer = useMemo(
    () => (allFieldsValid && hasParticipant ? resolvePayer(rows, balances) : null),
    [allFieldsValid, hasParticipant, rows, balances],
  );

  const displayedErrors = useMemo<ErrorsByRowId>(() => {
    const base = showValidation ? validationErrors : {};
    const merged: ErrorsByRowId = { ...base };
    for (const [rowId, fields] of Object.entries(serverErrors)) {
      merged[rowId] = { ...merged[rowId], ...fields };
    }
    return merged;
  }, [showValidation, validationErrors, serverErrors]);

  /* ---------------------------------------------------------------- Submitting */

  const handleSubmit = useCallback(async () => {
    setShowValidation(true);
    setServerErrors({});
    setConfirmation(null);

    if (rows.length === 0) {
      setSubmitError(NO_ROWS_MESSAGE);
      return;
    }
    if (Object.keys(validationErrors).length > 0) {
      setSubmitError(CORRECT_FIELDS_MESSAGE);
      return;
    }
    if (!hasParticipant) {
      setSubmitError(NO_PARTICIPANTS_MESSAGE);
      return;
    }

    const payload: OrderLinePayload[] = rows.map((row) => ({
      name: row.name.trim(),
      drink: row.drink.trim(),
      price: row.isRemoved ? 0 : (parsePrice(row.price) ?? 0),
      isRemoved: row.isRemoved,
    }));

    setSubmitting(true);
    setSubmitError(null);
    try {
      const response = await submitGroupOrder(payload);
      setConfirmation(response);
      setShowValidation(false);
      // Reload so removed people drop off and prices/drinks carry forward from what was saved.
      await loadOrder();
    } catch (error) {
      if (error instanceof ApiError) {
        setSubmitError(error.message);
        setServerErrors(mapServerErrors(error, rows));
      } else {
        setSubmitError('Something went wrong while saving. Please try again.');
      }
    } finally {
      setSubmitting(false);
    }
  }, [rows, validationErrors, hasParticipant, loadOrder]);

  /* ------------------------------------------------------------------ Render */

  if (loading && rows.length === 0 && !loadError) {
    return (
      <div className="panel-padded flex items-center justify-center gap-3 py-16 text-stone-400">
        <span className="spinner" aria-hidden="true" />
        <span>Warming the jebena…</span>
      </div>
    );
  }

  return (
    <>
      <AppHeader />

      {loadError ? (
        <div className="mb-4">
          <AlertBanner tone="error" title="Could not load the previous order">
            <p>{loadError}</p>
            <button type="button" className="btn-secondary mt-3" onClick={() => void loadOrder()}>
              Try again
            </button>
          </AlertBanner>
        </div>
      ) : null}

      {confirmation ? (
        <div className="mb-4">
          <AlertBanner
            tone="success"
            title="Order saved"
            onDismiss={() => setConfirmation(null)}
          >
            <p>
              <span className="font-semibold text-ember-200">{confirmation.payer}</span> is paying{' '}
              <span className="font-semibold text-ember-200 tabular-nums">
                {formatMoney(confirmation.total)}
              </span>{' '}
              for the group today. {confirmation.savedLines}{' '}
              {confirmation.savedLines === 1 ? 'person was' : 'people were'} recorded for{' '}
              {confirmation.orderDate}.
            </p>
          </AlertBanner>
        </div>
      ) : null}

      {submitError ? (
        <div className="mb-4">
          <AlertBanner tone="error" onDismiss={() => setSubmitError(null)}>
            <p>{submitError}</p>
          </AlertBanner>
        </div>
      ) : null}

      <section className="panel-padded" aria-labelledby="group-order-heading">
        <div className="mb-4 flex flex-col gap-3 sm:mb-5">
          <h2 id="group-order-heading" className="panel-heading">
            Today&apos;s Group Order
          </h2>
          <div className="panel-note">
            <svg className="banner-icon text-ember-400" viewBox="0 0 20 20" fill="currentColor" aria-hidden="true">
              <path
                fillRule="evenodd"
                d="M10 1.6a8.4 8.4 0 1 0 0 16.8 8.4 8.4 0 0 0 0-16.8ZM10 5a1.1 1.1 0 1 1 0 2.2A1.1 1.1 0 0 1 10 5Zm.9 9.2a.9.9 0 1 1-1.8 0V9.3a.9.9 0 1 1 1.8 0v4.9Z"
                clipRule="evenodd"
              />
            </svg>
            <div className="min-w-0">
              <p className="font-semibold text-ember-100">Notes:</p>
              <ul className="mt-1 list-disc space-y-1 pl-4 marker:text-ember-500/80">
                {ORDER_NOTES.map((note) => (
                  <li key={note.lead}>
                    <span className="font-semibold text-ember-100">{note.lead}</span> {note.detail}
                  </li>
                ))}
              </ul>
            </div>
          </div>
        </div>

        <OrderTable
          rows={rows}
          errors={displayedErrors}
          onChange={updateRow}
          onToggleRemoved={toggleRemoved}
          onPriceBlur={handlePriceBlur}
        />

        {/* Add Person on the left, Payer and Total on the right. */}
        <div className="mt-6 flex flex-col gap-5 border-t border-stone-800 pt-5 lg:flex-row lg:items-start lg:justify-between">
          <button type="button" className="btn-secondary self-start" onClick={addPerson}>
            <svg className="h-4 w-4" viewBox="0 0 16 16" fill="none" stroke="currentColor" strokeWidth="2">
              <path d="M8 3v10M3 8h10" strokeLinecap="round" />
            </svg>
            Add Person
          </button>

          <SummaryPanel payer={payer} total={total} />
        </div>
      </section>

      {/* Submit sits at the bottom right of the screen. */}
      <div className="mt-5 flex justify-end">
        <button
          type="button"
          className="btn-primary w-full sm:w-auto sm:min-w-[11rem]"
          onClick={() => void handleSubmit()}
          disabled={submitting}
        >
          {submitting ? (
            <>
              <span className="spinner h-4 w-4 border-ember-100/40 border-t-ember-50" aria-hidden="true" />
              Saving…
            </>
          ) : (
            'Submit'
          )}
        </button>
      </div>
    </>
  );
}
