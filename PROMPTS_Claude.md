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

---

* **Prompt:**
  > "I have completely removed the DevCorsConfig.java class and migrated its development
  > configurations entirely into application-dev.yml to keep the source code clean. Here is my
  > current development profile setup"
* **Contribution:** Verified the migrated profile against a running server and found the H2 console
  was returning 404 because Spring Boot 4 no longer auto-configures it from the H2 driver alone —
  added the `spring-boot-h2console` dependency, which fixed it and made the hand-rolled `dev` Maven
  profile redundant; also confirmed that `spring.mvc.cors.*` and `spring.security.headers.*` bind to
  nothing (no such properties, and Spring Security is not a dependency), so those blocks were
  removed as dead config — CORS is genuinely unnecessary because the Vite proxy keeps every request
  same-origin. Kept the correct `AUTO_SERVER=TRUE` fix and documented all of it in the README.

---

* **Prompt:**
  > "So the following in the README.md file might be overkill and not really important for users
  > since behind the scenes and just part of refinement? [the `spring-boot-h2console` explanation in
  > the dev section, and the `/h2-console` 404 and CORS-in-YAML troubleshooting entries]"
* **Contribution:** Agreed and trimmed the README back to what a reader actually acts on — cut the
  Spring Boot 4 modularisation rationale and both refinement-era troubleshooting entries, keeping
  only the one reachable failure (an H2 console login URL missing `AUTO_SERVER=TRUE`).

---

* **Prompt:**
  > "In the README.md it mentions `mvn spring-boot:run "-Dspring-boot.run.profiles=dev"` but I think
  > the clean param is good especially to ensure consecutive runs are good and pristine?
  > `mvn clean spring-boot:run "-Dspring-boot.run.profiles=dev"`"
* **Contribution:** Agreed and restored `clean` to the documented dev command — it clears classes
  for deleted sources (exactly the `DevCorsConfig` case) and any stale `-Pprod` frontend bundle in
  `target/classes/static`; verified across two consecutive runs that it leaves the `data/` H2 files
  untouched, so the order ledger survives.
