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

---

* **Prompt:**
  > "So I requested initially that the database columns should be in this order: order_date, name,
  > drink, price, total_paid_today, is_removed. The README.md matches this - so good there. However
  > the actual database columns are not in that order and currently shows (see attached image)"
* **Contribution:** Replaced Hibernate's generated DDL (which emits columns alphabetically and has
  no ordering option) with an explicit `src/main/resources/schema.sql` in the agreed column order
  and switched `ddl-auto` to `validate`, so the entity is checked against the real schema instead of
  defining it; confirmed via `INFORMATION_SCHEMA.COLUMNS` that a fresh database now reports
  `id, order_date, name, drink, price, total_paid_today, is_removed`.

---

* **Prompt:**
  > "This is probably too much behind the scenes technical info for README.md audience: [the
  > explanation of why the DDL lives in schema.sql and why Hibernate is set to ddl-auto: validate]"
* **Contribution:** Cut it back to a one-line pointer to `schema.sql`, and removed the accompanying
  `CREATE TABLE IF NOT EXISTS` caveat as well since it is already documented in `schema.sql` itself
  where a maintainer editing the DDL will see it.

---

* **Prompt:**
  > "I updated the UI text strings in the header subtitle, banner note, and payer TBD box to more
  > ideal verbiage than what I initially requested. Keep these in mind for future UI edits."
* **Contribution:** Adopted the new wording as the reference copy — synced the two places in
  `README.md` that still quoted the superseded note and payer placeholder, left the original
  strings untouched in `PROMPTS.md` since that is the historical record of the request, and
  verified all four strings (including the reworded footer) render in the running UI.

---

* **Prompt:**
  > "Please update the following assumption 5 in README to be: '5. **One group order per day.**
  > Submitting multiple times on the same day will **replace** the existing order rather than add to
  > it...' Can you then move it above the 'A few smaller decisions...' line. GUI Changes: At the top
  > note section replace the single note with **Notes:** and 2 bullet points."
* **Contribution:** Promoted the same-day replacement rule into the main assumptions list as item 4
  and renumbered the remaining two, and replaced the single note above the table with a "Notes:"
  heading over two bulleted points sourced from an `ORDER_NOTES` array — the container also had to
  become a `<div>`, since a `<ul>` inside a `<p>` is invalid HTML and breaks the panel styling.

---

* **Prompt:**
  > "Let's maybe simplify the README assumption from '4. **One group order per day.** Submitting
  > multiple times on the same day will **replace** the existing order rather than add to it,
  > preventing duplicate entries in the ledger...' to '4. **One group order per day:** same-day
  > re-submissions overwrite the previous order of the day (used for corrections, not additional
  > orders). Orders from previous days are immutable (per Assumption 3).'"
* **Contribution:** Applied the shorter wording, which also brings the assumption in line with the
  matching bullet shown in the UI.

---

* **Prompt:**
  > "On the GUI: 1) Add a 'Reset' button at the bottom left of the table, on same line as the
  > 'Submit' button, hover text 'Undo current edits (Historical values not affected).', asking for
  > confirmation with 'Yes' and 'Cancel'. 2) When the 'Remove' checkbox is selected: a brand new
  > entry needs no validation and is not written to the database; an existing person has their name
  > and drink reset to the original values of the day so the removal is easy to follow."
* **Contribution:** Added a Reset button paired with a themed confirmation modal that reloads the
  page's starting state, and taught each row whether it was pre-populated — removing a carried-over
  person now restores their original name and drink (and locks those fields) so the stored row lines
  up with their previous entry, while removing a never-saved row skips validation entirely and is
  dropped from the submitted payload.

---

* **Prompt:**
  > "With regards to: '"root": "file:.." is sitting in frontend/package.json' When I took it out it
  > kept popping back in"
* **Contribution:** Tracked it to `npm install --prefix frontend` run from the repository root —
  with a `package.json` named `root` in the working directory, npm installs that directory into the
  prefix as a dependency; removed the entry and deleted the stray `frontend/package-lock.json`,
  since npm workspaces keep a single lockfile at the root, and reworked Dockerfile stage 1 to
  install from the workspace root so the container build still gets a reproducible `npm ci`.
