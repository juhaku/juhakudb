package db.juhaku.juhakudb.exception;

/**
 * Created by juha on 14/04/16.
 *<p>Conversion exception is thrown when any failure occurs during entity conversion.</p>
 * See {@link db.juhaku.juhakudb.core.android.EntityConverter} for more details.
 * @author juha
 *
 * @since 1.0.2
 */
public class ConversionException extends RuntimeException {

    public ConversionException(String detailMessage) {
        super(detailMessage);
    }

    public ConversionException(String detailMessage, Throwable cause) {
        super(detailMessage, cause);
    }
}
