package net;

import java.util.ArrayList;
import java.util.List;
import model.Department;
import model.Tower;
import model.UnitType;

/**
 * What one player brings to a LAN 1v1 match: name, department, permanent tower level and lineup.
 * It is sent to the other computer as one line of text, for example:
 *   juan_dc EDUCATION 2 Student,Teacher,Principal
 */
public record LanPlayer(String name, Department department, int towerLevel, List<UnitType> lineup) {

    public String encode() {
        List<String> names = new ArrayList<>();
        for (UnitType t : lineup) {
            names.add(t.name());
        }
        return name + " " + department.name() + " " + towerLevel + " " + String.join(",", names);
    }

    /** Reads the text made by encode(). Returns null if it is broken. */
    public static LanPlayer decode(String text) {
        try {
            String[] parts = text.trim().split(" ", 4);
            Department dept = Department.valueOf(parts[1]);
            int level = Integer.parseInt(parts[2]);
            List<UnitType> lineup = new ArrayList<>();
            for (String unit : parts[3].split(",")) {
                UnitType type = dept.findUnit(unit);
                if (type == null) {
                    return null;
                }
                lineup.add(type);
            }
            if (lineup.size() != Tower.LINEUP_SIZE) {
                return null;
            }
            return new LanPlayer(parts[0], dept, level, lineup);
        } catch (RuntimeException e) {
            return null;
        }
    }
}
