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
package db.juhaku.juhakudb.core.android.transaction;

import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.List;

import db.juhaku.juhakudb.core.NameResolver;
import db.juhaku.juhakudb.core.android.EntityConverter;
import db.juhaku.juhakudb.core.schema.Schema;
import db.juhaku.juhakudb.exception.MappingException;
import db.juhaku.juhakudb.exception.NameResolveException;
import db.juhaku.juhakudb.filter.QueryProcessor;

/**
 * Created by juha on 12/05/16.
 *<p>General level abstract transaction template for executing queries of all sorts in database.</p>
 * @author juha
 * @since 1.0.2
 */
public abstract class TransactionTemplate<T> {

    private SQLiteDatabase db;
    private T result;
    private Schema schema;
    private Class<?> rootClass;
    private QueryProcessor processor;
    private EntityConverter converter;
    private boolean successful = false;
    private List<Object> resultCache;

    /**
     * This method will execute the query inside a transaction against database. Do not
     * call this method as it is called automatically. <br/><br/>
     * This method will call {@link #onTransaction()}
     * that should be implemented by child transaction template to provide action that is performed
     * against database.
     *
     * @since 1.0.2
     */
    public final void execute() {
        db.beginTransactionNonExclusive();
        try {
            onTransaction();
            clearCache();
            if (successful) {
                db.setTransactionSuccessful();
            }
        } finally {
            db.endTransaction();
        }
    }

    /**
     * Set result for the query available for later usage.
     *
     * @param result Result of the query.
     *
     * @since 1.0.2
     */
    final void setResult(T result) {
        this.result = result;
    }

    /**
     * Get the result of query if it was set.
     * @return result of the query.
     *
     * @since 1.0.2
     */
    public final T getResult() {
        return result;
    }

    /**
     * Set database to execute query against.
     * @param db instance of {@link SQLiteDatabase}.
     *
     * @since 1.0.2
     */
    public final void setDb(SQLiteDatabase db) {
        this.db = db;
    }

    /**
     * Get database for executing the query.
     * @return the database that was put to template.
     *
     * @since 1.0.2
     */
    SQLiteDatabase getDb() {
        return db;
    }

    /**
     * Set schema of database.
     * @param schema instance of {@link Schema}.
     *
     * @since 1.0.2
     */
    public final void setSchema(Schema schema) {
        this.schema = schema;
    }

    Schema getSchema() {
        return schema;
    }

    /**
     * Set root class for the query.
     * @param rootClass Class of the root table in database.
     *
     * @since 1.0.2
     */
    public final void setRootClass(Class<?> rootClass) {
        this.rootClass = rootClass;
    }

    /**
     * Get the root class of table in database.
     * @return Class of root table.
     *
     * @since 1.0.2
     */
    Class<?> getRootClass() {
        return rootClass;
    }

    /**
     * Set query processor for template that is used to create database queries in simplified manner.
     * @param processor instance of {@link QueryProcessor}.
     *
     * @since 1.2.0
     */
    public final void setProcessor(QueryProcessor processor) {
        this.processor = processor;
    }

    /**
     * Gets entity converter to convert entities to database table rows and vise versa.
     * @return instance of {@link EntityConverter}.
     *
     * @since 1.0.2
     */
    EntityConverter getConverter() {
        return converter;
    }

    /**
     * Set entity converter that is used to transform entities to database table rows and vise versa.
     * @param converter instance of {@link EntityConverter}.
     *
     * @since 1.0.2
     */
    public final void setConverter(EntityConverter converter) {
        this.converter = converter;
    }

    /**
     * Get the previously put query processor.
     * @return instance of {@link QueryProcessor}.
     *
     * @since 1.2.0
     */
    QueryProcessor getProcessor() {
        return processor;
    }

    /**
     * Marks transaction as unsuccessful and rollback it.
     *
     * @since 1.0.2
     */
    final void rollback() {
        successful = false;
    }

    /**
     * Marks transaction successful and commits it.
     *
     * @since 1.0.2
     */
    final void commit() {
        successful = true;
    }

    /**
     * Clears the table cache.
     * @hide
     */
    void clearCache() {
        resultCache = null;
    }

    /**
     * Implementation of this method is executed inside transaction.
     * <p>Remember to call either {@link #commit()} or {@link #rollback()} at the end of
     * this method. If neither of them is called transaction will be rolled back.</p>
     *
     * @since 1.0.2
     */
    abstract void onTransaction();

    /**
     * Resolve id column name of given class. Class must be used as an entity for database. If
     * name cannot be resolved mapping exception will be thrown.
     * @param clazz Class to resolve id column name for.
     * @return String value of id column name if found; otherwise will be null. If name
     * cannot ber resolved an mapping exception will be thrown.
     *
     * @since 1.0.2
     */
    public static final String resolveIdColumn(Class<?> clazz) {
        try {
            return NameResolver.resolveIdName(clazz);
        } catch (NameResolveException e) {
            throw new MappingException("Could not resolve id column name for entity: " + clazz, e);
        }
    }

    /**
     * Resolve table name of given class. Class must be used as an entity for database. If
     * name cannot be resolved mapping exception will be thrown.
     * @param clazz Class to resolve table name for.
     * @return String value of table name if found; otherwise will be null. If name
     * cannot ber resolved an mapping exception will be thrown.
     *
     * @since 1.0.2
     */
    public static final String resolveTableName(Class<?> clazz) {
        try {
            return NameResolver.resolveName(clazz);
        } catch (NameResolveException e) {
            throw new MappingException("Failed to resolve associate entity table: ", e);
        }
    }

    /**
     * Check whether result is cached to result cache.
     * <p>Result cache will be cleared always at end of transaction automatically.</p>
     * @param result String value of result name.
     * @return boolean value; true if result is cached; otherwise false;
     *
     * @since 1.0.2
     */
    final boolean isCached(Object result) {
        return (resultCache == null ? false : (resultCache.contains(result)));
    }

    /**
     * Insert object to object cache for later checking. Each object can only be set once to cache.
     * <p>Result cache will be cleared always at end of transaction automatically.</p>
     * @param result Object to put to the cache.
     *
     * @since 1.0.2
     */
    final void cache(Object result) {
        if (resultCache == null) {
            resultCache = new ArrayList<>();
        }
        if (!isCached(result)) {
            resultCache.add(result);
        }
    }

    /**
     * Remove object from result cache.
     *
     * @param object Object that need to be removed from cache.
     *
     * @since 1.2.0
     */
    final void removeFromCache(Object object) {
        if (resultCache != null && isCached(object)) {
            resultCache.remove(object);
        }
    }

}
