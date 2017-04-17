package db.juhaku.juhakudb.core.android.transaction;

import android.content.ContentValues;
import android.util.Log;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import db.juhaku.juhakudb.annotation.ManyToMany;
import db.juhaku.juhakudb.annotation.ManyToOne;
import db.juhaku.juhakudb.annotation.OneToMany;
import db.juhaku.juhakudb.annotation.OneToOne;
import db.juhaku.juhakudb.core.Cascade;
import db.juhaku.juhakudb.core.NameResolver;
import db.juhaku.juhakudb.core.schema.Reference;
import db.juhaku.juhakudb.core.schema.Schema;
import db.juhaku.juhakudb.exception.IllegalJoinException;
import db.juhaku.juhakudb.exception.MappingException;
import db.juhaku.juhakudb.filter.Filter;
import db.juhaku.juhakudb.filter.Predicate;
import db.juhaku.juhakudb.filter.Predicates;
import db.juhaku.juhakudb.filter.Query;
import db.juhaku.juhakudb.filter.Root;
import db.juhaku.juhakudb.util.ReflectionUtils;
import db.juhaku.juhakudb.util.StringUtils;

/**
 * Created by juha on 20/05/16.
 *
 * @author juha
 *
 * @since 1.0.2
 */
public class StoreTransactionTemplate<T> extends TransactionTemplate {

    private Collection<T> items;
    private List<PendingTransaction> cascadeList = new ArrayList<>();

    public void setItems(Collection<T> items) {
        this.items = new ArrayList<>(items); // transform collection of items to a list.
    }

    @Override
    void onTransaction() {
        store(items, null);
//        cascadeList.clear();
        setResult(items);
        commit();
    }


    private void store(Collection<T> items, Object parent) {
        for (T item : items) {

            cascadeStoreBefore(item);

            ContentValues values = getConverter().entityToContentValues(item);

            // If parent is specified add parent id to content values as it references to child.
            if (parent != null) {
                values.put(resolveReverseJoinColumnName(item.getClass(), parent.getClass()),
                        ReflectionUtils.getIdFieldValue(parent).toString());
            }

            Long id = insertOrReplace(resolveTableName(item.getClass()), values);


            // If storing was successful populate object with the database row id.
            if (id > -1) {

                ReflectionUtils.setFieldValue(ReflectionUtils.findIdField(item.getClass()), item, id);

                cascadeStoreAfter(item);

            } else {
                // Some general logging if storing fails.
                Log.v(getClass().getName(), "Failed to store item: " + item + " to database!");
            }
        }
    }

    private void cascadeStoreBefore(T item) {
        for (Field field : item.getClass().getDeclaredFields()) {

            Object value = ReflectionUtils.getFieldValue(item, field);

            /*
             * If field has foreign key relation it should be stored before the actual item is being
             * stored.
             */
            if (field.isAnnotationPresent(ManyToOne.class) ||
                    (field.isAnnotationPresent(OneToOne.class) && StringUtils.isBlank(field.getAnnotation(OneToOne.class).mappedBy()))) {

                // Check that there is actually something to store.
                if (value != null) {
                    store((Collection<T>) toCollection(value), null);
                }
            }
        }
    }

    private void cascadeStoreAfter(T item) {
        for (Field field : item.getClass().getDeclaredFields()) {

            Object value = ReflectionUtils.getFieldValue(item, field);

            /*
             * If field has primary key relation referenced item will be stored after the actual item
             * is being stored.
             */
            if (field.isAnnotationPresent(ManyToMany.class) || field.isAnnotationPresent(OneToMany.class)
                    || (field.isAnnotationPresent(OneToOne.class) && !StringUtils.isBlank(field.getAnnotation(OneToOne.class).mappedBy()))) {

                // Check that there is actually something to store.
                if (value != null) {

                    if (field.isAnnotationPresent(ManyToMany.class)) {

                        store((Collection<T>) toCollection(value), null);

                        // Update middle table reference for many to many relations.
                        storeMiddleTable(item, (Collection<T>) value);

                    } else {

                        store((Collection<T>) toCollection(value), item);
                    }
                }

            }
        }
    }

    private void storeMiddleTable(final T item, Collection<T> items) {
        final Schema middleTable = findMiddleTable(item.getClass(), items.iterator().next().getClass());

        /*
         * Delete existing references by id.
         */
        Query where = getProcessor().createWhere(null, new Filter() {
            @Override
            public void filter(Root root, Predicates predicates) {
                String middleTableJoinColumn = null;
                for (Reference reference : middleTable.getReferences()) {
                    if (reference.getReferenceTableName().equals(resolveTableName(item.getClass()))) {
                        middleTableJoinColumn = reference.getColumnName();
                    }
                }

                predicates.add(Predicate.eq(middleTableJoinColumn, ReflectionUtils.getIdFieldValue(item)));
            }
        });
        getDb().delete(middleTable.getName(), where.getSql(), where.getArgs());


        String fromTable = resolveTableName(item.getClass());

        /*
         * Store middle table references.
         */
        for (T joinItem : items) {

            // Create content value for each join item as each item represents one row in database.
            ContentValues values = new ContentValues();

            for (Reference reference : middleTable.getReferences()) {

                Object value;

                /*
                 * Determine which id is being used to to which reference. If reference table equals
                 * from table get id of the from item otherwise use to id.
                 */
                if (reference.getReferenceTableName().equals(fromTable)) {
                    value = ReflectionUtils.getIdFieldValue(item).toString();

                } else {

                    value = ReflectionUtils.getIdFieldValue(joinItem);
                }

                values.put(reference.getColumnName(), value.toString());
            }

            insertOrReplace(middleTable.getName(), values);
        }
    }

    /**
     * Find middle table by model class and reference model class.
     * @param model Entity class that join is made from.
     * @param joinModel Entity class that join is made to.
     *
     * @return Schema found middle table or null if not found.
     *
     * @since 1.2.0-SNAPSHOT
     *
     * @hide
     */
    private Schema findMiddleTable(Class<?> model, Class<?> joinModel) {
        String tableName = resolveTableName(model);
        String joinTable = resolveTableName(joinModel);

        Schema middleTable;

        if ((middleTable = getSchema().getElement(tableName.concat("_").concat(joinTable))) == null) {
            middleTable = getSchema().getElement(joinTable.concat("_").concat(tableName));
        }

        return middleTable;
    }

    /**
     * Inserts or replaces given content values in given table. If SQL was executed successfully the
     * id of database row will be returned. If execution fails -1 will be returned.
     *
     * @param tableName String name of the table to store content values to.
     * @param values {@link ContentValues} that is being stored to given table.
     * @return Long id of stored row in database table or -1 if storing will fail.
     *
     * @since 1.2.0-SNAPSHOT
     *
     * @hide
     */
    private Long insertOrReplace(String tableName, ContentValues values) {
        return getDb().replace(tableName, null, values);
    }

    private void store(Collection<T> items, Class<?> parentClass, Long id, Class<?> toType) {
        for (T item : items) {

            cascadeBefore(item);

            Long storedId = -1L;

            if (id == null) {
                storedId = getDb().replace(resolveTableName(item.getClass()), null, getConverter().entityToContentValues(item));

            } else {
                PendingTransaction rootEntry = null;
                for (PendingTransaction transaction : cascadeList) {
                    if (transaction.getStoredId().longValue() == id.longValue() && transaction.getTo().equals(parentClass)) {
                        rootEntry = transaction;
                    }
                }

                if (rootEntry != null) {
                    /*
                     * Convert item to content values. Item will not have reference id so we
                     * need to add it afterwards.
                     */
                    ContentValues contentValues = getConverter().entityToContentValues(item);

                    /*
                     * Add reference id to associated table.
                     *
                     * TODO NOTE! Multiple joins might break the functionality.
                     */
                    contentValues.put(resolveReverseJoinColumnName(toType, parentClass), rootEntry.getStoredId());

                    storedId = getDb().replace(resolveTableName(item.getClass()), null, contentValues);
                }
            }

            // After successful store add pending transaction to trace back successful transactions.
            if (storedId != -1) {
                PendingTransaction transaction = new PendingTransaction(storedId, parentClass, item.getClass());
                cascadeList.add(transaction);
                setIdToItem(item, storedId);
            }

            cascadeAfter(item, storedId);
        }
    }

    private void cascadeBefore(T item) {
        Class<?> clazz = item.getClass();
        for (Field field : clazz.getDeclaredFields()) {
            field.setAccessible(true);

            //TODO probably remove cascade check as it will be forced to store
            if (field.isAnnotationPresent(ManyToMany.class)) {
                ManyToMany manyToMany = field.getAnnotation(ManyToMany.class);
                if (manyToMany.cascade() == Cascade.STORE || manyToMany.cascade() == Cascade.ALL) {
                    storeCascadeBefore(item, field);
                }
            }
            if (field.isAnnotationPresent(ManyToOne.class)) {
                ManyToOne manyToOne = field.getAnnotation(ManyToOne.class);
                if (manyToOne.cascade() == Cascade.STORE || manyToOne.cascade() == Cascade.ALL) {
                    storeCascadeBefore(item, field);
                }
            }
            if (field.isAnnotationPresent(OneToOne.class)) {
                OneToOne oneToOne = field.getAnnotation(OneToOne.class);
                if (StringUtils.isBlank(oneToOne.mappedBy()) && (oneToOne.cascade() == Cascade.STORE
                        || oneToOne.cascade() == Cascade.ALL)) {
                    storeCascadeBefore(item, field);
                }
            }
        }
    }

    private void storeCascadeBefore(T item, Field field) {
        T value = ReflectionUtils.getFieldValue(item, field);

        if (value != null) {
            store(toCollection(value), item.getClass(), null, null);
        }
    }

    private void cascadeAfter(T item, Long id) {
        Class<?> clazz = item.getClass();
        for (Field field : clazz.getDeclaredFields()) {
            field.setAccessible(true);
            Class<?> type = ReflectionUtils.getFieldType(field);

            if (field.isAnnotationPresent(ManyToMany.class)) {
                ManyToMany manyToMany = field.getAnnotation(ManyToMany.class);
                if (manyToMany.cascade() == Cascade.STORE || manyToMany.cascade() == Cascade.ALL) {

                    List<PendingTransaction> transactions = new ArrayList<>();
                    PendingTransaction rootTransaction = null;
                    for (PendingTransaction transaction : cascadeList) {
                        if (transaction.getTo().equals(type) && transaction.getFrom().isAssignableFrom(item.getClass())) {
                            transactions.add(transaction);
                        } else if (transaction.getStoredId().longValue() == id.longValue() && transaction.getFrom().isAssignableFrom(item.getClass())) {
                            rootTransaction = transaction;
                        }
                    }
                    if (rootTransaction != null && transactions.size() > 0) {
                        storeCascadeManyToMany(transactions, rootTransaction);
                    }
                }
            }
            if (field.isAnnotationPresent(OneToOne.class) && !StringUtils.isBlank(field.getAnnotation(OneToOne.class).mappedBy())
                    || field.isAnnotationPresent(OneToMany.class)) {
                T value = ReflectionUtils.getFieldValue(item, field);

                if (value != null) {
                    store(toCollection(value), item.getClass(), id, type);
                }
            }
        }
    }

    /**
     * Maps given value to collection if value itself is not assignable from collection.
     * @param value Object value to map.
     * @return Collection containing given value or if value is collection then itself will be returned.
     *
     * @since 1.2.0-SNAPSHOT
     *
     * @hide
     */
    private static <T> Collection<T> toCollection(T value) {
        if (Collection.class.isAssignableFrom(value.getClass())) {
            return (Collection<T>) value;
        } else {
            return Arrays.asList(value);
        }
    }

    private String resolveReverseJoinColumnName(Class<?> model, Class<?> reverseModel) {
        Schema table = getSchema().getElement(resolveTableName(model));

        if (table != null) {
            String reverseJoinTableName = resolveTableName(reverseModel);

            for (Reference reference : table.getReferences()) {
                if (reference.getReferenceTableName().equals(reverseJoinTableName)) {
                    return reference.getColumnName();
                }
            }
        }

        return null;
    }

    private void storeCascadeManyToMany(final List<PendingTransaction> transactions, final PendingTransaction transaction) {
        String table;
        String rootTableName;
        String associateTableName;
        rootTableName = resolveTableName(transaction.getFrom());
        associateTableName = resolveTableName(transactions.get(0).getTo());
        table = findManyToManyTable(rootTableName, associateTableName);

        final String fromTable = rootTableName;
        Query query = getProcessor().createWhere(null, new Filter() {
            @Override
            public void filter(Root root, Predicates predicates) {
                predicates.add(Predicate.eq(fromTable.concat(NameResolver.ID_FIELD_SUFFIX), transaction.getStoredId()));
            }
        });
        getDb().delete(table, query.getSql(), query.getArgs());

        for (PendingTransaction pendingTransaction : transactions) {
            getDb().insert(table, null, createManyToManyContentValues(pendingTransaction, transaction, rootTableName, associateTableName));
        }
        cascadeList.clear();
    }

    private String findManyToManyTable(String rootTableName, String associateTableName) {
        Schema table;
        if ((table = getSchema().getElement(rootTableName.concat("_").concat(associateTableName))) == null) {
            return getSchema().getElement(associateTableName.concat("_").concat(rootTableName)).getName();
        } else {
            return table.getName();
        }
    }

    private ContentValues createManyToManyContentValues(PendingTransaction pendingTransaction,
                                                        PendingTransaction rootTransaction,
                                                        String rootTableName, String associateTableName) {
        ContentValues values = new ContentValues();
        values.put(rootTableName.concat(NameResolver.ID_FIELD_SUFFIX), rootTransaction.getStoredId());
        values.put(associateTableName.concat(NameResolver.ID_FIELD_SUFFIX), pendingTransaction.getStoredId());

        return values;
    }

    private void setIdToItem(T item, Long id) {
        try {
            Field field = ReflectionUtils.findIdField(item.getClass());
            field.set(item, id);
        } catch (IllegalAccessException e) {
            throw new MappingException("Could not set id: " + id + " to item: " + item.getClass(), e);
        }
    }
}
