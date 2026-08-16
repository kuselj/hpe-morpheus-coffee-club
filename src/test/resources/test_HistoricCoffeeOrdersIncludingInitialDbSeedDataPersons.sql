-- ---------------------------------------------------------------------------
-- Historic orders loaded on top of the initial seed, INCLUDING the seeded people.
--
-- Same situation as the "Excluding" fixture, except the imported history also
-- covers Bob and Jim. Their seed rows are priced 0.00, so these older rows are
-- where their last real price comes from — the group order page should show
-- Bob at 4.50 and Jim at 3.00 while still taking the drink from their most
-- recent (seeded) record.
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
    (CURRENT_DATE - INTERVAL '6' DAY, 'Bob',      'Cappuccino',        4.50, 0.00, 'N'),
    (CURRENT_DATE - INTERVAL '6' DAY, 'Jim',      'Black Coffee',      3.00, 0.00, 'N'),
    (CURRENT_DATE - INTERVAL '6' DAY, 'Angie',    'Iced Tea',          3.15, 0.00, 'N'),
    (CURRENT_DATE - INTERVAL '6' DAY, 'Caroline', 'Mocha',             5.00, 0.00, 'N'),
    (CURRENT_DATE - INTERVAL '6' DAY, 'Don',      'Macchiato',         4.85, 0.00, 'N'),
    (CURRENT_DATE - INTERVAL '6' DAY, 'Trevor',   'Latte',             4.15, 0.00, 'N'),
    (CURRENT_DATE - INTERVAL '6' DAY, 'Beth',     'Caramel Macchiato', 4.99, 0.00, 'N');
