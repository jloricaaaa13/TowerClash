-- =====================================================================
--  DEPARTMENT TOWER CLASH - LAN SERVER SETUP (run ONCE on the server computer)
--
--  Lets the other computers in the same Wi-Fi/LAN use this computer's
--  MySQL, so everybody shares the same accounts and leaderboard.
--  XAMPP > start MySQL > phpMyAdmin > SQL tab > paste > Go
--
--  The other computers then use (Login screen > DATABASE):
--      SERVER IP:  this computer's IPv4 address (cmd > ipconfig)
--      PORT:       3306
--      MYSQL USER: towergame
--      PASSWORD:   tower123      (you can change it below)
-- =====================================================================

CREATE DATABASE IF NOT EXISTS tower_battle_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

-- A user that may connect from any computer ('%') but can ONLY use the game database
CREATE USER IF NOT EXISTS 'towergame'@'%' IDENTIFIED BY 'tower123';
GRANT ALL PRIVILEGES ON tower_battle_db.* TO 'towergame'@'%';
FLUSH PRIVILEGES;
