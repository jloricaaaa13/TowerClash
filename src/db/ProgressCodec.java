package db;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import model.Department;
import model.PlayerProgress;
import model.Stage;
import model.UnitType;

/**
 * Turns a player's whole progress into simple text lines (and back), for example:
 *   coins=250
 *   stars.1=3
 *   towerLevel.EDUCATION=2
 *   lineup.EDUCATION=Student,Librarian,Principal
 *
 * Both save methods use this: the MySQL store keeps the text in the save_data column,
 * and the local store keeps it in a file.
 */
public final class ProgressCodec {

    private ProgressCodec() {
    }

    public static String toText(PlayerProgress progress) {
        Properties props = new Properties();
        props.setProperty("coins", String.valueOf(progress.getCoins()));
        props.setProperty("unlockedStage", String.valueOf(progress.getUnlockedStage()));
        props.setProperty("matchesPlayed", String.valueOf(progress.getMatchesPlayed()));
        props.setProperty("wins", String.valueOf(progress.getWins()));
        props.setProperty("losses", String.valueOf(progress.getLosses()));
        props.setProperty("highScore", String.valueOf(progress.getHighScore()));
        props.setProperty("bestWinSeconds", String.valueOf(progress.getBestWinSeconds()));
        props.setProperty("bestEndlessSeconds", String.valueOf(progress.getBestEndlessSeconds()));
        props.setProperty("bestEndlessScore", String.valueOf(progress.getBestEndlessScore()));
        props.setProperty("lanWins", String.valueOf(progress.getLanWins()));
        props.setProperty("lanLosses", String.valueOf(progress.getLanLosses()));
        for (Department d : Department.values()) {
            props.setProperty("towerLevel." + d.name(), String.valueOf(progress.getTowerLevel(d)));
            List<String> names = new ArrayList<>();
            for (UnitType type : progress.getLineup(d)) {
                names.add(type.name());
            }
            props.setProperty("lineup." + d.name(), String.join(",", names));
        }
        for (Stage s : Stage.ALL) {
            props.setProperty("stars." + s.number(), String.valueOf(progress.getStars(s.number())));
        }
        StringWriter out = new StringWriter();
        try {
            props.store(out, null);
        } catch (IOException e) {
            throw new IllegalStateException(e); // cannot happen when writing to a String
        }
        return out.toString();
    }

    /** Reads progress back. Missing or broken values just use the starting values. */
    public static PlayerProgress fromText(String text) {
        PlayerProgress progress = new PlayerProgress();
        if (text == null || text.isBlank()) {
            return progress;
        }
        Properties props = new Properties();
        try {
            props.load(new StringReader(text));
        } catch (IOException e) {
            return progress;
        }
        return fromProperties(props);
    }

    public static PlayerProgress fromProperties(Properties props) {
        PlayerProgress progress = new PlayerProgress();
        progress.setCoins(read(props, "coins", 0));
        progress.setUnlockedStage(read(props, "unlockedStage", 1));
        progress.setMatchesPlayed(read(props, "matchesPlayed", 0));
        progress.setWins(read(props, "wins", 0));
        progress.setLosses(read(props, "losses", 0));
        progress.setHighScore(read(props, "highScore", 0));
        progress.setBestWinSeconds(read(props, "bestWinSeconds", 0));
        progress.setBestEndlessSeconds(read(props, "bestEndlessSeconds", 0));
        progress.setBestEndlessScore(read(props, "bestEndlessScore", 0));
        progress.setLanWins(read(props, "lanWins", 0));
        progress.setLanLosses(read(props, "lanLosses", 0));
        for (Department d : Department.values()) {
            progress.setTowerLevel(d, read(props, "towerLevel." + d.name(), 1));
            String text = props.getProperty("lineup." + d.name());
            if (text != null) {
                List<UnitType> lineup = new ArrayList<>();
                for (String name : text.split(",")) {
                    lineup.add(d.findUnit(name));
                }
                progress.setLineup(d, lineup); // ignored if the text is broken
            }
        }
        for (Stage s : Stage.ALL) {
            progress.setStars(s.number(), read(props, "stars." + s.number(), 0));
        }
        return progress;
    }

    private static int read(Properties props, String key, int fallback) {
        try {
            return Integer.parseInt(props.getProperty(key, String.valueOf(fallback)).trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}
