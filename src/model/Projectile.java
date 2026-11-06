package model;

import game.Battle;
import java.awt.Color;
import java.awt.Graphics2D;
import java.util.function.Consumer;

/**
 * Something flying toward a target: hot plates, books, code bolts, chalk...
 * It follows the target (homing), and when it arrives it deals damage and
 * runs its onHit effect (burn, slow, chain lightning, etc.).
 */
public class Projectile {

    public enum Kind { PLATE, BOOK, BOLT, CHALK, CODE, COFFEE, VIRUS }

    private final Kind kind;
    private final Side side;
    private final Combatant target;
    private final double speed;
    private final double damage;
    private final double arcHeight;
    private final Consumer<Combatant> onHit;
    private double x;
    private double y;
    private double traveled;
    private double remaining = 1;
    private double counterMultiplier = 1;
    private boolean done;

    /**
     * @param arcHeight how high it curves (0 = straight line)
     * @param onHit     extra effect when it hits (can be null)
     */
    public Projectile(Kind kind, Side side, double x, double y, Combatant target,
                      double speed, double damage, double arcHeight, Consumer<Combatant> onHit) {
        this.kind = kind;
        this.side = side;
        this.x = x;
        this.y = y;
        this.target = target;
        this.speed = speed;
        this.damage = damage;
        this.arcHeight = arcHeight;
        this.onHit = onHit;
    }

    /** Remembers the role counter bonus (only used to color the damage number). */
    public Projectile withCounterMultiplier(double multiplier) {
        this.counterMultiplier = multiplier;
        return this;
    }

    public void update(Battle battle, double dt) {
        if (done) {
            return;
        }
        if (!target.isAlive()) {
            done = true; // target already died: the projectile disappears
            return;
        }
        double tx = battle.hitX(target);
        double ty = battle.hitY(target);
        double dx = tx - x;
        double dy = ty - y;
        double dist = Math.sqrt(dx * dx + dy * dy); // exact on every computer (needed for LAN 1v1)
        double step = speed * dt;
        remaining = dist;
        if (dist <= step + 1) {
            battle.hit(target, damage, side, counterMultiplier);
            if (onHit != null) {
                onHit.accept(target);
            }
            done = true;
        } else {
            x += dx / dist * step;
            y += dy / dist * step;
            traveled += step;
        }
    }

    public boolean isDone() {
        return done;
    }

    public void draw(Graphics2D g, int tick) {
        double progress = traveled / Math.max(1, traveled + remaining);
        int px = (int) Math.round(x);
        int py = (int) Math.round(y - Math.sin(progress * Math.PI) * arcHeight);
        int dir = side.direction();
        switch (kind) {
            case PLATE -> {
                g.setColor(new Color(0xFF7A1A));
                g.fillRect(px - dir * 3, py - 1, 2, 1);
                g.setColor(new Color(0xFFD23F));
                g.fillRect(px - dir * 2, py - 2, 1, 1);
                g.setColor(new Color(0xF4F4F4));
                g.fillRect(px - 2, py, 5, 1);
                g.setColor(new Color(0xB8C0CC));
                g.fillRect(px - 1, py + 1, 3, 1);
            }
            case BOOK -> {
                boolean open = (tick / 3) % 2 == 0;
                g.setColor(new Color(0x1E3A8A));
                if (open) {
                    g.fillRect(px - 2, py - 1, 5, 3);
                    g.setColor(Color.WHITE);
                    g.fillRect(px - 1, py, 3, 1);
                } else {
                    g.fillRect(px - 1, py - 2, 3, 5);
                    g.setColor(Color.WHITE);
                    g.fillRect(px + 1, py - 1, 1, 3);
                }
            }
            case BOLT -> {
                g.setColor(new Color(0xFFF59D));
                g.fillRect(px - dir * 4, py, 5, 1);
                g.setColor(new Color(0xFACC15));
                g.fillRect(px - dir * 6, py, 3, 1);
                g.setColor(Color.WHITE);
                g.fillRect(px, py, 1, 1);
            }
            case CHALK -> {
                g.setColor(Color.WHITE);
                g.fillRect(px - 1, py, 3, 1);
            }
            case CODE -> {
                g.setColor(new Color(0x4ADE80));
                g.fillRect(px - 1, py - 1, 1, 3);
                g.fillRect(px + 1, py - 1, 2, 1);
                g.fillRect(px + 1, py + 1, 2, 1);
            }
            case COFFEE -> {
                g.setColor(Color.WHITE);
                g.fillRect(px - 1, py - 1, 3, 3);       // paper cup
                g.setColor(new Color(0x6B3E1E));
                g.fillRect(px - 1, py - 1, 3, 1);       // coffee on top
                g.setColor(new Color(0xD6D3D1));
                g.fillRect(px, py - 3 + (tick / 4) % 2, 1, 1); // steam
            }
            case VIRUS -> {
                g.setColor(new Color(0xEF4444));
                g.fillRect(px - 1, py - 1, 3, 3);
                g.setColor(new Color(0xFCA5A5));
                g.fillRect(px - 2, py, 1, 1);
                g.fillRect(px + 2, py, 1, 1);
                g.fillRect(px, py - 2, 1, 1);
                g.fillRect(px, py + 2, 1, 1);
            }
        }
    }
}
