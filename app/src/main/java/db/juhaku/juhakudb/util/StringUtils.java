package db.juhaku.juhakudb.util;

/**
 * Created by juha on 23/12/15.
 *<p>Utility class for String.</p>
 * @author juha
 *
 * @since 1.0.2
 */
public class StringUtils {

    /**
     * Checks is provided String null or otherwise empty by containing only white space.
     * @param arg0 String value to check.
     * @return Returns true if provided String is blank otherwise false.
     */
    public static final boolean isBlank(String arg0) {
        return !(arg0 != null && arg0.trim().length() > 0);
    }

    /**
     * Converts array of strings to String for easy logging.
     *
     * @param array String array of values to convert to loggable String.
     * @return Stringified array.
     *
     * @since 1.3.2-SNAPSHOT
     */
    public static final String arrayToString(String... array) {
        StringBuilder builder = new StringBuilder();
        for (String str : array) {
            builder.append(str);
            if (str != array[array.length - 1]) {
                builder.append(",");
            }
        }

        return builder.toString();
    }
}
