package db.juhaku.juhakudb.core.schema;

/**
 * Created by juha on 16/12/15.
 *<p>Creation mode for database schema.</p>
 *
 * @author juha
 *
 * @since 1.0.2
 */
public enum SchemaCreationMode {

    /**
     * Always creates new database. Useful when database works as online cache.
     */
    CREATE,

    /**
     * Updates already found database schema. Will not delete tables and data.
     */
    UPDATE
}
