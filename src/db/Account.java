package db;

/** A logged-in player. The id comes from the database (or the save file). */
public record Account(int id, String username, String email) {
}
