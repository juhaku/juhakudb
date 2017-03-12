package db.juhaku.juhakudb.exception;

import db.juhaku.juhakudb.core.schema.SchemaFactory;

/**
 * Created by juha on 16/12/15.
 *<p>This exception is thrown when database schema cannot be initialized in
 * {@link SchemaFactory}.</p>
 *
 * @author Juha Kukkonen
 *
 * @since 1.0.2
 */
public class SchemaInitializationException extends Exception {

    public SchemaInitializationException(String detailMessage) {
        super(detailMessage);
    }

    public SchemaInitializationException(String detailMessage, Throwable cause) {
        super(detailMessage, cause);
    }
}
