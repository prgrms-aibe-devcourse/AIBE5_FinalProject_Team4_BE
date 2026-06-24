-- RECO / FEED domain tables (룩피드 FEED-001~008)
-- ddl-auto: update 환경에서는 참고용. 신규 환경 bootstrap 시 실행.

CREATE TABLE IF NOT EXISTS feed_posts (
    feed_post_id   BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id        BIGINT       NOT NULL,
    outfit_id      BIGINT       NULL,
    caption        TEXT         NOT NULL,
    hidden         TINYINT(1)   NOT NULL DEFAULT 0,
    deleted_at     DATETIME(6)  NULL,
    created_at     DATETIME(6)  NULL,
    updated_at     DATETIME(6)  NULL,
    INDEX idx_feed_posts_public (deleted_at, hidden, created_at),
    INDEX idx_feed_posts_user (user_id, deleted_at, created_at)
);

CREATE TABLE IF NOT EXISTS feed_post_images (
    feed_post_image_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    feed_post_id       BIGINT       NOT NULL,
    image_url          VARCHAR(500) NOT NULL,
    sort_order         INT          NOT NULL,
    created_at         DATETIME(6)  NULL,
    updated_at         DATETIME(6)  NULL,
    INDEX idx_feed_post_images_post (feed_post_id, sort_order)
);

CREATE TABLE IF NOT EXISTS feed_likes (
    feed_like_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id      BIGINT NOT NULL,
    feed_post_id BIGINT NOT NULL,
    created_at   DATETIME(6) NULL,
    updated_at   DATETIME(6) NULL,
    UNIQUE KEY uk_feed_likes_user_post (user_id, feed_post_id),
    INDEX idx_feed_likes_post (feed_post_id)
);

CREATE TABLE IF NOT EXISTS feed_comments (
    feed_comment_id    BIGINT AUTO_INCREMENT PRIMARY KEY,
    feed_post_id       BIGINT NOT NULL,
    user_id            BIGINT NOT NULL,
    parent_comment_id  BIGINT NULL,
    content            TEXT   NOT NULL,
    deleted_at         DATETIME(6) NULL,
    created_at         DATETIME(6) NULL,
    updated_at         DATETIME(6) NULL,
    INDEX idx_feed_comments_post (feed_post_id, deleted_at, created_at),
    INDEX idx_feed_comments_parent (parent_comment_id)
);

CREATE TABLE IF NOT EXISTS user_follows (
    user_follow_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    follower_id    BIGINT NOT NULL,
    followee_id    BIGINT NOT NULL,
    created_at     DATETIME(6) NULL,
    updated_at     DATETIME(6) NULL,
    UNIQUE KEY uk_user_follows_follower_followee (follower_id, followee_id),
    INDEX idx_user_follows_followee (followee_id)
);
