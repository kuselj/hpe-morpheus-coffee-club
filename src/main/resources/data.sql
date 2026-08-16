-- ---------------------------------------------------------------------------
-- Seed data for a blank HPE Morpheus Coffee Club database.
--
-- Two coworkers dated yesterday, so the group order page has a previous order to
-- pre-populate from on first run. Both are seeded at 0.00, which leaves everyone
-- on a net difference of 0.00 and hands the first round to whoever comes first
-- alphabetically among today's participants.
--
-- This script runs on every startup, so it is guarded: the WHERE NOT EXISTS
-- applies to the whole INSERT, meaning nothing is written once the table holds
-- any row at all. Re-running it can never duplicate the seed, and it will not
-- resurrect someone who has since been removed through the UI.
--
-- To change who the club starts with, edit the VALUES list below and delete the
-- data/ folder so the next startup seeds afresh.
--
-- Portability: written to ISO SQL. `CURRENT_DATE - INTERVAL '1' DAY` is standard
-- datetime arithmetic and the row constructor `FROM (VALUES ...) AS t (cols)` is
-- SQL:1999; both run unchanged on H2 and PostgreSQL. See schema.sql for the
-- engines that need adjustments.
-- ---------------------------------------------------------------------------
INSERT INTO hpe_morpheus_coffee_club (order_date, name, drink, price, total_paid_today, is_removed)
SELECT *
FROM (VALUES
    (CURRENT_DATE - INTERVAL '1' DAY, 'Bob', 'Cappuccino',   0.00, 0.00, 'N'),
    (CURRENT_DATE - INTERVAL '1' DAY, 'Jim', 'Black Coffee', 0.00, 0.00, 'N')
) AS seed (order_date, name, drink, price, total_paid_today, is_removed)
WHERE NOT EXISTS (SELECT 1 FROM hpe_morpheus_coffee_club);
