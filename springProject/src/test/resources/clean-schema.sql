-- Clean all tables for integration tests
DROP SCHEMA IF EXISTS public CASCADE;
CREATE SCHEMA public;
GRANT ALL ON SCHEMA public TO test;
GRANT ALL ON SCHEMA public TO public;
