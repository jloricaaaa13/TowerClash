package ui.scenes;

import audio.Sfx;
import game.Battle;
import game.EnemyAI;
import game.effects.Effect;
import java.awt.Color;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import model.Department;
import model.Match;
import model.MatchReward;
import model.PlayerProgress;
import model.Projectile;
import model.Side;
import model.Stage;
import model.Tower;
import model.UltimateSkill;
import model.Unit;
import model.UnitType;
import net.LanMatch;
import ui.GameWindow;
import ui.hud.UltimateButton;
import ui.hud.UnitButton;
import ui.hud.UpgradeButton;
import ui.pixel.Landscape;
import ui.pixel.Palette;
import ui.pixel.PixelArt;
import ui.pixel.PixelButton;
import ui.pixel.PixelFont;
import ui.pixel.PixelWidget;

/**
 * The battle screen: draws the Battle and the HUD, and turns clicks / keys into actions.
 *
 * Game loop: a Swing Timer calls update() about 60 times per second.
 * update() measures the real time since the last frame and moves the Battle
 * forward by that much (twice as much with Speed x2). Then everything is repainted.
 *
 * Extras: a VS screen before the fight, tips during the first matches, tooltips when
 * hovering a button, screen shake, and an automatic pause when the window loses focus.
 *
 * Hotkeys: 1 2 3 = spawn   U = upgrade   R = ultimate   F = speed   P / Esc = pause
 */
public class BattleScene extends PixelScene {

    private static final int PANEL_Y = 127;
    private static final int BUTTON_Y = 139;
    private static final int BUTTON_H = 38;
    private static final double MAX_STEP = 1 / 60.0;
    private static final double RESULTS_DELAY = 1.5;  // seconds to watch the tower fall
    private static final double INTRO_SECONDS = 5;    // VS screen closes by itself after this
    private static final double HINT_SECONDS = 5;
    private static final double LAN_COUNTDOWN = 4;     // LAN: seconds before the battle starts
    private static final int[] TREES = {66, 254};
    private static final int[] FAR_TREES = {110, 160, 210};

    private final UnitButton[] unitButtons = new UnitButton[3];
    private final UpgradeButton upgradeButton;
    private final UltimateButton ultimateButton;
    private final PixelButton speedButton;
    private final Random random = new Random();

    private Landscape landscape = new Landscape(Battle.GROUND_Y, TREES, FAR_TREES);
    private Battle battle;
    private EnemyAI enemyAI;           // the computer opponent (null in LAN 1v1)
    private Side mySide = Side.PLAYER; // the tower controlled on this computer
    private LanMatch lan;              // not null = LAN 1v1 against another computer
    private double lanClock;           // real time not yet turned into LAN ticks
    private boolean surrendered;
    private Stage stage;
    private boolean fastSpeed;
    private boolean resultsShown;
    private double endTimer;
    private long lastFrameNanos;
    private boolean intro;
    private double introTime;

    // Tips for new players (each shown once per match)
    private boolean showTips;
    private final boolean[] tipShown = new boolean[5];
    private String tipText;
    private double tipTime;

    public BattleScene(GameWindow window) {
        super(window);
        setFrameDelay(16); // about 60 FPS

        for (int i = 0; i < 3; i++) {
            int slot = i;
            unitButtons[i] = addWidget(new UnitButton(slot, 2 + i * 64, BUTTON_Y, 62, BUTTON_H, () -> trySpawn(slot)));
        }
        upgradeButton = addWidget(new UpgradeButton(194, BUTTON_Y, 42, BUTTON_H, this::tryUpgrade));
        ultimateButton = addWidget(new UltimateButton(238, BUTTON_Y, 50, BUTTON_H, this::tryUltimate));
        speedButton = addWidget(new PixelButton("X1", 290, BUTTON_Y, 28, 18, this::toggleSpeed));
        addWidget(new PixelButton("||", 290, BUTTON_Y + 20, 28, 18, this::pause));

        bindKey("1", () -> trySpawn(0));
        bindKey("2", () -> trySpawn(1));
        bindKey("3", () -> trySpawn(2));
        bindKey("U", this::tryUpgrade);
        bindKey("R", this::tryUltimate);
        bindKey("F", this::toggleSpeed);
        bindKey("P", this::pause);
    }

    /**
     * Starts a new match. You bring the lineup you picked on the Lineup screen.
     * The enemy randomly gets one of the other two departments and a random lineup
     * (always one FAST, one RANGED and one TANK unit).
     */
    public void start(Department player, Stage stage) {
        this.stage = stage;
        List<Department> others = new ArrayList<>(List.of(Department.values()));
        others.remove(player);
        Department enemy = others.get(random.nextInt(others.size()));

        PlayerProgress progress = window.getProgress();
        long seed = System.nanoTime();
        List<UnitType> enemyLineup = enemy.randomEnemyLineup(random);
        battle = Battle.forStage(player, progress.getTowerLevel(player), progress.getLineup(player),
                enemy, enemyLineup, stage, seed);
        battle.setSoundListener(window.getSound()::play);
        enemyAI = new EnemyAI(battle, Side.ENEMY, stage, seed + 1);
        lan = null;
        mySide = Side.PLAYER;
        setUpScreen();
        showTips = progress.getMatchesPlayed() < 2 && !stage.endless();
    }

    /**
     * Starts a LAN 1v1 match. Both computers build the same battle; this computer controls
     * the left tower if it is the host, or the right tower if it joined.
     */
    public void startLan(LanMatch match) {
        this.stage = Stage.LAN;
        lan = match;
        mySide = match.getMySide();
        battle = match.createBattle();
        battle.setSoundListener(window.getSound()::play);
        enemyAI = null;
        lanClock = 0;
        surrendered = false;
        setUpScreen();
        showTips = false;
    }

    /** What every new match needs (single player or LAN). */
    private void setUpScreen() {
        Unit.setFriendlySide(mySide);
        landscape = new Landscape(Battle.GROUND_Y, TREES, FAR_TREES, stage.theme());
        for (UnitButton b : unitButtons) {
            b.setBattle(battle, mySide);
        }
        upgradeButton.setBattle(battle, mySide);
        ultimateButton.setBattle(battle, mySide);

        fastSpeed = false;
        speedButton.setText("X1");
        resultsShown = false;
        endTimer = 0;
        lastFrameNanos = 0;
        intro = true;
        introTime = 0;
        java.util.Arrays.fill(tipShown, false);
        tipText = null;
    }

    /** The other player's tower side. */
    private Side foeSide() {
        return mySide.opponent();
    }

    // ------------------------------------------------------------------
    //  Game loop
    // ------------------------------------------------------------------

    @Override
    protected void update() {
        if (battle == null) {
            return;
        }
        long now = System.nanoTime();
        double elapsed = lastFrameNanos == 0 ? MAX_STEP : Math.min((now - lastFrameNanos) / 1e9, 0.1);
        lastFrameNanos = now;
        if (isDialogOpen() && (lan == null || resultsShown)) {
            return; // paused (pause menu or results). A LAN battle never pauses.
        }
        if (intro && lan != null) {
            introTime += elapsed; // LAN: both computers count down together, it cannot be skipped
            if (introTime >= LAN_COUNTDOWN) {
                endIntro();
            }
            return;
        }
        if (intro) {
            if (window.isFocused()) {
                introTime += elapsed; // wait while the player is in another window
            }
            if (introTime >= INTRO_SECONDS) {
                endIntro();
            }
            return;
        }
        if (lan != null) {
            updateLan(elapsed);
            return;
        }
        double gameTime = elapsed * (fastSpeed ? 2 : 1);
        while (gameTime > 1e-9) {
            double step = Math.min(MAX_STEP, gameTime);
            enemyAI.update(step);
            battle.update(step);
            gameTime -= step;
            if (battle.isOver()) {
                endTimer += step;
            }
        }
        updateTips(elapsed);
        if (battle.isOver() && !resultsShown && endTimer >= RESULTS_DELAY) {
            showResults();
        }
    }

    /** LAN 1v1: turn real time into ticks, but only as fast as the other computer allows. */
    private void updateLan(double elapsed) {
        if (battle.isOver() || lan.isFinished() || surrendered) {
            battle.update(elapsed); // only the effects still move; the result is already decided
            endTimer += elapsed;
            boolean decided = battle.isOver();
            if (!resultsShown && (!decided || endTimer >= RESULTS_DELAY)) {
                showLanResults();
            }
            return;
        }
        lanClock = Math.min(lanClock + elapsed, 0.5);
        while (lanClock >= LanMatch.TICK && lan.step(battle)) {
            lanClock -= LanMatch.TICK;
            if (battle.isOver()) {
                break;
            }
        }
    }

    @Override
    protected boolean interceptInput() {
        if (intro && lan != null) {
            return true; // LAN: wait for the countdown
        }
        if (intro) {
            endIntro(); // any click or Enter skips the VS screen
            return true;
        }
        return false;
    }

    private void endIntro() {
        intro = false;
        battle.showBanner(stage.endless() ? "SURVIVE!" : "FIGHT!", Palette.TEXT_GOLD);
        window.getSound().play(Sfx.SPAWN);
    }

    @Override
    public void onWindowFocusLost() {
        if (lan == null) {
            pause(); // alt-tab or minimize: never lose a match without knowing
        }
    }

    // ------------------------------------------------------------------
    //  Tips for new players
    // ------------------------------------------------------------------

    private void updateTips(double dt) {
        if (tipText != null) {
            tipTime -= dt;
            if (tipTime <= 0) {
                tipText = null;
            }
            return;
        }
        if (!showTips || battle.isOver()) {
            return;
        }
        double t = battle.getMatch().getElapsed();
        Tower me = battle.getTower(mySide);
        tryTip(0, t > 0.5, "SPAWN UNITS: CLICK A BUTTON BELOW OR PRESS 1 2 3");
        tryTip(1, t > 12, "FAST BEATS RANGED, RANGED BEATS TANK, TANK BEATS FAST");
        tryTip(2, t > 20 && me.getLevel() == 1 && battle.canUpgrade(mySide), "TIP: PRESS U TO UPGRADE YOUR TOWER");
        tryTip(3, me.getUltimate().isReady(), "YOUR ULTIMATE IS READY! PRESS R");
        tryTip(4, battle.getTower(foeSide()).getUltimate().isReady(), "WATCH OUT! THE ENEMY ULTIMATE IS READY");
    }

    private void tryTip(int index, boolean condition, String text) {
        if (tipText == null && !tipShown[index] && condition) {
            tipShown[index] = true;
            tipText = text;
            tipTime = HINT_SECONDS;
        }
    }

    // ------------------------------------------------------------------
    //  Player actions
    // ------------------------------------------------------------------

    private boolean canAct() {
        return battle != null && !battle.isOver() && !isDialogOpen() && !intro;
    }

    private void error(String message) {
        showToast(message);
        window.getSound().play(Sfx.ERROR);
    }

    private void trySpawn(int slot) {
        if (!canAct()) {
            return;
        }
        if (lan != null) {
            if (battle.canSpawn(mySide, slot)) {
                lan.queue("S" + slot); // happens on both computers a moment later
                return;
            }
        } else if (battle.spawn(mySide, slot)) {
            return;
        }
        Tower tower = battle.getTower(mySide);
        if (battle.getSpawnDisabledTime(mySide) > 0) {
            error("SPAWNING DISABLED!");
        } else if (battle.getSpawnCooldownRatio(mySide, slot) > 0) {
            error("NOT READY YET");
        } else if (battle.getEnergy(mySide) < tower.getUnitTypes().get(slot).cost()) {
            error("NOT ENOUGH ENERGY");
        }
    }

    private void tryUpgrade() {
        if (!canAct()) {
            return;
        }
        if (lan != null && battle.canUpgrade(mySide)) {
            lan.queue("U");
            return;
        }
        if (lan != null || !battle.upgradeTower(mySide)) {
            error(battle.getTower(mySide).getUpgradeCost() < 0 ? "TOWER IS MAX LEVEL" : "NOT ENOUGH ENERGY");
        }
    }

    private void tryUltimate() {
        if (!canAct()) {
            return;
        }
        if (lan != null && battle.getTower(mySide).getUltimate().isReady()) {
            lan.queue("R");
            return;
        }
        if (lan != null || !battle.useUltimate(mySide)) {
            error("ULTIMATE NOT READY");
        }
    }

    private void toggleSpeed() {
        if (!canAct()) {
            return;
        }
        if (lan != null) {
            error("NO SPEED-UP IN LAN 1V1");
            return;
        }
        fastSpeed = !fastSpeed;
        speedButton.setText(fastSpeed ? "X2" : "X1");
    }

    private void pause() {
        if (!canAct()) {
            return;
        }
        if (lan != null) {
            showDialog("SURRENDER?", new String[]{"THE BATTLE DOES NOT STOP IN LAN 1V1!",
                "SURRENDER = THE OTHER PLAYER WINS."}, 0, true,
                    new DialogButton("KEEP PLAYING", null),
                    new DialogButton("SURRENDER", () -> {
                        lan.surrender();
                        surrendered = true;
                    }));
            return;
        }
        showDialog("PAUSED", new String[]{
            "1 2 3 = SPAWN UNITS    U = UPGRADE",
            "R = ULTIMATE    F = SPEED    P = PAUSE",
            "",
            "FAST > RANGED > TANK > FAST"
        }, 0, true,
                new DialogButton("RESUME", null),
                new DialogButton("QUIT", window::showStageSelect));
    }

    @Override
    protected void onEscape() {
        pause();
    }

    // ------------------------------------------------------------------
    //  End of match
    // ------------------------------------------------------------------

    private void showResults() {
        resultsShown = true;
        Match match = battle.getMatch();
        PlayerProgress progress = window.getProgress();
        MatchReward reward = progress.recordMatch(match);
        boolean saved = window.saveProgress();
        window.recordMatch(match);
        boolean win = match.isPlayerWin();
        window.getSound().play(win || (stage.endless() && reward.newRecord()) ? Sfx.VICTORY : Sfx.DEFEAT);

        List<String> lines = new ArrayList<>();
        List<DialogButton> buttons = new ArrayList<>();
        String title;
        if (stage.endless()) {
            title = win ? "VICTORY!" : "SURVIVED " + formatTime(match.getSeconds());
            lines.add("WAVE REACHED: " + battle.getEndlessWave() + "     UNITS DEFEATED: " + match.getUnitsDefeated());
            lines.add("DAMAGE DEALT: " + (int) match.getDamageDealt());
            lines.add("");
            lines.add("SCORE: " + match.getScore());
            lines.add(reward.newRecord() ? "NEW RECORD!" : "BEST: " + formatTime(progress.getBestEndlessSeconds()));
            lines.add("COINS EARNED: +" + match.getCoinsEarned());
        } else {
            title = win ? "VICTORY!" : "DEFEAT";
            lines.add("TIME: " + formatTime(match.getSeconds()) + "     UNITS DEFEATED: " + match.getUnitsDefeated());
            lines.add("DAMAGE DEALT: " + (int) match.getDamageDealt()
                    + "     TOWER HP LEFT: " + Math.round(match.getTowerHpLeftRatio() * 100) + "%");
            lines.add("");
            lines.add("SCORE: " + match.getScore() + "     STARS: " + starText(reward.stars()));
            String coins = "COINS EARNED: +" + (match.getCoinsEarned() + reward.starCoins());
            if (reward.newStars() > 0) {
                coins += "  (" + reward.newStars() + " NEW STAR" + (reward.newStars() > 1 ? "S" : "") + "!)";
            }
            lines.add(coins);
            if (reward.unlocked()) {
                lines.add(progress.isEndlessUnlocked() && stage.number() == Stage.ENDLESS_UNLOCK_AFTER
                        ? "NEW STAGE + ENDLESS MODE UNLOCKED!" : "NEW STAGE UNLOCKED!");
            } else if (win && stage.isLast()) {
                lines.add("CAMPAIGN COMPLETE!");
            }
            if (win && !stage.isLast()) {
                buttons.add(new DialogButton("NEXT", () -> window.startBattle(Stage.get(stage.number() + 1))));
            }
        }
        if (!saved) {
            lines.add("(COULD NOT SAVE - CHECK THE DATABASE)");
        }
        buttons.add(new DialogButton("RETRY", () -> window.startBattle(stage)));
        buttons.add(new DialogButton("STAGES", window::showStageSelect));
        buttons.add(new DialogButton("HOME", window::showMainMenu));
        showDialog(title, lines.toArray(new String[0]), 0, false, buttons.toArray(new DialogButton[0]));
    }

    /** End of a LAN 1v1 match: who won, save the result, close the connection. */
    private void showLanResults() {
        resultsShown = true;
        String opponent = lan.getOpponent().name();
        Boolean won; // null = no result
        String reason;
        if (surrendered) {
            won = false;
            reason = "YOU SURRENDERED";
        } else if (battle.isOver()) {
            won = battle.getWinner() == mySide;
            reason = won ? "YOU DESTROYED " + opponent + "'S TOWER!" : opponent + " DESTROYED YOUR TOWER";
        } else if (lan.isOpponentQuit()) {
            won = true;
            reason = opponent + " SURRENDERED";
        } else if (lan.isOutOfSync()) {
            won = null;
            reason = "THE TWO GAMES WENT OUT OF SYNC";
        } else {
            won = null;
            reason = "CONNECTION TO " + opponent + " WAS LOST";
        }
        int seconds = battle.getMatch().getSeconds();
        PlayerProgress progress = window.getProgress();
        if (won != null) {
            progress.recordLanMatch(won);
            window.saveProgress();
            window.recordLanMatch(battle.getTower(mySide).getDepartment(),
                    battle.getTower(foeSide()).getDepartment(), won, seconds);
        }
        lan.close();
        window.getSound().play(Boolean.TRUE.equals(won) ? Sfx.VICTORY : Sfx.DEFEAT);

        String title = won == null ? "NO CONTEST" : won ? "VICTORY!" : "DEFEAT";
        String[] lines = {
            window.getAccount().username().toUpperCase() + "  VS  " + opponent.toUpperCase(),
            reason,
            "",
            "TIME: " + formatTime(seconds),
            won == null ? "THIS MATCH DOES NOT COUNT" : "YOUR LAN 1V1 RECORD: " + progress.getLanWins()
                    + " WIN" + (progress.getLanWins() == 1 ? "" : "S") + " - " + progress.getLanLosses() + " LOSS"
                    + (progress.getLanLosses() == 1 ? "" : "ES"),
        };
        showDialog(title, lines, 0, false,
                new DialogButton("LAN LOBBY", window::showLanLobby),
                new DialogButton("HOME", window::showMainMenu));
    }

    /** 2 stars = "* * -" */
    private static String starText(int stars) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 3; i++) {
            sb.append(i < stars ? "*" : "-");
            if (i < 2) {
                sb.append(' ');
            }
        }
        return sb.toString();
    }

    private static String formatTime(int seconds) {
        return seconds / 60 + ":" + String.format("%02d", seconds % 60);
    }

    // ------------------------------------------------------------------
    //  Drawing
    // ------------------------------------------------------------------

    @Override
    protected void paintScene(Graphics2D g) {
        if (battle == null) {
            landscape.draw(g, tick);
            return;
        }
        // World (shakes)
        int dx = 0;
        int dy = 0;
        if (battle.getShake() > 0.3) {
            dx = (int) Math.round((random.nextDouble() * 2 - 1) * battle.getShake());
            dy = (int) Math.round((random.nextDouble() * 2 - 1) * battle.getShake() * 0.5);
        }
        g.translate(dx, dy);
        landscape.draw(g, tick);
        battle.getTower(Side.PLAYER).draw(g, Battle.GROUND_Y, tick);
        battle.getTower(Side.ENEMY).draw(g, Battle.GROUND_Y, tick);
        if (lan != null) { // LAN: point at your own tower
            Tower mine = battle.getTower(mySide);
            int bob = (tick / 10) % 2;
            PixelFont.drawCentered(g, "YOU", (int) mine.centerX(), Battle.GROUND_Y - Tower.HEIGHT - 22 - bob, 1,
                    Palette.TEXT_GOLD, Palette.TEXT_DARK);
        }
        List<Unit> sorted = new ArrayList<>(battle.getUnits());
        sorted.sort(Comparator.comparingInt(Unit::getLane)); // back lane first
        for (Unit u : sorted) {
            u.draw(g, Battle.GROUND_Y, tick);
        }
        for (Projectile p : battle.getProjectiles()) {
            p.draw(g, tick);
        }
        for (Effect e : battle.getEffects()) {
            e.draw(g, tick);
        }
        landscape.drawWeather(g, tick);
        g.translate(-dx, -dy);

        // HUD (does not shake)
        drawTopHud(g);
        drawBanner(g);
        if (lan != null && !intro && !battle.isOver() && !lan.isFinished() && lan.secondsSinceLastMessage() > 1.0) {
            PixelFont.drawCentered(g, "WAITING FOR " + lan.getOpponent().name().toUpperCase() + "...", W / 2, 50, 1,
                    Palette.TEXT_OFF, Palette.TEXT_DARK);
        }
        drawTip(g);
        drawBottomPanel(g);
        drawTooltip(g);
        if (intro) {
            drawIntro(g);
        }
    }

    private void drawTopHud(Graphics2D g) {
        g.setColor(new Color(0, 0, 0, 90));
        g.fillRect(0, 0, W, 21);

        Tower me = battle.getTower(Side.PLAYER);
        Tower foe = battle.getTower(Side.ENEMY);
        String myName = me.getDepartment().getTowerShortName() + " LV" + me.getLevel();
        String foeName = "LV" + foe.getLevel() + " " + foe.getDepartment().getTowerShortName();
        if (lan != null) { // LAN: show who plays each tower
            myName = shortName(lan.nameOf(Side.PLAYER)) + " - " + myName;
            foeName = foeName + " - " + shortName(lan.nameOf(Side.ENEMY));
        }
        PixelFont.drawOutlined(g, myName, 4, 2, 1,
                Palette.lighter(me.getDepartment().getColor(), 0.2f), Palette.TEXT_DARK);
        PixelFont.drawOutlined(g, foeName, W - 4 - PixelFont.width(foeName, 1), 2, 1,
                Palette.lighter(foe.getDepartment().getColor(), 0.2f), Palette.TEXT_DARK);
        drawHpBar(g, 4, 12, 120, me);
        drawHpBar(g, W - 124, 12, 120, foe);

        Match match = battle.getMatch();
        String time = formatTime(match.getSeconds());
        Color timeColor = Palette.TEXT_WHITE;
        if (stage.endless()) {
            time += " W" + battle.getEndlessWave();
        } else if (battle.isOvertime()) {
            time += " OT";
            timeColor = Palette.TEXT_OFF;
        }
        PixelFont.drawCentered(g, time, W / 2, 2, 1, timeColor, Palette.TEXT_DARK);
        PixelFont.drawCentered(g, lan != null ? "LAN 1V1" : match.getScore() + " PTS", W / 2, 12, 1,
                Palette.TEXT_GOLD, Palette.TEXT_DARK);
    }

    private void drawHpBar(Graphics2D g, int x, int y, int w, Tower tower) {
        double ratio = tower.hpRatio();
        Color fill = ratio > 0.5 ? Palette.HP_GOOD : ratio > 0.25 ? Palette.HP_MID : Palette.HP_LOW;
        PixelArt.bar(g, x, y, w, 7, ratio, fill);
        String text = (int) Math.ceil(tower.getHp()) + "/" + (int) tower.getMaxHp();
        PixelFont.drawCentered(g, text, x + w / 2, y, 1, Palette.TEXT_WHITE, Palette.TEXT_DARK);
    }

    private void drawBanner(Graphics2D g) {
        String text = battle.getBannerText();
        if (text != null && !intro) {
            PixelFont.drawCentered(g, text, W / 2, 30, 2, battle.getBannerColor(), Palette.TEXT_DARK);
        }
    }

    private void drawTip(Graphics2D g) {
        if (tipText == null) {
            return;
        }
        int tw = PixelFont.width(tipText, 1) + 16;
        int tx = (W - tw) / 2;
        PixelArt.board(g, tx, 50, tw, 15);
        PixelFont.drawOutlined(g, tipText, tx + 8, 54, 1, Palette.TEXT_GOLD, Palette.TEXT_DARK);
    }

    private void drawBottomPanel(Graphics2D g) {
        g.setColor(Palette.PANEL);
        g.fillRect(0, PANEL_Y, W, H - PANEL_Y);
        g.setColor(Palette.WOOD_OUTLINE);
        g.fillRect(0, PANEL_Y, W, 1);
        g.setColor(Palette.WOOD_DARK);
        g.fillRect(0, PANEL_Y + 1, W, 1);

        // Player energy
        double energy = battle.getEnergy(mySide);
        PixelArt.bar(g, 3, 130, 188, 7, energy / Battle.MAX_ENERGY, Palette.ENERGY);
        PixelFont.drawCentered(g, "ENERGY " + (int) energy + "/" + (int) Battle.MAX_ENERGY, 97, 130, 1,
                Palette.TEXT_WHITE, Palette.TEXT_DARK);

        // Enemy ultimate meter (so the player can prepare)
        Tower foe = battle.getTower(foeSide());
        boolean foeReady = foe.getUltimate().isReady();
        PixelArt.bar(g, 195, 130, 122, 7, foe.getUltimate().getMeter() / UltimateSkill.METER_MAX,
                foeReady ? Palette.HP_LOW : Palette.lighter(foe.getDepartment().getColor(), 0.1f));
        PixelFont.drawCentered(g, foeReady ? "ENEMY ULT READY!" : "ENEMY ULTIMATE", 256, 130, 1,
                Palette.TEXT_WHITE, Palette.TEXT_DARK);
    }

    /** Explains the button under the mouse. */
    private void drawTooltip(Graphics2D g) {
        PixelWidget target = getTooltipTarget();
        List<String> lines = new ArrayList<>();
        List<Color> colors = new ArrayList<>();
        if (target instanceof UnitButton ub && ub.getPreview() != null) {
            Unit u = ub.getPreview();
            lines.add(u.getName() + "  (" + u.getRole().getLabel() + ")");
            colors.add(u.getRole().getColor());
            lines.add("STRONG VS " + u.getRole().beats().getLabel() + "  -  WEAK VS " + u.getRole().counteredBy().getLabel());
            colors.add(Palette.TEXT_WHITE);
            lines.add("HP " + (int) u.getMaxHp() + "  DMG " + (int) u.getDamage() + "  "
                    + (u.isRanged() ? "RANGE " + (int) u.getRange() : "MELEE") + "  COST " + ub.getType().cost());
            colors.add(Palette.TEXT_WHITE);
            lines.add("ABILITY: " + u.getAbilityName());
            colors.add(Palette.TEXT_GOLD);
            lines.add(u.getAbilityText());
            colors.add(Palette.TEXT_GOLD);
        } else if (target == upgradeButton) {
            Tower me = battle.getTower(mySide);
            lines.add("TOWER UPGRADE");
            colors.add(Palette.TEXT_GOLD);
            lines.add(Tower.UPGRADE_TEXT);
            colors.add(Palette.TEXT_WHITE);
            lines.add(me.getUpgradeCost() < 0 ? "ALREADY MAX LEVEL" : "COST: " + me.getUpgradeCost() + " ENERGY");
            colors.add(Palette.TEXT_WHITE);
        } else if (target == ultimateButton) {
            UltimateSkill skill = battle.getTower(mySide).getUltimate();
            lines.add(skill.getName().toUpperCase());
            colors.add(Palette.TEXT_GOLD);
            lines.add(skill.getDescription());
            colors.add(Palette.TEXT_WHITE);
            lines.add("FILLS OVER TIME AND WHEN YOU DEAL DAMAGE");
            colors.add(Palette.TEXT_WHITE);
        } else {
            return;
        }

        int widest = 0;
        for (String line : lines) {
            widest = Math.max(widest, PixelFont.width(line, 1));
        }
        int bw = widest + 14;
        int bh = lines.size() * 10 + 6;
        int bx = Math.max(2, Math.min(W - bw - 2, target.getX() + target.getWidth() / 2 - bw / 2));
        int by = PANEL_Y - bh - 2; // above the panel, so the energy bar stays visible
        g.setColor(new Color(20, 12, 8, 225));
        g.fillRect(bx, by, bw, bh);
        g.setColor(Palette.WOOD_LIGHT);
        g.fillRect(bx, by, bw, 1);
        g.fillRect(bx, by + bh - 1, bw, 1);
        g.fillRect(bx, by, 1, bh);
        g.fillRect(bx + bw - 1, by, 1, bh);
        for (int i = 0; i < lines.size(); i++) {
            PixelFont.drawOutlined(g, lines.get(i), bx + 7, by + 4 + i * 10, 1, colors.get(i), Palette.TEXT_DARK);
        }
    }

    private static String shortName(String name) {
        return name.length() > 10 ? name.substring(0, 10) : name;
    }

    /** The VS screen before the fight. */
    private void drawIntro(Graphics2D g) {
        g.setColor(new Color(10, 6, 20, 150));
        g.fillRect(0, 0, W, PANEL_Y);
        int bx = 26;
        int by = 26;
        int bw = W - 52;
        int bh = 92;
        PixelArt.board(g, bx, by, bw, bh);

        String header = lan != null ? "LAN 1V1 BATTLE" : stage.endless() ? "ENDLESS SIEGE"
                : "STAGE " + stage.number() + " - " + stage.name().toUpperCase();
        PixelFont.drawCentered(g, header, W / 2, by + 7, 1, Palette.TEXT_DARK, null);

        Department me = battle.getTower(mySide).getDepartment();
        Department foe = battle.getTower(foeSide()).getDepartment();
        PixelFont.drawCentered(g, me.getShortName(), W / 2 - 62, by + 24, 1,
                Palette.lighter(me.getColor(), 0.15f), Palette.TEXT_DARK);
        PixelFont.drawCentered(g, "VS", W / 2, by + 20, 2, Palette.TEXT_GOLD, Palette.TEXT_DARK);
        PixelFont.drawCentered(g, foe.getShortName(), W / 2 + 62, by + 24, 1,
                Palette.lighter(foe.getColor(), 0.15f), Palette.TEXT_DARK);
        PixelFont.drawCentered(g, "YOU", W / 2 - 62, by + 34, 1, Palette.TEXT_DARK, null);
        PixelFont.drawCentered(g, lan != null ? lan.getOpponent().name() : "ENEMY", W / 2 + 62, by + 34, 1,
                Palette.TEXT_DARK, null);

        if (lan != null) {
            PixelFont.drawCentered(g, "DESTROY THE OTHER PLAYER'S TOWER!", W / 2, by + 45, 1,
                    Palette.TEXT_GOLD, Palette.TEXT_DARK);
            PixelFont.drawCentered(g, mySide == Side.PLAYER ? "YOUR TOWER IS ON THE LEFT" : "YOUR TOWER IS ON THE RIGHT",
                    W / 2, by + 55, 1, Palette.TEXT_DARK, null);
        } else if (stage.endless()) {
            PixelFont.drawCentered(g, "SURVIVE AS LONG AS YOU CAN!", W / 2, by + 45, 1, Palette.TEXT_GOLD, Palette.TEXT_DARK);
            PixelFont.drawCentered(g, "THE ENEMY GETS STRONGER EVERY 30 SECONDS", W / 2, by + 55, 1, Palette.TEXT_DARK, null);
        } else {
            PixelFont.drawCentered(g, "RULE: " + stage.modifier().getTitle(), W / 2, by + 45, 1,
                    Palette.TEXT_GOLD, Palette.TEXT_DARK);
            PixelFont.drawCentered(g, stage.modifier().getDescription(), W / 2, by + 55, 1, Palette.TEXT_DARK, null);
        }
        // Scouting report: which units the enemy brought
        List<String> foeUnits = new ArrayList<>();
        for (UnitType type : battle.getTower(foeSide()).getUnitTypes()) {
            foeUnits.add(type.name().toUpperCase());
        }
        PixelFont.drawCentered(g, "ENEMY UNITS: " + String.join(", ", foeUnits), W / 2, by + 66, 1,
                Palette.lighter(foe.getColor(), 0.15f), Palette.TEXT_DARK);
        if (lan != null) {
            int left = (int) Math.ceil(LAN_COUNTDOWN - introTime);
            PixelFont.drawCentered(g, "STARTING IN " + Math.max(1, left) + "...", W / 2, by + 79, 1,
                    Palette.TEXT_WHITE, Palette.TEXT_DARK);
        } else if ((tick / 15) % 2 == 0) {
            PixelFont.drawCentered(g, "CLICK OR PRESS ENTER TO START", W / 2, by + 79, 1,
                    Palette.TEXT_WHITE, Palette.TEXT_DARK);
        }
    }
}
