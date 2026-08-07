/**
 * Full-bleed background art: a traditional clay Jebena resting over a charcoal brazier, with
 * roasted beans scattered across burlap and soft smoke drifting upward.
 *
 * Drawn as inline SVG rather than a photographic asset so the bundle stays self-contained, scales
 * to any viewport without artefacts, and costs nothing to download.
 */
export function BackgroundScene() {
  return (
    <div className="app-backdrop" aria-hidden="true">
      <svg
        className="h-full w-full"
        viewBox="0 0 1200 800"
        preserveAspectRatio="xMidYMid slice"
        xmlns="http://www.w3.org/2000/svg"
      >
        <defs>
          {/* Espresso canvas, darkest at the edges. */}
          <radialGradient id="canvas" cx="50%" cy="82%" r="95%">
            <stop offset="0%" stopColor="#3a2418" />
            <stop offset="45%" stopColor="#1c120c" />
            <stop offset="100%" stopColor="#0a0706" />
          </radialGradient>

          {/* Firelight pooling around the brazier. */}
          <radialGradient id="firelight" cx="50%" cy="50%" r="50%">
            <stop offset="0%" stopColor="#ffb457" stopOpacity="0.55" />
            <stop offset="40%" stopColor="#e2740f" stopOpacity="0.24" />
            <stop offset="100%" stopColor="#7a2f06" stopOpacity="0" />
          </radialGradient>

          <linearGradient id="clay" x1="0%" y1="0%" x2="100%" y2="100%">
            <stop offset="0%" stopColor="#6b4230" />
            <stop offset="38%" stopColor="#432a1e" />
            <stop offset="100%" stopColor="#1d120c" />
          </linearGradient>

          <linearGradient id="clayRim" x1="0%" y1="0%" x2="100%" y2="0%">
            <stop offset="0%" stopColor="#c98a4b" stopOpacity="0.55" />
            <stop offset="50%" stopColor="#8a5730" stopOpacity="0.2" />
            <stop offset="100%" stopColor="#2b1a12" stopOpacity="0.1" />
          </linearGradient>

          <linearGradient id="embers" x1="0%" y1="0%" x2="0%" y2="100%">
            <stop offset="0%" stopColor="#ffd58a" />
            <stop offset="45%" stopColor="#f4791f" />
            <stop offset="100%" stopColor="#8f2f05" />
          </linearGradient>

          {/* Coarse burlap weave. */}
          <pattern id="burlap" width="16" height="16" patternUnits="userSpaceOnUse">
            <rect width="16" height="16" fill="none" />
            <path d="M0 4h16M0 12h16" stroke="#e9c9a2" strokeOpacity="0.05" strokeWidth="2.5" />
            <path d="M4 0v16M12 0v16" stroke="#3a2417" strokeOpacity="0.16" strokeWidth="2.5" />
          </pattern>

          <filter id="softGlow" x="-60%" y="-60%" width="220%" height="220%">
            <feGaussianBlur stdDeviation="16" />
          </filter>

          <filter id="smokeBlur" x="-70%" y="-40%" width="240%" height="200%">
            <feGaussianBlur stdDeviation="11" />
          </filter>

          <filter id="beanBlur" x="-50%" y="-50%" width="200%" height="200%">
            <feGaussianBlur stdDeviation="1.6" />
          </filter>
        </defs>

        {/* ------------------------------------------------------------ Canvas */}
        <rect width="1200" height="800" fill="url(#canvas)" />
        <rect width="1200" height="800" fill="url(#burlap)" />

        {/* ------------------------------------------------- Ambient firelight */}
        <ellipse cx="600" cy="640" rx="520" ry="330" fill="url(#firelight)" className="ember-flicker" />
        <ellipse cx="185" cy="742" rx="215" ry="120" fill="url(#firelight)" opacity="0.4" />
        <ellipse cx="1035" cy="712" rx="235" ry="130" fill="url(#firelight)" opacity="0.32" />

        {/* --------------------------------------------------- Roasted beans */}
        <g filter="url(#beanBlur)" opacity="0.72">
          {[
            [150, 726, -22],
            [214, 758, 14],
            [281, 730, 40],
            [102, 770, 62],
            [356, 764, -8],
            [946, 742, 26],
            [1012, 714, -34],
            [1076, 752, 8],
            [1138, 722, 52],
            [886, 774, -18],
            [432, 782, 34],
            [742, 786, -44],
          ].map(([x, y, angle]) => (
            <g key={`${x}-${y}`} transform={`translate(${x} ${y}) rotate(${angle})`}>
              <ellipse rx="17" ry="11.5" fill="#2a180f" />
              <ellipse rx="17" ry="11.5" fill="none" stroke="#6d4327" strokeOpacity="0.5" strokeWidth="1.1" />
              <path d="M-13 0c5-5.5 5 5.5 0 0M0 -10.5C-5 -3 -5 3 0 10.5" stroke="#0d0805" strokeWidth="2.1" fill="none" />
            </g>
          ))}
        </g>

        {/* -------------------------------------------------- Charcoal brazier */}
        <g transform="translate(600 0)">
          {/* Glow beneath the pot. */}
          <ellipse cx="0" cy="676" rx="185" ry="52" fill="#f4791f" opacity="0.3" filter="url(#softGlow)" className="ember-flicker" />

          {/* Bowl of the brazier. */}
          <path d="M-165 652 L-132 742 H132 L165 652 Z" fill="#170e09" />
          <path d="M-165 652 L-132 742 H132 L165 652 Z" fill="none" stroke="#5a3a26" strokeOpacity="0.55" strokeWidth="2" />

          {/* Glowing charcoal. */}
          <g className="ember-flicker">
            {[
              [-104, 664, 30, 12],
              [-52, 658, 38, 14],
              [4, 662, 34, 13],
              [58, 657, 36, 13],
              [110, 665, 28, 11],
              [-78, 682, 26, 10],
              [30, 684, 30, 11],
              [82, 681, 24, 9],
            ].map(([x, y, rx, ry]) => (
              <ellipse key={`${x}-${y}`} cx={x} cy={y} rx={rx} ry={ry} fill="url(#embers)" />
            ))}
          </g>
          <ellipse cx="0" cy="666" rx="150" ry="30" fill="#ffcf85" opacity="0.22" filter="url(#softGlow)" />
        </g>

        {/* ------------------------------------------------------ Jebena pot */}
        <g transform="translate(600 0)">
          {/* Handle. */}
          <path
            d="M92 470c58 4 74 62 30 96"
            fill="none"
            stroke="url(#clay)"
            strokeWidth="19"
            strokeLinecap="round"
          />
          <path
            d="M92 470c58 4 74 62 30 96"
            fill="none"
            stroke="#8a5730"
            strokeOpacity="0.35"
            strokeWidth="3"
            strokeLinecap="round"
          />

          {/* Spout. */}
          <path
            d="M-84 452c-46-10-70-46-58-84"
            fill="none"
            stroke="url(#clay)"
            strokeWidth="16"
            strokeLinecap="round"
          />
          <circle cx="-142" cy="366" r="9" fill="#2b1a12" />

          {/* Bulbous body. */}
          <path d="M0 380c74 0 118 56 118 116S74 620 0 620s-118-64-118-124S-74 380 0 380Z" fill="url(#clay)" />
          <path
            d="M-72 424c22-20 52-30 82-28"
            fill="none"
            stroke="#d69a5c"
            strokeOpacity="0.28"
            strokeWidth="7"
            strokeLinecap="round"
          />

          {/* Neck. */}
          <path d="M-25 388 L-17 268 H17 L25 388 Z" fill="url(#clay)" />
          <path d="M-25 388 L-17 268 H17 L25 388 Z" fill="url(#clayRim)" />

          {/* Collar and lid. */}
          <rect x="-30" y="252" width="60" height="17" rx="8" fill="#4d3122" />
          <path d="M-33 252c0-20 15-31 33-31s33 11 33 31Z" fill="#3b2418" />
          <circle cx="0" cy="212" r="9" fill="#5c3a27" />

          {/* Base ring. */}
          <ellipse cx="0" cy="620" rx="86" ry="17" fill="#150d09" />
          <ellipse cx="0" cy="616" rx="86" ry="15" fill="#33200f" opacity="0.85" />
        </g>

        {/* ---------------------------------------------------------- Smoke */}
        <g filter="url(#smokeBlur)" stroke="#f7e4c8" fill="none" strokeLinecap="round">
          <path
            d="M596 206c-28-46 26-76-4-124s16-72 16-72"
            strokeWidth="17"
            opacity="0.28"
            className="ember-drift"
          />
          <path
            d="M556 200c-40-58 22-96-14-148"
            strokeWidth="12"
            opacity="0.2"
            className="ember-drift"
            style={{ animationDelay: '-7s' }}
          />
          <path
            d="M646 198c34-52-20-88 12-140"
            strokeWidth="13"
            opacity="0.22"
            className="ember-drift"
            style={{ animationDelay: '-14s' }}
          />
        </g>

        {/* --------------------------------------------------------- Vignette */}
        <radialGradient id="vignette" cx="50%" cy="55%" r="78%">
          <stop offset="55%" stopColor="#000000" stopOpacity="0" />
          <stop offset="100%" stopColor="#000000" stopOpacity="0.82" />
        </radialGradient>
        <rect width="1200" height="800" fill="url(#vignette)" />
      </svg>
    </div>
  );
}
