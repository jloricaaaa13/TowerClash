package db;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import model.PlayerProgress;

/**
 * Before accounts existed, the game saved ONE progress file on this computer
 * (save.properties). The first account created on this computer receives that
 * old progress, so nothing is lost. The old file is then renamed so it is used only once.
 */
public final class LegacySave {

    private static final Path FILE = Paths.get(System.getProperty("user.home"), ".department-tower-clash",
            "save.properties");

    private LegacySave() {
    }

    /** The old progress, or null if there is none (or it was already given to an account). */
    public static PlayerProgress take() {
        if (!Files.exists(FILE)) {
            return null;
        }
        try {
            PlayerProgress progress = ProgressCodec.fromText(Files.readString(FILE));
            Files.move(FILE, FILE.resolveSibling("save.properties.imported"));
            return progress;
        } catch (IOException e) {
            return null;
        }
    }
}
