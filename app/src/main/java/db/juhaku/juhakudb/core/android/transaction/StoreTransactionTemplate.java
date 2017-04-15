package db.juhaku.juhakudb.core.android.transaction;

import android.content.ContentValues;

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
        store(items, getRootClass(), null, null);
        cascadeList.clear();
        setResult(items);
        commit();
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

                    // After storing many to many cascading values continue cascade.

//                    T value = ReflectionUtils.getFieldValue(item, field);
//
//                    if (value != null) {
//                        store(toCollection(value), item.getClass(), null, null);
//                    }
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
