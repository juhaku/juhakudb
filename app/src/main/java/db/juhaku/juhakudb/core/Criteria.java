package db.juhaku.juhakudb.core;

/**
 * Created by juha on 16/04/16.
 *<p>Super interface for filtering items. Items that meet the criteria should be processed differently
 * to items that does not meet.</p>
 * @author juha
 *
 * @since 1.0.2
 */
public interface Criteria<T> {

    /**
     * This method should provide implementation to determine whether required criteria is met.
     *
     * @param type type of object to resolve is criteria met. e.g. String or Integer if there is
     *             certain criteria for them to meet.
     * @return boolean value; true if criteria is met; false otherwise.
     *
     * @since 1.0.2
     */
    boolean meetCriteria(T type);
}
