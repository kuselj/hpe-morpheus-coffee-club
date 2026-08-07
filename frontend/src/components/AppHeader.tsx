/** Page masthead: the club name, a one-line explanation of the fairness rule and today's date. */
export function AppHeader() {
  const today = new Date().toLocaleDateString(undefined, {
    weekday: 'long',
    day: 'numeric',
    month: 'long',
    year: 'numeric',
  });

  return (
    <header className="mb-6 flex flex-col gap-4 sm:mb-8 sm:flex-row sm:items-start sm:justify-between">
      <div className="flex items-start gap-3.5">
        <svg
          className="mt-1 h-9 w-9 shrink-0 text-ember-400 drop-shadow-[0_0_12px_rgba(247,155,50,0.45)] sm:h-11 sm:w-11"
          viewBox="0 0 48 48"
          fill="none"
          aria-hidden="true"
        >
          <path
            d="M24 6c-3.2 3.6-3.2 7.2 0 10.8"
            stroke="currentColor"
            strokeWidth="2.4"
            strokeLinecap="round"
            opacity="0.75"
            className="ember-flicker"
          />
          <path d="M21 17h6l1.6 5H19.4L21 17Z" fill="currentColor" opacity="0.8" />
          <path
            d="M24 22c6.6 0 11 4.6 11 10s-4.4 10-11 10-11-4.6-11-10 4.4-10 11-10Z"
            fill="currentColor"
            opacity="0.9"
          />
          <path d="M34 26c4 .6 5 5 1.6 7.4" stroke="currentColor" strokeWidth="2.6" strokeLinecap="round" />
          <path d="M14.5 25.5C11 24 9.4 20.6 10.6 17.4" stroke="currentColor" strokeWidth="2.4" strokeLinecap="round" />
        </svg>

        <div>
          <h1 className="app-title">HPE Morpheus Coffee Club</h1>
          <p className="app-subtitle">
            One person buys the round each day. Whoever is furthest behind on what they have put in
            versus what they have drunk pays next.
          </p>
        </div>
      </div>

      <p className="shrink-0 rounded-full border border-stone-800 bg-stone-900/70 px-3.5 py-1.5 text-xs font-medium tracking-wide text-stone-400 backdrop-blur-md">
        {today}
      </p>
    </header>
  );
}
