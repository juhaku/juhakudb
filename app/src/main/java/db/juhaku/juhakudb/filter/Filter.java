package db.juhaku.juhakudb.filter;

/**
 * Created by juha on 17/04/16.
 *
 * @author juha
 */
public interface Filter<T> {

    void filter(Root<T> root, Predicates predicates);
}
