-- ---------------------------------------------------------------------------
-- Historic orders loaded on top of the initial seed, WITHOUT the seeded people.
--
-- Models an environment where the application started (seeding Bob and Jim), and
-- an earlier history was then imported that does not mention either of them —
-- for example when standing the app up in a different cloud environment.
--
-- None of these rows is flagged removed, so all five are still club members even
-- though they do not appear on the most recent order date. The group order page
-- must therefore carry them forward alongside Bob and Jim.
--
-- Dates are relative so the fixture cannot rot: this batch is always older than
-- the seed, which is dated CURRENT_DATE - INTERVAL '1' DAY.
--
-- Portability: ISO SQL. Multi-row VALUES is SQL:1999 and datetime arithmetic with
-- INTERVAL is standard, so this runs unchanged on H2 and PostgreSQL. Oracle has
-- no multi-row VALUES and would need INSERT ALL or UNION ALL.
-- ---------------------------------------------------------------------------
INSERT INTO hpe_morpheus_coffee_club (order_date, name, drink, price, total_paid_today, is_removed)
VALUES
    (CURRENT_DATE - INTERVAL '6' DAY, 'Angie',    'Iced Tea',          3.15, 0.00, 'N'),
    (CURRENT_DATE - INTERVAL '6' DAY, 'Caroline', 'Mocha',             5.00, 0.00, 'N'),
    (CURRENT_DATE - INTERVAL '6' DAY, 'Don',      'Macchiato',         4.85, 0.00, 'N'),
    (CURRENT_DATE - INTERVAL '6' DAY, 'Trevor',   'Latte',             4.15, 0.00, 'N'),
    (CURRENT_DATE - INTERVAL '6' DAY, 'Beth',     'Caramel Macchiato', 4.99, 0.00, 'N');
