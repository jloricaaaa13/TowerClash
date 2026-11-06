package game.effects;

import java.awt.Graphics2D;

/** A short visual effect (smoke, laser, flash...). It does not change the game, it only looks cool. */
public abstract class Effect {

    protected final double life;
    protected double age;

    protected Effect(double life) {
        this.life = life;
    }

    public void update(double dt) {
        age += dt;
    }

    public boolean isDone() {
        return age >= life;
    }

    /** 0 at the start, 1 at the end. */
    protected double progress() {
        return Math.min(1, age / life);
    }

    public abstract void draw(Graphics2D g, int tick);
}
