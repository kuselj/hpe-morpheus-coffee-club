import { formatMoney, PAYER_PLACEHOLDER } from '../utils/orderLogic';

interface SummaryPanelProps {
  /** Resolved payer, or null while the table still has gaps. */
  payer: string | null;
  total: number;
}

/**
 * The Payer and Total read-outs that sit below the table. Both are derived, never typed into, and
 * both update live as prices change.
 */
export function SummaryPanel({ payer, total }: SummaryPanelProps) {
  return (
    <div className="grid w-full gap-3 sm:grid-cols-2 lg:w-auto lg:min-w-[26rem]">
      <div className="summary-tile">
        <span className="summary-label" id="payer-label">
          Payer
        </span>
        <output
          className={payer ? 'summary-value-active' : 'summary-value-pending'}
          aria-labelledby="payer-label"
          aria-live="polite"
          title={payer ?? PAYER_PLACEHOLDER}
        >
          {payer ?? PAYER_PLACEHOLDER}
        </output>
      </div>

      <div className="summary-tile">
        <span className="summary-label" id="total-label">
          Total
        </span>
        <output className="summary-value-active" aria-labelledby="total-label" aria-live="polite">
          {formatMoney(total)}
        </output>
      </div>
    </div>
  );
}
