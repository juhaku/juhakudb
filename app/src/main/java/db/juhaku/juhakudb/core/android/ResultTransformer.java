package db.juhaku.juhakudb.core.android;

import java.util.List;

/**
 * Created by juha on 26/04/16.
 * <p>Implement this interface to provide custom conversion for list of result set objects returned
 * by database query.</p>
 * @author juha
 *
 * @since 1.0.2
 */
public interface ResultTransformer<T> {

    /**
     * Converts list of {@link ResultSet} objects returned by database query.
     *
     * @param resultSets {@link ResultSet} returned by database query.
     * @return instance of object that is converted from result sets.
     *
     * @since 1.0.2
     */
    T transformResult(List<ResultSet> resultSets);
}
