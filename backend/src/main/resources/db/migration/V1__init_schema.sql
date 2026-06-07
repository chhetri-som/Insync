-- enable UUID generation extension
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ENUMS

CREATE TYPE album_style AS ENUM (
    'CHRONOLOGICAL',
    'COLOR_PALETTE',
    'PORTRAIT_GALLERY',
    'HIGH_CONTRAST'
);

CREATE TYPE album_status AS ENUM (
    'DRAFT',
    'PROCESSING',
    'READY',
    'ERROR'
);

CREATE TYPE photo_processing_status AS ENUM (
    'PENDING',
    'PROCESSING',
    'DONE',
    'FAILED'
);

CREATE TYPE layout_type as ENUM (
    'ALGORITHM',
    'USER_OVERRIDE'
);

-- USERS

CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    display_name VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT  NOW()
);

CREATE INDEX idx_users_email ON users(email);

-- ALBUMS

CREATE TABLE albums (
    id UUID PRIMARY KEY  DEFAULT  gen_random_uuid(),
    owner_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL,
    style album_style NOT NULL DEFAULT 'CHRONOLOGICAL',
    status album_status NOT NULL DEFAULT 'DRAFT',
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_albums_owner ON albums(owner_id);

-- PHOTOS

CREATE TABLE photos (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    album_id UUID NOT NULL REFERENCES albums(id) ON DELETE CASCADE,
    original_storage_key VARCHAR(500) NOT NULL,
    processed_storage_key VARCHAR(500),
    thumbnail_storage_key VARCHAR(500),
    processing_status photo_processing_status NOT NULL DEFAULT 'PENDING',
    upload_order INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_photos_album ON photos(album_id);
CREATE INDEX idx_photos_status ON photos(processing_status);

-- EXIF + CV analysis

CREATE TABLE photo_metadata (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    photo_id UUID NOT NULL UNIQUE REFERENCES photos(id) ON DELETE CASCADE,
    taken_at TIMESTAMP,
    latitude DECIMAL(10, 7),
    longitude DECIMAL(10, 7),
    dominant_colors JSONB, -- hex, weight
    brightness_score FLOAT,
    contrast_score FLOAT,
    complexity_score FLOAT,
    has_faces BOOLEAN NOT NULL DEFAULT FALSE,
    face_count INTEGER NOT NULL DEFAULT 0
);

CREATE INDEX idx_photo_metadata_photo ON photo_metadata(photo_id);
CREATE INDEX idx_photo_metadata_taken_at ON photo_metadata(taken_at);

-- ALBUM LAYOUTS
-- One row = one complete layout snapshot.
--
-- `pages` is a JSONB document representing the full page/slot tree:
-- [
--   {
--     "pageNumber": 1,
--     "slots": [
--       {
--         "photoId":    "uuid-string",
--         "position":   0,
--         "cropX":      0.0,
--         "cropY":      0.0,
--         "cropWidth":  1.0,
--         "cropHeight": 1.0,
--         "isFullPage": false
--       }
--     ]
--   }
-- ]
--
-- Two layout rows can exist per album at most:
--   1. layout_type = ALGORITHM    (always present once processing completes)
--   2. layout_type = USER_OVERRIDE (present only after the user edits)
-- The GET /layout endpoint merges them in Java — USER_OVERRIDE wins conflicts.

CREATE TABLE album_layouts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    album_id UUID NOT NULL REFERENCES albums(id) ON DELETE CASCADE,
    layout_type layout_type NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    cover_photo_id UUID REFERENCES photos(id) ON DELETE SET NULL,
    pages JSONB NOT NULL DEFAULT '[]',
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_layouts_album ON album_layouts(album_id);
CREATE INDEX idx_layouts_active ON album_layouts(album_id, layout_type) WHERE is_active = TRUE;

-- update_at

CREATE OR REPLACE FUNCTION update_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at =  NOW();
    RETURN NEW;
END
$$ LANGUAGE plpgsql;

CREATE TRIGGER album_updated_at
    BEFORE UPDATE ON albums
    FOR EACH ROW EXECUTE FUNCTION update_updated_at();
