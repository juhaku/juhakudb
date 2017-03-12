package db.juhaku.juhakudb.core.android.transaction;

import android.content.ContentValues;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;

import db.juhaku.juhakudb.annotation.ManyToMany;
import db.juhaku.juhakudb.annotation.ManyToOne;
import db.juhaku.juhakudb.annotation.OneToMany;
import db.juhaku.juhakudb.annotation.OneToOne;
import db.juhaku.juhakudb.core.Cascade;
import db.juhaku.juhakudb.core.NameResolver;
import db.juhaku.juhakudb.core.schema.Schema;
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
        store(items, getRootClass(), null);
        cascadeList.clear();
        setResult(items);
        commit();
    }

    private void store(Collection<T> items, Class<?> parentClass, Long id) {
        for (T item : items) {
            cascadeBefore(item);
            Long storedId = -1L;
            if (id == null) {
                storedId = getDb().replace(resolveTableName(item.getClass()), null, getConverter().entityToContentValues(item));
            } else {
                PendingTransaction rootEntry = null;
                for (PendingTransaction transaction : cascadeList) {
                    if (transaction.getStoredId() == id && transaction.getTo().equals(parentClass)) {
                        rootEntry = transaction;
                    }
                }
                if (rootEntry != null) {
                    ContentValues contentValues = getConverter().entityToContentValues(item);
                    // add reference id to associated table
                    contentValues.put(resolveTableName(rootEntry.getTo()).concat(NameResolver.ID_FIELD_SUFFIX), rootEntry.getStoredId());
                    storedId = getDb().replace(resolveTableName(item.getClass()), null, contentValues);
                }
            }
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
            if (Collection.class.isAssignableFrom(value.getClass())) {
                store((Collection<T>) value, item.getClass(), null);
            } else {
                store(Arrays.asList(value), item.getClass(), null);
            }
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
                        } else if (transaction.getStoredId() == id && transaction.getFrom().isAssignableFrom(item.getClass())) {
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
                    Collection<T> values;
                    if (Collection.class.isAssignableFrom(value.getClass())) {
                        values = (Collection<T>) value;
                    } else {
                        values = Arrays.asList(value);
                    }
                    store(values, item.getClass(), id);
                }
            }
        }
    }

    private void storeCascadeManyToMany(final List<PendingTransaction> transactions, final PendingTransaction transaction) {
        String table;
        String rootTableName;
        String associateTableName;
        rootTableName = resolveTableName(transaction.getFrom());
        associateTableName = resolveTableName(transactions.get(0).getTo());
        table = findManyToManyTable(rootTableName, associateTableName);

        final String fromTable = rootTableName;
        Query query = getCreator().createWhereClause(new Filter() {
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
