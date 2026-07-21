ALTER TABLE restaurants ADD COLUMN menu VARCHAR(100);
ALTER TABLE restaurants ADD COLUMN description VARCHAR(255);
ALTER TABLE restaurants ADD COLUMN map_url TEXT;
ALTER TABLE restaurants ADD COLUMN blog_url TEXT;

ALTER TABLE restaurant_sections ADD COLUMN menu VARCHAR(100);
ALTER TABLE restaurant_sections ADD COLUMN description VARCHAR(255);
ALTER TABLE restaurant_sections ADD COLUMN map_url TEXT;
ALTER TABLE restaurant_sections ADD COLUMN blog_url TEXT;