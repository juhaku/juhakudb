package db.juhaku.juhakudb.exception;

/**
 * Created by juha on 16/04/16.
 *<p>This exception is thrown if entity manager fails to resolve entities and their columns.</p>
 * @author juha
 *
 * @since 1.0.2
 */
public class MappingException extends RuntimeException {

    public MappingException(String detailMessage) {
        super(detailMessage);
    }

    public MappingException(String detailMessage, Throwable throwable) {
        super(detailMessage, throwable);
    }
}
