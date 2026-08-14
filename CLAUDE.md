# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

`README.md` documents the build, run and deployment workflows in full. This file covers only what
is not obvious from reading it.

## Commands

Every command here is written to run unchanged on Windows, Linux and macOS — keep it that way when
adding more. In particular, quote every `-D…` argument: PowerShell splits an unquoted one at the
first dot, while Bash, zsh and cmd simply ignore the quotes. For the same reason, never use a
Bash-only `VAR=value command` prefix; pass the setting to the tool instead.

```bash
mvn test
```

```bash
mvn test "-Dtest=PayerSelectorTest#tiesGoToTheFirstRowInTheList"
```

```bash
mvn clean package -Pprod
```

Dev needs two terminals — backend on 8080, Vite on 5173 proxying `/api/*` to it:

```bash
mvn clean spring-boot:run "-Dspring-boot.run.profiles=dev"
```

```bash
npm run dev --workspace=frontend
```

The frontend is an npm workspace, so always use `--workspace=frontend` from the repository root.
Never `npm install --prefix frontend`: npm installs the working directory into the prefix and
writes a self-referential `"root": "file:.."` into `frontend/package.json`.

## Things that must change together

- **The payer rule exists twice.** `PayerSelector.java` is authoritative and recomputes on submit;
  `resolvePayer` in `frontend/src/utils/orderLogic.ts` mirrors it so the Payer field can update as
  the user types. No test cross-checks them — change both, or the live preview silently disagrees
  with what gets saved.
- **Input validation exists twice**, for the same reason: constraints on `OrderLineRequest` and
  `validateRow` in `orderLogic.ts`.
- **Note:** Exact UI strings for `ORDER_NOTES`, `PAYER_PLACEHOLDER`, and the header subtitle are
  documented in the `README.md` file as well. Any copy changes made in the code or documentation
  must be mirrored in both places.
- **The `docs/*.drawio.svg` diagrams** describe the layering and the submit flow. Update them when
  either changes. They are draw.io SVGs — the diagram XML lives in the file's `content` attribute
  and is what draw.io re-renders from, so edit them in draw.io rather than hand-patching the SVG.

## Architectural invariants

- **The ledger is derived, never stored.** One row per person per order date; the payer's row holds
  the entire group total in `total_paid_today`, everyone else `0.00`. Balances are recomputed by
  summing history on each request. Money is compared in whole cents so floating point never decides
  who pays.
- **`schema.sql` owns the DDL; Hibernate is `validate` only.** The physical column order is
  contractual and Hibernate emits columns alphabetically, so do not switch back to
  `ddl-auto: update`. `CREATE TABLE IF NOT EXISTS` will not restructure an existing database —
  delete `data/` after a schema change.
- **Removal never deletes.** A pre-populated person gets a tombstone row with `is_removed = 'Y'` and
  their original name and drink restored, so their balance survives being re-added. A row added via
  *Add Person* and removed before it was ever saved is dropped from the payload entirely.
  `OrderRow.original` (null = never saved) is what distinguishes the two cases.
- **Submitting twice on the same day replaces that day's rows** rather than appending. Earlier
  dates are immutable.
- **Names are matched case-insensitively with whitespace collapsed** (`Names.java`, `nameKey`).

## Frontend specifics

- **Mobile-first, but equally suited to desktop.** Base styles target small screens and wider
  layouts are layered on with `sm:` / `md:` / `lg:` variants — never the reverse. Check new UI at
  375 px with no horizontal overflow, and again on desktop, where the layout should be a considered
  one rather than a stretched phone view.
- **CSS / styling:** common and repeating style elements belong in a separate CSS file,
  `frontend/src/styles/coffeehouse.css`, so a restyle happens in one place. Components reference
  those class names instead of repeating long utility strings — when a pattern appears a second
  time, add a shared class there rather than inlining it. `index.css` is only the Tailwind entry
  point and page-level base rules.
- Composable base classes in that file are declared with Tailwind v4 `@utility`, not
  `@layer components` — only a `@utility` can be `@apply`-ed by the variants built on it, and a base
  must be declared before its variants.
- The order table renders twice (stacked cards below `md`, a real `<table>` above), both always in
  the DOM. Element ids are namespaced with `idPrefix` (`card-` / `table-`) to stay unique.

## Spring Boot 4

Auto-configuration is modularised into separate artifacts: `@DataJpaTest` needs
`spring-boot-data-jpa-test`, `@WebMvcTest` needs `spring-boot-webmvc-test`, and the H2 console needs
`spring-boot-h2console`.

## How the documentation files relate

- **`CLAUDE.md`** (this file) is the authoritative context for any Claude instance working in this
  repository. It exists so that another developer's session starts with the same understanding and
  behaves consistently. Keep it current when an invariant changes.
- **`PROMPTS.md`** was written for the code assignment reviewers, who asked to see every prompt used
  on the project together with the research that preceded it. **`PROMPTS_Claude.md`** is its
  companion, covering the Claude Code prompts specifically.

Both prompt files are historical entries kept for human auditability — a record of *how* the project
was built, not instructions to follow and not a specification of how it should behave now. Never
rewrite the prompts quoted in them to match later changes, and where the code has since moved on
(the hand-tuned UI copy, for example) the code is correct, not the quoted spec. Append each new user
prompt to `PROMPTS_Claude.md` with a one-sentence note on the result.
