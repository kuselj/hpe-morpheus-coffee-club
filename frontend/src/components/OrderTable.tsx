import type { ErrorsByRowId, OrderRow } from '../types';
import { DrinkField, NameField, PriceField, RemoveField, type FieldHandlers } from './OrderFields';

interface OrderTableProps extends FieldHandlers {
  rows: OrderRow[];
  errors: ErrorsByRowId;
}

const EMPTY_MESSAGE = "No one is on the list yet. Use 'Add Person' below to start today's round.";

/**
 * The group order table. Mobile-first: below the `md` breakpoint each person is a stacked card
 * with visible labels, and from `md` upward the same fields are laid out as a real table.
 */
export function OrderTable({ rows, errors, onChange, onToggleRemoved, onPriceBlur }: OrderTableProps) {
  const handlers: FieldHandlers = { onChange, onToggleRemoved, onPriceBlur };

  return (
    <>
      {/* ------------------------------------------------ Mobile: stacked cards */}
      <div className="space-y-3 md:hidden">
        {rows.length === 0 ? (
          <p className="table-empty">{EMPTY_MESSAGE}</p>
        ) : (
          rows.map((row, index) => {
            const rowErrors = errors[row.id] ?? {};
            const fieldProps = {
              row,
              errors: rowErrors,
              rowNumber: index + 1,
              idPrefix: 'card',
              ...handlers,
            };

            return (
              <article
                key={row.id}
                className={row.isRemoved ? 'order-card order-card-removed' : 'order-card'}
              >
                <div className="mb-3 flex items-center justify-between gap-3">
                  <span className="order-card-index">{index + 1}</span>
                  <label className="checkbox-row" htmlFor={`card-${row.id}-remove`}>
                    <RemoveField {...fieldProps} />
                    <span>Remove</span>
                  </label>
                </div>

                <div className="space-y-3">
                  <div>
                    <label className="field-label" htmlFor={`card-${row.id}-name`}>
                      Name
                    </label>
                    <NameField {...fieldProps} />
                  </div>

                  <div>
                    <label className="field-label" htmlFor={`card-${row.id}-drink`}>
                      Drink
                    </label>
                    <DrinkField {...fieldProps} />
                  </div>

                  <div className="max-w-[10rem]">
                    <label className="field-label" htmlFor={`card-${row.id}-price`}>
                      Price
                    </label>
                    <PriceField {...fieldProps} />
                  </div>
                </div>
              </article>
            );
          })
        )}
      </div>

      {/* ----------------------------------------------------- Desktop: table */}
      <div className="hidden overflow-x-auto md:block">
        <table className="order-table">
          <caption className="sr-only">
            Today&apos;s group coffee order: name, drink and price for each person, with a checkbox to
            remove someone from the club.
          </caption>
          <thead>
            <tr>
              <th scope="col" className="w-[32%]">
                Name
              </th>
              <th scope="col" className="w-[34%]">
                Drink
              </th>
              <th scope="col" className="w-[18%] text-right">
                Price
              </th>
              <th scope="col" className="w-[16%] text-center">
                Remove
              </th>
            </tr>
          </thead>
          <tbody>
            {rows.length === 0 ? (
              <tr>
                <td colSpan={4} className="table-empty">
                  {EMPTY_MESSAGE}
                </td>
              </tr>
            ) : (
              rows.map((row, index) => {
                const rowErrors = errors[row.id] ?? {};
                const fieldProps = {
                  row,
                  errors: rowErrors,
                  rowNumber: index + 1,
                  idPrefix: 'table',
                  ...handlers,
                };

                return (
                  <tr key={row.id} className={row.isRemoved ? 'order-table-row-removed' : undefined}>
                    <td>
                      <NameField {...fieldProps} />
                    </td>
                    <td>
                      <DrinkField {...fieldProps} />
                    </td>
                    <td>
                      <PriceField {...fieldProps} />
                    </td>
                    <td className="text-center">
                      <div className="flex h-[2.85rem] items-center justify-center">
                        <RemoveField {...fieldProps} />
                      </div>
                    </td>
                  </tr>
                );
              })
            )}
          </tbody>
        </table>
      </div>
    </>
  );
}
