package db.juhaku.juhakudb.exception;

/**
 * Created by juha on 16/12/15.
 *<p>This exception is thrown when name cannot be resolved via
 * {@link db.juhaku.juhakudb.core.NameResolver} for any reason.</p>
 *
 * @author Juha Kukkonen
 *
 * @since 1.0.2
 */
public class NameResolveException extends Exception {

    public NameResolveException(String detailMessage) {
        super(detailMessage);
    }
}
