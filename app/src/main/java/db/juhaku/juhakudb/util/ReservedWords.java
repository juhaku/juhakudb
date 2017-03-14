package db.juhaku.juhakudb.util;

/**
 * Created by juha on 14/03/17.
 * <p>Utility enum for mapping reserved words for SQL queries and ORM mapping.</p>
 *
 * @author juha
 * @since 1.1.2
 */
public enum ReservedWords {

    LEFT, JOIN, RIGHT, INNER, FULL, ON, FROM, SELECT, WHERE, AND, OR, IN, NOT, LIKE,
    IS, NULL, BETWEEN, ASC, DESC, LIMIT, ORDER, BY, OFFSET;

    /**
     * Check if given word belongs to reserved words. Word match is searched case insensitive.
     * @param word String value of word to look for.
     * @return true if reserved words has given word; false otherwise.
     * @since 1.1.2
     */
    public static boolean has(String word) {
        for (ReservedWords reservedWord : ReservedWords.values()) {
            if (reservedWord.toString().equalsIgnoreCase(word)) {
                return true;
            }
        }

        return false;
    }
}
