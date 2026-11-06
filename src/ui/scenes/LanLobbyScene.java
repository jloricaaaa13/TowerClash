package ui.scenes;

import java.awt.Color;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.List;
import model.Department;
import model.PlayerProgress;
import model.UnitType;
import net.LanPlayer;
import net.LanSetup;
import ui.GameWindow;
import ui.pixel.Landscape;
import ui.pixel.Palette;
import ui.pixel.PixelArt;
import ui.pixel.PixelButton;
import ui.pixel.PixelFont;
import ui.pixel.PixelTextField;

/**
 * LAN 1v1 lobby: play against a classmate on another computer in the same Wi-Fi/LAN.
 *
 *   1. Both players pick their department (and lineup).
 *   2. One player presses HOST and tells the other their IP address (shown on screen).
 *   3. The other player types that IP and presses JOIN.
 *   4. The battle starts on both computers at the same time.
 */
public class LanLobbyScene extends PixelScene {

    private static final int LEFT_X = 4;
    private static final int LEFT_W = 150;
    private static final int RIGHT_X = 158;
    private static final int RIGHT_W = 158;
    private static final int TOP = 24;
    private static final int BOX_H = 126;

    private final Landscape landscape = new Landscape(170, new int[]{}, new int[]{});
    private final LanSetup setup = new LanSetup();
    private final List<PixelButton> departmentButtons = new ArrayList<>();
    private final PixelTextField ipField;
    private final PixelButton hostButton;
    private final PixelButton joinButton;
    private final PixelButton cancelButton;
    private Department department = Department.HOSPITALITY;

    public LanLobbyScene(GameWindow window) {
        super(window);
        Department[] all = Department.values();
        for (int i = 0; i < all.length; i++) {
            Department d = all[i];
            PixelButton b = addWidget(new PixelButton(d.getFullName().toUpperCase(), LEFT_X + 8, TOP + 14 + i * 15,
                    LEFT_W - 16, 13, () -> pick(d)));
            departmentButtons.add(b);
        }
        addWidget(new PixelButton("CHANGE LINEUP", LEFT_X + (LEFT_W - 92) / 2, TOP + BOX_H - 20, 92, 14,
                () -> {
                    setup.cancel();
                    window.showLineupForLan(department);
                }));

        hostButton = addWidget(new PixelButton("HOST GAME", RIGHT_X + (RIGHT_W - 84) / 2, TOP + 15, 84, 14, this::host));
        ipField = addWidget(new PixelTextField(RIGHT_X + 10, TOP + 78, RIGHT_W - 20, 40, false)
                .withHint("HOST IP, E.G. 192.168.1.5"));
        ipField.setOnSubmit(this::join);
        joinButton = addWidget(new PixelButton("JOIN GAME", RIGHT_X + (RIGHT_W - 84) / 2, TOP + 96, 84, 14, this::join));

        addWidget(new PixelButton("BACK", 6, 160, 60, 15, this::back));
        cancelButton = addWidget(new PixelButton("CANCEL", W - 66, 160, 60, 15, setup::cancel));
    }

    @Override
    public void onShow() {
        Department chosen = window.getSelectedDepartment();
        if (chosen != null) {
            department = chosen;
        }
        pick(department);
        super.onShow();
    }

    @Override
    public void onHide() {
        if (setup.getState() != LanSetup.State.READY) {
            setup.cancel(); // leaving the lobby stops hosting / joining
        }
        super.onHide();
    }

    private void pick(Department d) {
        department = d;
        window.setSelectedDepartment(d);
        Department[] all = Department.values();
        for (int i = 0; i < all.length; i++) {
            departmentButtons.get(i).setTextColor(all[i] == d ? Palette.TEXT_WHITE : Palette.TEXT_GOLD);
        }
    }

    /** What this player brings: name, department, tower level, lineup. */
    private LanPlayer me() {
        PlayerProgress p = window.getProgress();
        return new LanPlayer(window.getAccount().username(), department, p.getTowerLevel(department),
                p.getLineup(department));
    }

    private void host() {
        setup.host(me());
    }

    private void join() {
        String ip = ipField.getText().trim();
        if (ip.isEmpty()) {
            showToast("TYPE THE HOST'S IP ADDRESS FIRST");
            setTypingField(ipField);
            return;
        }
        setup.join(ip, me());
    }

    private void back() {
        setup.cancel();
        window.showMainMenu();
    }

    @Override
    protected void onEscape() {
        back();
    }

    @Override
    protected void update() {
        LanSetup.State state = setup.getState();
        boolean busy = state == LanSetup.State.HOSTING || state == LanSetup.State.CONNECTING;
        cancelButton.setVisible(busy);
        hostButton.setEnabled(!busy);
        joinButton.setEnabled(!busy);
        if (state == LanSetup.State.READY) {
            window.startLanBattle(setup.takeMatch()); // both computers start now
        }
    }

    // ------------------------------------------------------------------
    //  Drawing
    // ------------------------------------------------------------------

    @Override
    protected void paintScene(Graphics2D g) {
        landscape.draw(g, tick);
        PixelFont.drawCentered(g, "LAN 1V1 BATTLE", W / 2, 5, 2, Palette.TEXT_WHITE, Palette.TEXT_DARK);
        if (window.getAccount() == null) {
            return;
        }
        drawTeam(g);
        drawConnect(g);
        drawStatus(g);
    }

    private void drawTeam(Graphics2D g) {
        PixelArt.board(g, LEFT_X, TOP, LEFT_W, BOX_H);
        PixelFont.drawCentered(g, "YOUR TEAM", LEFT_X + LEFT_W / 2, TOP + 4, 1, Palette.TEXT_GOLD, Palette.TEXT_DARK);
        PlayerProgress p = window.getProgress();
        int y = TOP + 59;
        // small tower in the department color
        PixelArt.tower(g, LEFT_X + 12, y + 38, 14, 30, department.getColor(), tick);
        int textX = LEFT_X + 34;
        PixelFont.draw(g, department.getTowerName().toUpperCase(), textX, y, 1,
                Palette.darker(department.getColor(), 0.45f));
        PixelFont.draw(g, "LEVEL " + p.getTowerLevel(department) + "  -  LINEUP:", textX, y + 9, 1, Palette.TEXT_DARK);
        int i = 0;
        for (UnitType t : p.getLineup(department)) {
            PixelFont.draw(g, (i + 1) + ". " + t.name().toUpperCase(), textX + 4, y + 19 + i * 8, 1, Palette.WOOD_OUTLINE);
            i++;
        }
    }

    private void drawConnect(Graphics2D g) {
        PixelArt.board(g, RIGHT_X, TOP, RIGHT_W, BOX_H);
        int cx = RIGHT_X + RIGHT_W / 2;
        PixelFont.drawCentered(g, "PLAYER 1: HOST", cx, TOP + 4, 1, Palette.TEXT_GOLD, Palette.TEXT_DARK);
        List<String> ips = LanSetup.localAddresses();
        String ipText = ips.isEmpty() ? "NO NETWORK FOUND" : "YOUR IP: " + ips.get(0);
        PixelFont.drawCentered(g, ipText, cx, TOP + 34, 1, Palette.TEXT_DARK, null);
        if (ips.size() > 1) {
            PixelFont.drawCentered(g, "(OR " + ips.get(1) + ")", cx, TOP + 43, 1, Palette.WOOD_OUTLINE, null);
        }
        g.setColor(Palette.WOOD_DARK);
        g.fillRect(RIGHT_X + 8, TOP + 55, RIGHT_W - 16, 1);
        PixelFont.drawCentered(g, "PLAYER 2: JOIN", cx, TOP + 61, 1, Palette.TEXT_GOLD, Palette.TEXT_DARK);
        PixelFont.draw(g, "TYPE THE HOST'S IP:", RIGHT_X + 10, TOP + 70, 1, Palette.TEXT_DARK);
    }

    private void drawStatus(Graphics2D g) {
        LanSetup.State state = setup.getState();
        String text;
        Color color = Palette.TEXT_WHITE;
        switch (state) {
            case HOSTING -> {
                List<String> ips = LanSetup.localAddresses();
                text = "WAITING FOR A PLAYER" + ".".repeat(1 + (tick / 15) % 3)
                        + (ips.isEmpty() ? "" : "  (YOUR IP: " + ips.get(0) + ")");
                color = Palette.TEXT_GOLD;
            }
            case CONNECTING -> {
                text = "CONNECTING" + ".".repeat(1 + (tick / 15) % 3);
                color = Palette.TEXT_GOLD;
            }
            case FAILED -> {
                text = setup.getProblem();
                color = Palette.TEXT_OFF;
            }
            default -> text = "BOTH PLAYERS MUST BE ON THE SAME WI-FI / LAN";
        }
        PixelFont.drawCentered(g, text, W / 2, 152, 1, color, Palette.TEXT_DARK);
    }
}
