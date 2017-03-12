package db.juhaku.juhakudb.exception;

/**
 * Created by juha on 27/04/16.
 *<p>This exception is thrown if there is an error in building {@link db.juhaku.juhakudb.filter.Query}.
 * Currently occurs if association resolving fails somehow.</p>
 * @author juha
 *
 * @since 1.0.2
 */
public class QueryBuildException extends RuntimeException {

    public QueryBuildException(String detailMessage) {
        super(detailMessage);
    }

    public QueryBuildException(String detailMessage, Throwable throwable) {
        super(detailMessage, throwable);
    }
}
