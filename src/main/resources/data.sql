-- Runs automatically on every startup (spring.sql.init.mode=always would be needed if
-- ddl-auto doesn't create tables first — with ddl-auto=update, Hibernate creates the
-- schema before this file runs). Safe to re-run because of the WHERE NOT EXISTS guards.

INSERT INTO category (name, description)
SELECT * FROM (SELECT 'Electronics' AS name, 'Phones, laptops, accessories' AS description) AS tmp
WHERE NOT EXISTS (SELECT 1 FROM category WHERE name = 'Electronics');

INSERT INTO category (name, description)
SELECT * FROM (SELECT 'Office Supplies' AS name, 'Stationery and consumables' AS description) AS tmp
WHERE NOT EXISTS (SELECT 1 FROM category WHERE name = 'Office Supplies');

INSERT INTO supplier (name, contact_email, phone, address)
SELECT * FROM (SELECT 'Reliance Digital' AS name, 'contact@reliancedigital.in' AS contact_email,
                      '1800123456' AS phone, 'Mumbai, India' AS address) AS tmp
WHERE NOT EXISTS (SELECT 1 FROM supplier WHERE name = 'Reliance Digital');

INSERT INTO supplier (name, contact_email, phone, address)
SELECT * FROM (SELECT 'Staples India' AS name, 'sales@staples.in' AS contact_email,
                      '1800654321' AS phone, 'Bengaluru, India' AS address) AS tmp
WHERE NOT EXISTS (SELECT 1 FROM supplier WHERE name = 'Staples India');
