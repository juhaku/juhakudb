package db.juhaku.juhakudb.core;

import db.juhaku.juhakudb.core.android.DatabaseManager;

/**
 * Created by juha on 04/12/15.
 *<p>This interface should be implemented and passed to {@link DatabaseManager} what handles
 * configuration of the database for user. This interface provides simple and easy way to
 * configure database.</p>
 *
 * @author Juha Kukkonen
 *
 * @since 1.0.2
 */
public interface DatabaseConfigurationAdapter {

    /**
     * Implement this method to provide configuration for database and schema.
     * @param configuration instance of {@link DatabaseConfiguration}
     *
     * @since 1.0.2
     */
    void configure(DatabaseConfiguration configuration);
}
