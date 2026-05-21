ALTER TABLE mountains
    ADD COLUMN IF NOT EXISTS image_urls jsonb;

UPDATE mountains
SET image_urls = jsonb_build_array(image_url)
WHERE image_urls IS NULL
  AND image_url IS NOT NULL;
