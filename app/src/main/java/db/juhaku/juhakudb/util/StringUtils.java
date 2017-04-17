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
}
