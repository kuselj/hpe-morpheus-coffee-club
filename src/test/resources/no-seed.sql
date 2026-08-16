-- Deliberately does nothing.
--
-- Spring Boot's default for spring.sql.init.data-locations is classpath*:data.sql, which would
-- otherwise seed Bob and Jim into every test slice. Most tests build their own fixtures and assert
-- against an empty table, so the shared test config points data-locations here instead. Tests that
-- do want the real seed opt back in with @TestPropertySource.
--
-- The statement below is a no-op, but the file cannot be comments alone: Spring rejects an empty
-- script with "'script' must not be null or empty".
SELECT 1;
