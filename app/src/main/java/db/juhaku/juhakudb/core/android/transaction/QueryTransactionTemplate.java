package db.juhaku.juhakudb.core.android.transaction;

import android.database.Cursor;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import db.juhaku.juhakudb.annotation.ManyToMany;
import db.juhaku.juhakudb.annotation.ManyToOne;
import db.juhaku.juhakudb.annotation.OneToMany;
import db.juhaku.juhakudb.annotation.OneToOne;
import db.juhaku.juhakudb.core.Fetch;
import db.juhaku.juhakudb.core.android.ResultSet;
import db.juhaku.juhakudb.core.android.ResultTransformer;
import db.juhaku.juhakudb.exception.MappingException;
import db.juhaku.juhakudb.filter.Filter;
import db.juhaku.juhakudb.filter.JoinMode;
import db.juhaku.juhakudb.filter.Predicate;
import db.juhaku.juhakudb.filter.Predicates;
import db.juhaku.juhakudb.filter.Query;
import db.juhaku.juhakudb.filter.QueryProcessor.Alias;
import db.juhaku.juhakudb.filter.Root;
import db.juhaku.juhakudb.util.ReflectionUtils;
import db.juhaku.juhakudb.util.StringUtils;

/**
 * Created by juha on 24/05/16.
 *<p>Transaction template for returning data from database.</p>
 *
 * @author juha
 *
 * @since 1.0.2
 */
public class QueryTransactionTemplate<T> extends TransactionTemplate {

    private Query query;
    private ResultTransformer transformer;

    public void setQuery(Query query) {
        this.query = query;
    }

    public void setTransformer(ResultTransformer transformer) {
        this.transformer = transformer;
    }

    @Override
    void onTransaction() {
        query(query, getRootClass(), null, null);
        commit();
    }

    private void query(Query query, Class<?> rootClass, Object parentEntity, Field parentField) {
        Cursor retVal = getDb().rawQuery(query.getSql(), query.getArgs());

        if (transformer != null) {

            // For custom transformer perform a custom query and return after setting result
            List<ResultSet> result = getConverter().convertCursorToCustomResultSetList(retVal);
            setResult(transformer.transformResult(result));

            return;
        } else {

            List<?> result = getConverter().convertCursorToEntityList(retVal, query.getRoot());

            // Cascade the query for fetches & provide root always.
            cascadeQuery(result, rootClass);

            if (parentEntity != null) {
                if (parentField.isAnnotationPresent(ManyToMany.class) || parentField.isAnnotationPresent(OneToMany.class)) {

                    ReflectionUtils.setFieldValue(parentField, parentEntity, resultsToCollection(result, parentField.getType()));
                } else {

                    // Otherwise it will be one to one primary key association
                    // Add only if has results
                    if (!result.isEmpty()) {
                        ReflectionUtils.setFieldValue(parentField, parentEntity, result.get(0));
                    }
                }

            } else {
                setResult(result);
            }
        }
    }

    private <E> void cascadeQuery(List<E> result, final Class<?> rootClass) {
        for (final E entity : result) {

            // set primary key associations if available
            for (Field field : rootClass.getDeclaredFields()) {
                field.setAccessible(true);

                final Object fieldValue = ReflectionUtils.getFieldValue(entity, field);
                final Class<?> type = ReflectionUtils.getFieldType(field);

                // If field references to a foreign key in another table fetch items if necessary
                if (isPrimaryKeyAssociationFetchAllowed(field) && !isCached(new FetchHistory(field.getName(), entity.getClass()))) {

                    Query primaryKeySubQuery = getProcessor().createQuery(type, new Filter() {
                        @Override
                        public void filter(Root root, Predicates predicates) {
                            String alias = Alias.forModel(rootClass);
                            Object id = ReflectionUtils.getIdFieldValue(entity);

                            root.join(getAssociatedRootClassFieldNameByType(type, rootClass),
                                    alias, JoinMode.INNER_JOIN);

                            predicates.add(Predicate.eq(String.valueOf(alias)
                                    .concat(".").concat(resolveIdColumn(rootClass)), id));
                        }
                    });

                    cache(new FetchHistory(field.getName(), entity.getClass()));
                    query(primaryKeySubQuery, type, entity, field);

                    /*
                     * After cascading query is made remove it from cache in case next entity wants
                     * to make same cascading query.
                     */
                    removeFromCache(new FetchHistory(field.getName(), entity.getClass()));

                } else {

                    if (isForeignKeyAssociationFetchAllowed(field) && fieldValue != null) {

                        Query associatedSubQuery = getProcessor().createQuery(type, new Filter() {
                            @Override
                            public void filter(Root root, Predicates predicates) {
                                String alias = Alias.forModel(type);
                                Object id = ReflectionUtils.getFieldValue(fieldValue, ReflectionUtils.findIdField(fieldValue.getClass()));

                                predicates.add(Predicate.eq(alias.concat(".").concat(resolveIdColumn(type)), id));
                            }
                        });
                        query(associatedSubQuery, type, entity, field);
                    }
                }
            }
        }
    }

    private static String getAssociatedRootClassFieldNameByType(Class<?> clazz, Class<?> type) {
        for (Field field : clazz.getDeclaredFields()) {
            field.setAccessible(true);
            if (ReflectionUtils.getFieldType(field).isAssignableFrom(type)) {
                return field.getName();
            }
        }

        return null;
    }

    private <E> Collection<E> resultsToCollection(List<E> result, Class<?> type) {
        if (Set.class.isAssignableFrom(type)) {
            if (HashSet.class.isAssignableFrom(type)) {
                return new HashSet<>(result);
            } else if (TreeSet.class.isAssignableFrom(type)) {
                return new TreeSet<>(result);
            } else {
                throw new MappingException("Given type does not inherit: " + HashSet.class.getName() + " nor " + TreeSet.class.getName());
            }
        }
        if (List.class.isAssignableFrom(type)) {
            if (LinkedList.class.isAssignableFrom(type)) {
                return new LinkedList<>(result);
            } else {
                return new ArrayList<>(result);
            }
        }

        return null;
    }

    private static boolean isForeignKeyAssociationFetchAllowed(Field field) {
        return (field.isAnnotationPresent(ManyToOne.class) && field.getAnnotation(ManyToOne.class).fetch() == Fetch.EAGER)
                || (field.isAnnotationPresent(OneToOne.class) && field.getAnnotation(OneToOne.class).fetch() == Fetch.EAGER)
                && StringUtils.isBlank(field.getAnnotation(OneToOne.class).mappedBy());
    }

    private static boolean isPrimaryKeyAssociationFetchAllowed(Field field) {
        return ((field.isAnnotationPresent(ManyToMany.class) && field.getAnnotation(ManyToMany.class).fetch() == Fetch.EAGER))
                || (field.isAnnotationPresent(OneToOne.class) && field.getAnnotation(OneToOne.class).fetch() == Fetch.EAGER
                && !StringUtils.isBlank(field.getAnnotation(OneToOne.class).mappedBy())
                || (field.isAnnotationPresent(OneToMany.class) && field.getAnnotation(OneToMany.class).fetch() == Fetch.EAGER));
    }

    /**
     * Wrapper class to wrap field name and class what is fetched already.
     *
     * @since 1.2.0-SNAPSHOT
     *
     * @hide
     */
    private static class FetchHistory {
        private String fieldName;
        private Class<?> clazz;

        FetchHistory(String fieldName, Class<?> clazz) {
            this.fieldName = fieldName;
            this.clazz = clazz;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            FetchHistory that = (FetchHistory) o;

            if (fieldName != null ? !fieldName.equals(that.fieldName) : that.fieldName != null)
                return false;
            return clazz.getName() != null ? clazz.getName().equals(that.clazz.getName()) : that.clazz.getName() == null;
        }

        @Override
        public int hashCode() {
            int result = fieldName != null ? fieldName.hashCode() : 0;
            result = 31 * result + (clazz != null ? clazz.hashCode() : 0);
            return result;
        }
    }
}
