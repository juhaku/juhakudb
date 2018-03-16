/**
MIT License

Copyright (c) 2018 juhaku

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
package db.juhaku.juhakudb.repository.android;

import java.util.Collection;
import java.util.List;

import javax.persistence.Id;

import db.juhaku.juhakudb.core.NameResolver;
import db.juhaku.juhakudb.core.android.EntityManager;
import db.juhaku.juhakudb.core.android.ResultTransformer;
import db.juhaku.juhakudb.exception.MappingException;
import db.juhaku.juhakudb.exception.NameResolveException;
import db.juhaku.juhakudb.filter.Filter;
import db.juhaku.juhakudb.filter.Predicate;
import db.juhaku.juhakudb.filter.PredicateBuilder;
import db.juhaku.juhakudb.filter.Predicates;
import db.juhaku.juhakudb.filter.Query;
import db.juhaku.juhakudb.filter.Root;
import db.juhaku.juhakudb.repository.SimpleRepository;
import db.juhaku.juhakudb.util.ReflectionUtils;

/**
 * Created by juha on 15/03/17.
 *<p>General super repository class for all repositories. This repository provides
 * implementation for methods described in {@link SimpleRepository}.</p>
 *
 * <p>This repository can be extended if custom repository is what is needed to implement. Normally
 * it will be automatically implemented for you so all you need to do is to provide interface for
 * your repository. See {@link SimpleRepository} for additional info.</p>
 *
 * @author Juha Kukkonen
 *
 * @see SimpleRepository
 *
 * @since 1.1.3
 */
public abstract class SimpleAndroidRepository<K, T>  implements SimpleRepository<K, T> {

    private Class<?> persistentClass;
    private EntityManager entityManager;

    /**
     * Initialize new instance of simple android repository. Persistent class will be resolved
     * automatically from super class.
     *
     * @param entityManager Instance of {@link EntityManager}.
     *
     * @since 1.3.0
     */
    public SimpleAndroidRepository(EntityManager entityManager) {
        this.persistentClass = ReflectionUtils.getClassGenericTypes(this.getClass())[1];
        this.entityManager = entityManager;
    }

    /**
     * Initialize new instance of simple android repository with persistent class provided.
     *
     * @param entityManager Instance of {@link EntityManager}.
     *
     * @since 1.3.0
     */
    public SimpleAndroidRepository(EntityManager entityManager, Class<T> persistentClass) {
        this.entityManager = entityManager;
        this.persistentClass = persistentClass;
    }

    @Override
    public T store(T object) {
        return entityManager.store(object);
    }

    @Override
    public List<T> storeAll(Collection<T> objects) {
        return entityManager.store(objects);
    }

    @Override
    public int remove(K id) {
        return entityManager.delete(persistentClass, id);
    }

    @Override
    public int removeAll(Collection<K> ids) {
        return entityManager.delete(persistentClass, ids);
    }

    @Override
    public T findOne(final K id) {
        return findOne(new Filter<T>() {
            @Override
            public void filter(Root<T> root, PredicateBuilder builder) {
                builder.eq(resolveIdColumnName(), id);
            }
        });
    }

    @Override
    public T findOne(Filter<T> filter) {
        List<T> result = find(filter);
        if (result.isEmpty()) {

            return null;
        } else {

            return result.get(0);
        }
    }

    @Override
    public List<T> findAll() {
        return find(new Filter<T>() {
            @Override
            public void filter(Root<T> root, PredicateBuilder builder) {
            }
        });
    }

    @Override
    public List<T> find(Filter<T> filter) {

        return entityManager.query(persistentClass, filter);
    }

    @Override
    public <E> E find(Query query, ResultTransformer<E> resultTransformer) {
        return entityManager.query(persistentClass, query, resultTransformer);
    }

    /**
     * Resolve id column name for persistent class that is being used with this repository. If
     * any error occurs during resolving an exception will be thrown.
     *
     * @return String value if id column for persistent class of the repository.
     *
     * @since 1.3.0
     */
    String resolveIdColumnName() {
        try {
            return NameResolver.resolveIdName(persistentClass);
        } catch (NameResolveException e) {
            throw new MappingException("Failed to execute query, could not resolve id column for class: "
                    + persistentClass.getName() + " is " + Id.class.getName() + " annotation provided?", e);
        }
    }
}
