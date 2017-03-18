package db.juhaku.juhakudb.repository.android;

import java.util.Collection;
import java.util.List;

import db.juhaku.juhakudb.annotation.Id;
import db.juhaku.juhakudb.core.NameResolver;
import db.juhaku.juhakudb.core.android.EntityManager;
import db.juhaku.juhakudb.core.android.ResultTransformer;
import db.juhaku.juhakudb.exception.MappingException;
import db.juhaku.juhakudb.exception.NameResolveException;
import db.juhaku.juhakudb.filter.Filter;
import db.juhaku.juhakudb.filter.Predicate;
import db.juhaku.juhakudb.filter.Predicates;
import db.juhaku.juhakudb.filter.Query;
import db.juhaku.juhakudb.filter.Root;
import db.juhaku.juhakudb.util.ReflectionUtils;
import db.juhaku.juhakudb.util.TypedClass;

/**
 * Created by juha on 15/03/17.
 *<p>Super repository class for all repositories in Android devices. This repository provides
 * simple access to database to perform queries.</p>
 * <p>There is no need to initialize repositories manually as they are initialized by
 * {@link db.juhaku.juhakudb.core.android.DatabaseManager} by default. And so also available
 * via database manager.</p>
 * <strong>Example to create repository:</strong><br/>
 * <code>
 *  Create your repository interface:<br/><br/>
 *  &#64;Repository(DemoRepositoryImpl.class)<br/>
 *  public interface DemoRepository &#123;<br/>
 *  &#9;Long storeBook(Book book);<br/>
 *  &#9;List<Book> findBooks(Filters filters);<br/>
 *  &#125;<br/><br/>
 *  Create your repository implementation:<br/><br/>
 *  public class DemoRepositoryImpl extends SimpleAndroidRepository&lt;Long, Book&gt; implements DemoRepository &#123;<br/>
 *  <br/>
 *  &#9;public DemoRepositoryImpl(DatabaseHelper helper) &#123;<br/>
 *  &#9;&#9;super(helper);<br/>
 *  &#9;&#125;<br/><br/>
 *
 *  &#9;&#64;Override<br/>
 *  &#9;public Long storeBook(Book book) &#123;<br/>
 *  &#9;&#9;return store(book);<br/>
 *  &#9;&#125;<br/><br/>
 *
 *  &#9;&#64;Override<br/>
 *  &#9;public List&lt;Book&gt; findBooks(Filters filters) &#123;<br/>
 *  &#9;&#9;return find(filters);<br/>
 *  &#9;&#125;<br/><br/>
 *
 *  &#125;<br/><br/>
 *
 * Access to your repository by:<br/><br/>
 *  DemoRepository repository = dbManager.getRepository(DemoRepository.class);<br/>
 * </code>
 * @author juha
 * @see db.juhaku.juhakudb.core.android.DatabaseManager
 * @see db.juhaku.juhakudb.annotation.Repository
 *
 * @since 1.1.3
 */
public abstract class SimpleAndroidRepository<K, T> {

    private Class<?> persistentClass;
    private EntityManager entityManager;

    public SimpleAndroidRepository(EntityManager entityManager) {
        this.persistentClass = ReflectionUtils.getClassGenericTypes(this.getClass())[1];
        this.entityManager = entityManager;
    }

    public T store(T object) {
        return entityManager.store(object);
    }

    public List<T> storeAll(List<T> objects) {
        return entityManager.store(objects);
    }

    public int remove(K id) {
        return entityManager.delete(persistentClass, id);
    }

    public int removeAll(Collection<K> ids) {
        return entityManager.delete(persistentClass, ids);
    }

    public T findOne(final K id) {
        return findOne(new Filter<T>() {
            @Override
            public void filter(Root<T> root, Predicates predicates) {
                predicates.add(Predicate.eq(resolveIdColumnName(), id));
            }
        });
    }

    public T findOne(Filter<T> filter) {
        List<T> result = find(filter);
        if (result.isEmpty()) {

            return null;
        } else {

            return result.get(0);
        }
    }

    public List<T> find(Filter<T> filter) {

        return entityManager.query(persistentClass, filter);
    }

    public <E> E find(Query query, ResultTransformer<E> resultTransformer) {
        return entityManager.query(persistentClass, query, resultTransformer);
    }

    String resolveIdColumnName() {
        try {
            return NameResolver.resolveIdName(persistentClass);
        } catch (NameResolveException e) {
            throw new MappingException("Failed to execute query, could not resolve id column for class: "
                    + persistentClass.getName() + " is " + Id.class.getName() + " annotation provided?", e);
        }
    }
}
