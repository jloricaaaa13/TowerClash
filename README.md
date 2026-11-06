# ⚔️ TowerClash

Isang 2D/isometric strategy tower defense game na binuo gamit ang Java (Swing / OOP) at MySQL para sa database administration.

---

## 📌 Talaan ng Nilalaman
- [Tungkol sa Proyekto](#-tungkol-sa-proyekto)
- [Mga Pangunahing Tampok](#-mga-pangunahing-tampok)
- [Tech Stack at Tools](#-tech-stack-at-tools)
- [Estruktura ng Proyekto](#-estruktura-ng-proyekto)
- [Mga Kinakailangan (Prerequisites)](#-mga-kinakailangan-prerequisites)
- [Gabay sa Pag-install at Pagpapatakbo](#-gabay-sa-pag-install-at-pagpapatakbo)
- [Database Setup](#-database-setup)

---

## 📖 Tungkol sa Proyekto

Ang **TowerClash** ay isang tactical defense game kung saan kailangang protektahan ng mga manlalaro ang kanilang base laban sa mga paparating na waves gamit ang iba't ibang units at defensive towers. Dinisenyo ito gamit ang Object-Oriented Programming (OOP) principles sa Java at naglalaman ng mga custom scenes, unit pathfinding, at game state management.

---

## ✨ Mga Pangunahing Tampok

- **Authentication System:** Login at registration scene na may kasamang security questions para sa account recovery.
- **Dynamic Gameplay & Stages:** Stage selection na may iba't ibang lebel ng kahirapan at natatanging mapa.
- **Tower & Unit Deployment:** Pagpili ng mga tore at units gamit ang interactive card-based UI (TowerCards at UnitCards).
- **Upgrades & Lineup Customization:** Sistema para sa pag-upgrade ng attributes ng mga tore at pag-customize ng lineup bago sumabak sa laban.
- **Match History at Profile:** Pagsusubaybay sa mga nakaraang laban at istatistika ng manlalaro gamit ang MySQL database.

---

## 🛠 Tech Stack at Tools

| Bahagi | Teknolohiya / Tool |
| :--- | :--- |
| **Programming Language** | Java (JDK 8 o mas mataas) |
| **Development Environment** | NetBeans IDE |
| **UI Framework** | Java Swing / AWT |
| **Database** | MySQL (XAMPP / phpMyAdmin) |
| **Database Connector** | MySQL Connector/J |
| **Version Control** | Git at GitHub |

---

## 📂 Estruktura ng Proyekto

```text
TowerBattle/
├── lib/               # External libraries at JAR files (tulad ng MySQL driver)
├── nbproject/         # NetBeans project configuration
├── sql/               # SQL scripts para sa database tables at initial data
├── src/               # Pangunahing Java source code
│   └── ui/
│       └── scenes/    # UI screens (Login, StageSelect, GameView, atbp.)
├── .gitignore         # Git ignore rules
└── README.md          # Dokumentasyon ng proyekto
