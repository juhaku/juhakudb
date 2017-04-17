package db.juhaku.juhakudb.core;

/**
 * Created by juha on 11/05/16.
 *
 *<p>Define cascading type for database actions such like store and delete.</p>
 *
 * <p>Currently Cascade definition is not required at all. All the operations are cascading.</p>
 *
 * @author juha
 *
 * @since 1.0.2
 */
@Deprecated
public enum Cascade {
    /**
     * If cascade store is used over reference then referenced entity is also stored.
     */
    STORE,
    /**
     * If cascade delete is used over reference then referenced entity is also deleted.
     */
    DELETE,
    /**
     * If cascade all is used over reference then both store and delete actions are performed
     * to referenced entities as well.
     */
    ALL
}
