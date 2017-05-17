/**
MIT License

Copyright (c) 2017 juhaku

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
*/
package db.juhaku.juhakudb.repository;

import java.util.Collection;
import java.util.List;

import db.juhaku.juhakudb.core.android.ResultTransformer;
import db.juhaku.juhakudb.filter.Filter;
import db.juhaku.juhakudb.filter.Query;

/**
 * Created by juha on 18/04/17.
 *
 * <p>General interface for SQLite database repositories. All the repositories should extend or
 * implement this interface if custom repository is being implemented.</p>
 *
 * <p>This interface provides simple SPI for most needed database operations.</p>
 *
 * <p>Currently only creating a interface with {@code @Repository} annotation like below is enough to
 * create instance of repository.</p><br/>
 *
 * <code>@Repository<br/>
 * public interface PersonRepository extends SimpleRepository<Long, Permission> {}</code><br/><br/>
 *
 * <p>To get an instance of the repository in Activity or Fragment or other class you can either use
 * {@code @Inject} annotation or call {@code databaseManager.getRepository(PersonRepository.class)}
 * method.</p>
 *
 * @author Juha Kukkonen
 *
 * @see db.juhaku.juhakudb.annotation.Repository
 * @see db.juhaku.juhakudb.annotation.Inject
 * @see db.juhaku.juhakudb.core.android.DatabaseManager
 *
 * @since 1.3.0
 */
public interface SimpleRepository<K, T> {

    /**
     * Stores given entity and returns it with populated database id if successful and entity is new.
     * Given entity must be annotated with {@link javax.persistence.Entity} annotation.
     *
     * <p>Store operation uses replace on conflict algorithm when storing items. This means when
     * primary key or unique constraint or other constraint has conflict the row will be replaced with
     * provided data.</p>
     *
     * <p>Store operation is cascading.</p>
     *
     * @param object Object that must be database entity.
     * @return Stored instance of given object.
     *
     * @since 1.3.0
     */
    T store(T object);

    /**
     * Stores collection of given entities and returns them as a list with populated database id if
     * entities are new. Entities provided must have {@link javax.persistence.Entity}
     * annotation.
     *
     * <p>Store operation uses replace on conflict algorithm when storing items. This means when
     * primary key or unique constraint or other constraint has conflict the row will be replaced with
     * provided data.</p>
     *
     * <p>Store operation is cascading.</p>
     *
     * @param objects
     * @return List of stored instances of given entities.
     *
     * @since 1.3.0
     */
    List<T> storeAll(Collection<T> objects);

    /**
     * Removes entity with given id from database completely. Remove operation is performed for entity
     * that the current repository is managing. Remove operation is cascading.
     *
     * @param id Id of the entity to remove.
     * @return Int value of number of affected rows in total.
     *
     * @since 1.3.0
     */
    int remove(K id);

    /**
     * Removes entities with given ids from database completely. Remove operation is performed for entity
     * that the current repository is managing. Remove operation is cascading.
     *
     * @param ids Collection of ids of the entities to remove.
     * @return Int value of number of affected rows in total.
     *
     * @since 1.3.0
     */
    int removeAll(Collection<K> ids);

    /**
     * Find one entity with given id from database. Query is performed for entity that the repository
     * is managing. If entity is not found null will be returned.
     * No joins to other tables is performed explicitly. But if user has defined some relations
     * to EAGER then they will be fetched.
     *
     * @param id Id of the entity to look for.
     * @return Instance of found entity with the given id or null if not found.
     *
     * @since 1.3.0
     */
    T findOne(final K id);

    /**
     * Find one entity with given filter. Filter can be a single instance of {@link Filter} or
     * list of filters can be provided as {@link db.juhaku.juhakudb.filter.Filters}. In any case
     * expected result is one entity.
     *
     * @param filter Instance of {@link Filter} to create query for current entity as the root entity.
     * @return Instance of found entity or null if not found.
     *
     * @since 1.3.0
     */
    T findOne(Filter<T> filter);

    /**
     * Find all the entities from database. Query is performed for the current entity that the
     * repository is managing.
     *
     * @return List of all the the entities form database.
     *
     * @since 1.3.0
     */
    List<T> findAll();

    /**
     * Find list of entities with given filter. Filter can be a single instance of {@link Filter} or
     * list of filters can be provided as {@link db.juhaku.juhakudb.filter.Filters}.
     *
     * @param filter Instance of {@link Filter} to create query for current entity as the root entity.
     * @return List of found entities or null if not found.
     *
     * @since 1.3.0
     */
    List<T> find(Filter<T> filter);

    /**
     * Perform custom query to database and return the result. Result can be transformed with
     * {@link ResultTransformer}. Query can contain the sql and args as array. If args is provided
     * the query should have ? marks as indicators that they should be substituted with the given
     * arguments.
     * <p>The query could be like e.g. <br/><code>new Query("select id, name from persons where name = ?",
     * new String[]{"john"});</code></p>
     *
     * @param query Instance of custom {@link Query} to perform in the database.
     * @param resultTransformer Instance of {@link ResultTransformer} to transform result of the query.
     * @return The result that is transformed by the result transformer.
     *
     * @since 1.3.0
     */
    <E> E find(Query query, ResultTransformer<E> resultTransformer);

}
