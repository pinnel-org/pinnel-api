ALTER TABLE pins RENAME COLUMN description TO overview;
ALTER TABLE pins ADD COLUMN visitor_tips VARCHAR(2000);
ALTER TABLE pins ADD COLUMN history      VARCHAR(2000);
