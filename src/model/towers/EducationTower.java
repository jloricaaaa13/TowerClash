package model.towers;

import game.Battle;
import java.awt.Color;
import java.awt.Graphics2D;
import java.util.List;
import model.Department;
import model.Projectile;
import model.Side;
import model.Tower;
import model.Unit;
import model.UnitType;
import model.ultimates.FinalExam;
import model.units.Coach;
import model.units.Librarian;
import model.units.Principal;
import model.units.Student;
import model.units.Teacher;

/**
 * BLUE - Academy Tower.
 * The longest range. Shoots books that SLOW enemies by 40%.
 */
public class EducationTower extends Tower {

    /** All 5 Education units. The first 3 are the starting lineup. */
    public static final List<UnitType> ROSTER = List.of(
            new UnitType("Student", 15, 1.5, Student::new),
            new UnitType("Teacher", 50, 4.5, Teacher::new),
            new UnitType("Principal", 75, 8.0, Principal::new),
            new UnitType("Librarian", 55, 5.0, Librarian::new),
            new UnitType("Coach", 40, 6.0, Coach::new));

    public EducationTower(Side side, int permanentLevel, double hpMultiplier, List<UnitType> lineup) {
        //                                            hp    dmg  interval range regen/s
        super(Department.EDUCATION, side, permanentLevel, hpMultiplier, 610, 12, 1.2, 115, 0,
                new FinalExam(), lineup);
    }

    @Override
    protected void attack(Battle battle, Unit target) {
        battle.addProjectile(new Projectile(Projectile.Kind.BOOK, side,
                centerX(), Battle.GROUND_Y - launchHeight(), target, 140, damage, 4,
                hit -> hit.applySlow(0.4, 2.0)));
    }

    @Override
    protected void drawEmblem(Graphics2D g, int left, int top, int tick) {
        // School clock near the top
        g.setColor(new Color(0x1E3A8A));
        g.fillRect(left + 8, top + 3, 8, 8);
        g.setColor(Color.WHITE);
        g.fillRect(left + 9, top + 4, 6, 6);
        g.setColor(new Color(0x1E140C));
        g.fillRect(left + 12, top + 5, 1, 3);   // minute hand
        g.fillRect(left + 12, top + 7, 2, 1);   // hour hand
    }
}
