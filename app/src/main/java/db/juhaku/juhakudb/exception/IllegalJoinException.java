package db.juhaku.juhakudb.exception;

/**
 * Created by juha on 17/03/17.
 * <p>This exception will be thrown when joint is formed incorrectly.</p>
 *
 * @author juha
 *
 * @since 1.1.3-SNAPSHOT
 */
public class IllegalJoinException extends RuntimeException {

    public IllegalJoinException(String message) {
        super(message);
    }
}
