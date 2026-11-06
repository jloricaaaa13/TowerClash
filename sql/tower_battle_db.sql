-- =====================================================================
--  DEPARTMENT TOWER CLASH - Database Script (accounts, progress, leaderboard)
--
--  YOU DO NOT HAVE TO RUN THIS. The game creates the database and the
--  tables by itself the first time it connects to MySQL.
--  It is here so you can see the design, or set it up by hand:
--      XAMPP > start MySQL > phpMyAdmin > SQL tab > paste > Go
--  Safe to run more than once (IF NOT EXISTS / INSERT IGNORE).
-- =====================================================================

CREATE DATABASE IF NOT EXISTS tower_battle_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE tower_battle_db;

-- ---------------------------------------------------------------------
-- 1. USERS  (one row per account)
--    The real password is never stored: only SHA-256(salt + password)
--    and the random salt used for that user.
--    FORGOT PASSWORD: the security question + the HASH of its answer.
--    After 5 wrong answers, resetting is locked for 15 minutes.
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS users (
    user_id        INT          NOT NULL AUTO_INCREMENT,
    username       VARCHAR(20)  NOT NULL,
    email          VARCHAR(100) NOT NULL,
    password_hash  CHAR(64)     NOT NULL COMMENT 'SHA-256 hash (hex)',
    salt           VARCHAR(44)  NOT NULL COMMENT 'random salt (Base64)',
    created_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_login     DATETIME     NULL,
    security_question     VARCHAR(100) NULL COMMENT 'used by FORGOT PASSWORD',
    security_answer_hash  CHAR(64)     NULL COMMENT 'SHA-256(salt + answer), never the answer itself',
    security_salt         VARCHAR(44)  NULL,
    reset_attempts        INT          NOT NULL DEFAULT 0 COMMENT 'wrong answers in a row',
    reset_locked_until    BIGINT       NOT NULL DEFAULT 0 COMMENT 'milliseconds; 0 = not locked',
    PRIMARY KEY (user_id),
    UNIQUE KEY uq_users_username (username),
    UNIQUE KEY uq_users_email (email)
) ENGINE = InnoDB;

-- ---------------------------------------------------------------------
-- 2. TOWERS  (lookup table: the three departments)
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS towers (
    tower_code   VARCHAR(20) NOT NULL,
    tower_name   VARCHAR(50) NOT NULL,
    department   VARCHAR(60) NOT NULL,
    short_name   VARCHAR(20) NOT NULL,
    color_hex    CHAR(7)     NOT NULL,
    PRIMARY KEY (tower_code)
) ENGINE = InnoDB;

INSERT IGNORE INTO towers VALUES
    ('HOSPITALITY', 'Grand Hotel Tower', 'Hospitality Management', 'Hospitality', '#DC2626'),
    ('EDUCATION',   'Academy Tower',     'Education',              'Education',   '#2563EB'),
    ('COMPSCI',     'Server Tower',      'Computer Science',       'Comp Sci',    '#EAB308');

-- ---------------------------------------------------------------------
-- 3. STAGES  (lookup table: stages 1-5, Endless mode = 6, LAN 1v1 = 7)
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS stages (
    stage_number   INT          NOT NULL,
    stage_name     VARCHAR(50)  NOT NULL,
    description    VARCHAR(150) NOT NULL,
    difficulty     TINYINT      NOT NULL COMMENT '1 (easy) to 5 (hard)',
    ai_multiplier  DECIMAL(4,2) NOT NULL DEFAULT 1.00,
    reward_coins   INT          NOT NULL DEFAULT 50,
    PRIMARY KEY (stage_number)
) ENGINE = InnoDB;

INSERT IGNORE INTO stages VALUES
    (1, 'Freshman Rush',    'A new enemy still learning the ropes.', 1, 1.00,  60),
    (2, 'Sophomore Siege',  'Everyone is in a hurry today.',         2, 1.20,  80),
    (3, 'Junior Clash',     'The lights are out. Towers see less.',  3, 1.45, 100),
    (4, 'Senior Showdown',  'Exam week: the enemy is tougher.',      4, 1.75, 130),
    (5, 'Dean''s Gauntlet', 'Finals. Short and brutal.',             5, 2.10, 180),
    (6, 'Endless Siege',    'Survive as long as you can!',           5, 1.00,  40),
    (7, 'LAN 1v1',          'Player against player on the same network.', 5, 1.00, 0);

-- ---------------------------------------------------------------------
-- 4. PLAYER_PROGRESS  (one row per user, saved after every match)
--    The number columns are used by the LEADERBOARD.
--    save_data keeps everything else (tower levels, lineups, stars per stage).
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS player_progress (
    user_id               INT      NOT NULL,
    coins                 INT      NOT NULL DEFAULT 0,
    unlocked_stage        INT      NOT NULL DEFAULT 1,
    matches_played        INT      NOT NULL DEFAULT 0,
    wins                  INT      NOT NULL DEFAULT 0,
    losses                INT      NOT NULL DEFAULT 0,
    high_score            INT      NOT NULL DEFAULT 0,
    best_win_seconds      INT      NOT NULL DEFAULT 0,
    total_stars           INT      NOT NULL DEFAULT 0,
    best_endless_seconds  INT      NOT NULL DEFAULT 0,
    best_endless_score    INT      NOT NULL DEFAULT 0,
    save_data             TEXT     NULL COMMENT 'everything else: tower levels, lineups, stars per stage',
    updated_at            DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id),
    CONSTRAINT fk_progress_user FOREIGN KEY (user_id)
        REFERENCES users (user_id) ON DELETE CASCADE
) ENGINE = InnoDB;

-- ---------------------------------------------------------------------
-- 5. MATCH_HISTORY  (one row per finished match)
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS match_history (
    match_id          INT         NOT NULL AUTO_INCREMENT,
    user_id           INT         NOT NULL,
    stage_number      INT         NOT NULL,
    player_tower      VARCHAR(20) NOT NULL,
    enemy_tower       VARCHAR(20) NOT NULL,
    result            ENUM('WIN', 'LOSS') NOT NULL,
    score             INT         NOT NULL DEFAULT 0,
    duration_seconds  INT         NOT NULL DEFAULT 0,
    damage_dealt      INT         NOT NULL DEFAULT 0,
    units_defeated    INT         NOT NULL DEFAULT 0,
    tower_hp_left     INT         NOT NULL DEFAULT 0 COMMENT 'percent 0-100',
    coins_earned      INT         NOT NULL DEFAULT 0,
    played_at         DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (match_id),
    KEY idx_match_user (user_id, played_at),
    CONSTRAINT fk_match_user   FOREIGN KEY (user_id)      REFERENCES users (user_id) ON DELETE CASCADE,
    CONSTRAINT fk_match_ptower FOREIGN KEY (player_tower) REFERENCES towers (tower_code),
    CONSTRAINT fk_match_etower FOREIGN KEY (enemy_tower)  REFERENCES towers (tower_code),
    CONSTRAINT fk_match_stage  FOREIGN KEY (stage_number) REFERENCES stages (stage_number)
) ENGINE = InnoDB;


-- ---------------------------------------------------------------------
-- Handy queries to show your teacher
-- ---------------------------------------------------------------------
-- Leaderboard (top 10 by high score):
--   SELECT u.username, p.high_score FROM player_progress p
--   JOIN users u ON u.user_id = p.user_id
--   WHERE p.high_score > 0 ORDER BY p.high_score DESC LIMIT 10;
--
-- Last 10 matches of one player:
--   SELECT * FROM match_history WHERE user_id = 1 ORDER BY played_at DESC LIMIT 10;
