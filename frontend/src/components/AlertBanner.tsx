import type { ReactNode } from 'react';

interface AlertBannerProps {
  tone: 'error' | 'success';
  title?: string;
  children: ReactNode;
  onDismiss?: () => void;
}

const ICONS: Record<AlertBannerProps['tone'], ReactNode> = {
  error: (
    <svg className="banner-icon" viewBox="0 0 20 20" fill="currentColor" aria-hidden="true">
      <path
        fillRule="evenodd"
        d="M10 1.6a8.4 8.4 0 1 0 0 16.8 8.4 8.4 0 0 0 0-16.8ZM9.1 5.8a.9.9 0 0 1 1.8 0v4.9a.9.9 0 1 1-1.8 0V5.8ZM10 15a1.1 1.1 0 1 1 0-2.2 1.1 1.1 0 0 1 0 2.2Z"
        clipRule="evenodd"
      />
    </svg>
  ),
  success: (
    <svg className="banner-icon" viewBox="0 0 20 20" fill="currentColor" aria-hidden="true">
      <path
        fillRule="evenodd"
        d="M10 1.6a8.4 8.4 0 1 0 0 16.8 8.4 8.4 0 0 0 0-16.8Zm4 6.2a.9.9 0 0 0-1.4-1.1l-3.6 4.5-1.7-1.7a.9.9 0 1 0-1.3 1.3l2.4 2.4a.9.9 0 0 0 1.3-.1L14 7.8Z"
        clipRule="evenodd"
      />
    </svg>
  ),
};

/** Page-level feedback: validation failures on submit, or confirmation of a saved round. */
export function AlertBanner({ tone, title, children, onDismiss }: AlertBannerProps) {
  return (
    <div className={tone === 'error' ? 'banner-error' : 'banner-success'} role="alert">
      {ICONS[tone]}
      <div className="min-w-0 flex-1">
        {title ? <p className="font-semibold">{title}</p> : null}
        <div className={title ? 'mt-0.5' : undefined}>{children}</div>
      </div>
      {onDismiss ? (
        <button type="button" className="btn-ghost -my-1 -mr-2" onClick={onDismiss} aria-label="Dismiss message">
          <svg className="h-4 w-4" viewBox="0 0 16 16" fill="none" stroke="currentColor" strokeWidth="2">
            <path d="M3 3l10 10M13 3L3 13" strokeLinecap="round" />
          </svg>
        </button>
      ) : null}
    </div>
  );
}
