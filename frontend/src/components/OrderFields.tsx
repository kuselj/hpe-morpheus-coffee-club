import type { OrderRow, RowErrors } from '../types';

/**
 * The four editable cells of a group order row. They are shared verbatim by the desktop table and
 * the stacked mobile cards, so the two layouts can never drift apart in behaviour.
 */

export interface FieldHandlers {
  onChange: (id: string, patch: Partial<OrderRow>) => void;
  onToggleRemoved: (id: string, isRemoved: boolean) => void;
  onPriceBlur: (id: string) => void;
}

interface FieldProps extends FieldHandlers {
  row: OrderRow;
  errors: RowErrors;
  rowNumber: number;
  /**
   * Both layouts are always in the DOM and hidden with CSS, so element ids are namespaced per
   * layout to keep them unique and to keep each label bound to the field beside it.
   */
  idPrefix: string;
}

function fieldClass(base: string, invalid: boolean): string {
  return invalid ? `${base} field-invalid` : base;
}

export function NameField({ row, errors, rowNumber, idPrefix, onChange }: FieldProps) {
  const errorId = `${idPrefix}-${row.id}-name-error`;
  return (
    <>
      <input
        id={`${idPrefix}-${row.id}-name`}
        type="text"
        className={fieldClass('field', Boolean(errors.name))}
        value={row.name}
        maxLength={60}
        autoComplete="off"
        spellCheck={false}
        placeholder="e.g. Bob"
        aria-label={`Name for person ${rowNumber}`}
        aria-invalid={Boolean(errors.name)}
        aria-describedby={errors.name ? errorId : undefined}
        onChange={(event) => onChange(row.id, { name: event.target.value })}
      />
      {errors.name ? (
        <span id={errorId} className="error-text">
          {errors.name}
        </span>
      ) : null}
    </>
  );
}

export function DrinkField({ row, errors, rowNumber, idPrefix, onChange }: FieldProps) {
  const errorId = `${idPrefix}-${row.id}-drink-error`;
  return (
    <>
      <input
        id={`${idPrefix}-${row.id}-drink`}
        type="text"
        className={fieldClass('field', Boolean(errors.drink))}
        value={row.drink}
        maxLength={80}
        autoComplete="off"
        placeholder="e.g. Cappuccino"
        aria-label={`Drink for person ${rowNumber}`}
        aria-invalid={Boolean(errors.drink)}
        aria-describedby={errors.drink ? errorId : undefined}
        onChange={(event) => onChange(row.id, { drink: event.target.value })}
      />
      {errors.drink ? (
        <span id={errorId} className="error-text">
          {errors.drink}
        </span>
      ) : null}
    </>
  );
}

export function PriceField({ row, errors, rowNumber, idPrefix, onChange, onPriceBlur }: FieldProps) {
  const errorId = `${idPrefix}-${row.id}-price-error`;
  return (
    <>
      <input
        id={`${idPrefix}-${row.id}-price`}
        type="text"
        inputMode="decimal"
        className={fieldClass('field-numeric', Boolean(errors.price))}
        value={row.price}
        maxLength={6}
        autoComplete="off"
        disabled={row.isRemoved}
        aria-label={`Price for person ${rowNumber}`}
        aria-invalid={Boolean(errors.price)}
        aria-describedby={errors.price ? errorId : undefined}
        onChange={(event) => onChange(row.id, { price: event.target.value })}
        onBlur={() => onPriceBlur(row.id)}
      />
      {errors.price ? (
        <span id={errorId} className="error-text">
          {errors.price}
        </span>
      ) : null}
    </>
  );
}

export function RemoveField({ row, rowNumber, idPrefix, onToggleRemoved }: FieldProps) {
  return (
    <input
      id={`${idPrefix}-${row.id}-remove`}
      type="checkbox"
      className="checkbox"
      checked={row.isRemoved}
      aria-label={`Remove person ${rowNumber} from the coffee club`}
      onChange={(event) => onToggleRemoved(row.id, event.target.checked)}
    />
  );
}
