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

import android.database.Cursor;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import javax.persistence.FetchType;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;

import db.juhaku.juhakudb.core.android.ResultSet;
import db.juhaku.juhakudb.core.android.ResultTransformer;
import db.juhaku.juhakudb.exception.MappingException;
import db.juhaku.juhakudb.filter.Filter;
import db.juhaku.juhakudb.filter.JoinMode;
import db.juhaku.juhakudb.filter.PredicateBuilder;
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

    /**
     * Perform given query.
     *
     * @param query Instance of {@link Query} to be performed.
     * @param rootClass Root class of the queried entity.
     * @param parentEntity Cascading query parent entity.
     * @param parentField Cascading query field.
     *
     * @since 2.0.0
     *
     * @hide
     */
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

            /*
             * If query is performed for the root object set the final result. Otherwise add result
             * to the parent entity.
             */
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

    /**
     * Cascade query for given query result of objects. Cascading will be done if fields of the
     * returned entities has EAGER loading allowed.
     *
     * @param result List of query result objects to cascade query for.
     * @param rootClass Root class of the queried entity.
     *
     * @since 2.0.0
     *
     * @hide
     */
    private <E> void cascadeQuery(List<E> result, final Class<?> rootClass) {
        for (final E entity : result) {

            // set primary key associations if available
            for (Field field : rootClass.getDeclaredFields()) {
                field.setAccessible(true);

                final Object fieldValue = ReflectionUtils.getFieldValue(entity, field);
                final Class<?> type = ReflectionUtils.getFieldType(field);

                // If field references to a foreign key in another table fetch items if necessary
                if (isPrimaryKeyReverseJoinEagerFetchAllowed(field) && !isCached(new FetchHistory(field.getName(), entity.getClass()))) {

                    Query primaryKeySubQuery = getProcessor().createQuery(type, new Filter() {
                        @Override
                        public void filter(Root root, PredicateBuilder builder) {
                            String alias = Alias.forModel(rootClass);
                            Object id = ReflectionUtils.getIdFieldValue(entity);

                            // TODO may break the functionality if multiple joins occurs to same table with same type.
                            root.join(getAssociatedRootClassFieldNameByType(type, rootClass),
                                    alias, JoinMode.INNER_JOIN);

                            builder.eq(String.valueOf(alias)
                                    .concat(".").concat(resolveIdColumn(rootClass)), id);
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

                    if (isForeignKeyJoinEagerFetchAllowed(field) && fieldValue != null) {

                        Query associatedSubQuery = getProcessor().createQuery(type, new Filter() {
                            @Override
                            public void filter(Root root, PredicateBuilder builder) {
                                String alias = Alias.forModel(type);
                                Object id = ReflectionUtils.getFieldValue(fieldValue, ReflectionUtils.findIdField(fieldValue.getClass()));

                                builder.eq(alias.concat(".").concat(resolveIdColumn(type)), id);
                            }
                        });
                        query(associatedSubQuery, type, entity, field);
                    }
                }
            }
        }
    }

    /**
     * Get reverse join class field name by root class and type of field to look for. Method will
     * return first field which type is given type.
     *
     * @param clazz Instance of class to look for field from.
     * @param type Instance of class which type of field is looked for.
     * @return String name of the field from root class.
     *
     * @since 2.0.0
     *
     * @hide
     */
    private static String getAssociatedRootClassFieldNameByType(Class<?> clazz, Class<?> type) {
        for (Field field : clazz.getDeclaredFields()) {
            field.setAccessible(true);
            if (ReflectionUtils.getFieldType(field).isAssignableFrom(type)) {
                return field.getName();
            }
        }

        return null;
    }

    /**
     * Copies given list of elements to collection of given class type. Class type must be assignable
     * from {@link Collection} class.
     *
     * @param result List of elements to copy.
     * @param type Instance of collection {@link Class}.
     * @return Collection of elements with type of given collection class.
     *
     * @since 2.0.0
     *
     * @hide
     */
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

    /**
     * Checks whether foreign key join has {@link FetchType} set to EAGER.
     * @param field Instance of {@link Field}.
     * @return returns true if eager fetch is allowed; false otherwise.
     *
     * @since 2.0.0
     *
     * @hide
     */
    private static boolean isForeignKeyJoinEagerFetchAllowed(Field field) {
        return (field.isAnnotationPresent(ManyToOne.class) && field.getAnnotation(ManyToOne.class).fetch() == FetchType.EAGER)
                || (field.isAnnotationPresent(OneToOne.class) && field.getAnnotation(OneToOne.class).fetch() == FetchType.EAGER)
                && StringUtils.isBlank(field.getAnnotation(OneToOne.class).mappedBy());
    }

    /**
     * Checks whether primary key join has {@link FetchType} set to EAGER.
     * @param field Instance of {@link Field}.
     * @return returns true if eager fetch is allowed; false otherwise.
     *
     * @since 2.0.0
     *
     * @hide
     */
    private static boolean isPrimaryKeyReverseJoinEagerFetchAllowed(Field field) {
        return ((field.isAnnotationPresent(ManyToMany.class) && field.getAnnotation(ManyToMany.class).fetch() == FetchType.EAGER))
                || (field.isAnnotationPresent(OneToOne.class) && field.getAnnotation(OneToOne.class).fetch() == FetchType.EAGER
                && !StringUtils.isBlank(field.getAnnotation(OneToOne.class).mappedBy())
                || (field.isAnnotationPresent(OneToMany.class) && field.getAnnotation(OneToMany.class).fetch() == FetchType.EAGER));
    }

    /**
     * Wrapper class to wrap field name and class what is fetched already.
     *
     * @since 1.2.0
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
