ALTER TABLE users
    ADD COLUMN guide_tour_completed_home     BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN guide_tour_completed_wardrobe BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN guide_tour_completed_feed     BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN guide_tour_completed_mypage   BOOLEAN NOT NULL DEFAULT FALSE;