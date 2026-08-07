# Claude Code Prompts

This file logs the prompts given to Claude Code during development of the HPE Morpheus Coffee Club,
together with a one-line note on what each one contributed.

The full initial build prompt is recorded in [`PROMPTS.md`](PROMPTS.md) under
_"4. Initial App Generation via Claude"_; it is summarised below rather than repeated in full.

---

* **Prompt:**
  > "Please build a full-stack fully responsive single-page web application (Mobile-First layout but
  > equally suited for desktop), called 'HPE Morpheus Coffee Club'. [Full requirements: Spring Boot +
  > H2 file persistence + React/Vite/TypeScript + Tailwind, the fairness-based payer selection rule,
  > the group order page design, the snake_case/camelCase naming conventions, the seed data, the
  > atmospheric Ethiopian coffee ceremony theme, and the dev / prod / prod-cloud build workflows.]"
* **Contribution:** Built the complete application — Spring Boot 4.1 backend (entity, repository,
  balance/payer services, validated REST API, global error handling, seeded H2 file database),
  React 19 + Vite + Tailwind 4 mobile-first group order page with the coffeehouse theme and inline
  SVG background art, 63 JUnit tests, the three build workflows (dev proxy with HMR, `prod` profile
  fat JAR, three-stage Dockerfile plus `render.yaml`), and the README.
