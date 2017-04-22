package db.juhaku.juhakudb.core.android.transaction;

import android.content.ContentValues;
import android.util.Log;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;

import db.juhaku.juhakudb.core.schema.Reference;
import db.juhaku.juhakudb.core.schema.Schema;
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
 * <p>Store operation transaction template is used when one or multiple items are being stored
 * to database. All operations will cascade if items contains other entities.</p>
 *
 * @author juha
 *
 * @since 1.0.2
 */
public class StoreTransactionTemplate<T> extends TransactionTemplate {

    private Collection<T> items;

    public void setItems(Collection<T> items) {
        this.items = new ArrayList<>(items); // transform collection of items to a list.
    }

    @Override
    void onTransaction() {
        store(items, null);
        setResult(items);
        commit();
    }

    /**
     * Store given items to database. All store operations will be cascaded to referenced tables if
     * given items has child entities.
     *
     * @param items {@link Collection} of items to store.
     * @param parent Object parent entity that is being used to make foreign key relation to parent table.
     *
     * @since 1.2.0
     *
     * @hide
     */
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

    /**
     * Store cascade before the actual item is being stored. This stores items that the item itself
     * should refer to when being stored.
     *
     * @param item T item that is being cascade stored.
     *
     * @since 1.2.0
     *
     * @hide
     */
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

    /**
     * Store cascade after the item was stored to database. This stores items that are referenced
     * by the item itself.
     *
     * @param item T item that is being cascade stored.
     *
     * @since 1.2.0
     *
     * @hide
     */
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

    /**
     * Store middle table joins for given item. This is special processing that is being
     * executed after both join parties are stored to database. First existing references is being
     * deleted and then newly coming references is being stored to database for the item.
     *
     * <p>References is being created for given item from given collection of items.</p>
     *
     * @param item T item from table item.
     * @param items {@link Collection} of to table items.
     *
     * @since 1.2.0
     *
     * @hide
     */
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
     * @since 1.2.0
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
     * @since 1.2.0
     *
     * @hide
     */
    private Long insertOrReplace(String tableName, ContentValues values) {
        return getDb().replace(tableName, null, values);
    }

    /**
     * Maps given value to collection if value itself is not assignable from collection.
     * @param value Object value to map.
     * @return Collection containing given value or if value is collection then itself will be returned.
     *
     * @since 1.2.0
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

    /**
     * Resolves reverse join column name from given model class's table. Reverse join column name
     * is returned if reverse join table name is same as provided reverse join model.
     *
     * <p>Column is resolved by looking it for from {@link Schema} in order to maintain integrity.</p>
     *
     * @param model Instance of {@link Class} of model class of table where the join is made from.
     * @param reverseModel Instance of {@link Class} of reverse join model of table where the join is made to.
     * @return String reverse join column name from join table if found.
     *
     * @since 1.2.0
     *
     * @hide
     */
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
}
