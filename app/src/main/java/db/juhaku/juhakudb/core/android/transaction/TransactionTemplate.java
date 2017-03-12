package db.juhaku.juhakudb.core.android.transaction;

import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.List;

import db.juhaku.juhakudb.core.NameResolver;
import db.juhaku.juhakudb.core.android.EntityConverter;
import db.juhaku.juhakudb.core.schema.Schema;
import db.juhaku.juhakudb.exception.MappingException;
import db.juhaku.juhakudb.exception.NameResolveException;
import db.juhaku.juhakudb.filter.QueryCreator;

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
    private QueryCreator creator;
    private EntityConverter converter;
    private boolean successful = false;
    private List<String> tableCache;

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
     * Set query creator for template that is used to create database queries in simplified manner.
     * @param creator instance of {@link QueryCreator}.
     *
     * @since 1.0.2
     */
    public final void setCreator(QueryCreator creator) {
        this.creator = creator;
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
     * Get the previously put query creator.
     * @return instance of {@link QueryCreator}.
     *
     * @since 1.0.2
     */
    QueryCreator getCreator() {
        return creator;
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
    private void clearCache() {
        tableCache = null;
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
     * Helper class that contains necessary information about pending transactions that should be
     * executed before or after the main transaction process is executed.
     * <p>Used to execute additional queries to database to keep the data integrated.</p>
     *
     * @since 1.0.2
     *
     * @hide
     */
    class PendingTransaction {
        private Long storedId;
        private Class<?> from;
        private Class<?> to;

        public PendingTransaction(Long storedId, Class<?> from, Class<?> to) {
            this.storedId = storedId;
            this.from = from;
            this.to = to;
        }

        Long getStoredId() {
            return storedId;
        }

        Class<?> getFrom() {
            return from;
        }

        Class<?> getTo() {
            return to;
        }
    }

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
     * Check whether table is cached to table cache.
     * <p>Table cache will be cleared always at end of transaction automatically.</p>
     * @param table String value of table name.
     * @return boolean value; true if table is cached; otherwise false;
     *
     * @since 1.0.2
     */
    final boolean isCached(String table) {
        return (tableCache == null ? false : (tableCache.contains(table)));
    }

    /**
     * Insert table to table cache for later checking. Each table can only be set once to cache.
     * <p>Table cache will be cleared always at end of transaction automatically.</p>
     * @param table String value of table name.
     *
     * @since 1.0.2
     */
    final void cache(String table) {
        if (tableCache == null) {
            tableCache = new ArrayList<>();
        }
        if (!isCached(table)) {
            tableCache.add(table);
        }
    }
}
