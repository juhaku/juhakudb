package db.juhaku.juhakudb.core.android.transaction;

import android.database.Cursor;
import android.util.Log;

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
import db.juhaku.juhakudb.annotation.OneToOne;
import db.juhaku.juhakudb.core.Fetch;
import db.juhaku.juhakudb.core.NameResolver;
import db.juhaku.juhakudb.core.android.ResultSet;
import db.juhaku.juhakudb.core.android.ResultSet.Result;
import db.juhaku.juhakudb.core.android.ResultTransformer;
import db.juhaku.juhakudb.exception.ConversionException;
import db.juhaku.juhakudb.exception.MappingException;
import db.juhaku.juhakudb.exception.NameResolveException;
import db.juhaku.juhakudb.filter.Filter;
import db.juhaku.juhakudb.filter.JoinMode;
import db.juhaku.juhakudb.filter.Predicate;
import db.juhaku.juhakudb.filter.Predicates;
import db.juhaku.juhakudb.filter.Query;
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
            List<ResultSet> result = getConverter().cursorToResultSetList(retVal, rootClass, true);
            setResult(transformer.transformResult(result));
            return;
        } else {
            List<ResultSet> result = getConverter().cursorToResultSetList(retVal, rootClass, false);
            cascadeQuery(result, rootClass, query);
            if (parentEntity != null) {
                if (parentField.isAnnotationPresent(ManyToMany.class)) {
                    setFieldValue(parentField, parentEntity, resultSetListToCollection(result, parentField.getType()));
                } else { // otherwise it will be one to one primary key association
                    setFieldValue(parentField, parentEntity, result.get(0).getPopulatedEntity());
                }
            } else {
                setResult(resultSetListToCollection(result, ArrayList.class));
            }
        }
    }

    private <E> void cascadeQuery(List<ResultSet> result, final Class<?> rootClass, Query query) {
        for (ResultSet resultSet : result) {
            E entity = instantiate(rootClass);

            // set primary key associations if available
            for (Field field : rootClass.getDeclaredFields()) {
                field.setAccessible(true);
                try {
                    final Result column = resultSet.get(NameResolver.resolveName(field));
                    final Result idColumn = resultSet.get(resolveIdColumn(rootClass));
                    final Class<?> type = ReflectionUtils.getFieldType(field);
                    String referenceTable = null;
                    try {
                        referenceTable = NameResolver.resolveName(type);
                    } catch (NameResolveException e) {
                        // Just ignore the error because field's type is not a table in database.
                    }

                    // if field references to a foreign key in another table fetch items if necessary
                    if (isPrimaryKeyAssociationFetchAllowed(field, query, referenceTable) && !StringUtils.isBlank(referenceTable) && !isCached(type.getName())) {
                        Query primaryKeySubQuery = getCreator().create(type, new Filter() {
                            @Override
                            public void filter(Root root, Predicates predicates) {
                                String alias = String.valueOf(resolveTableName(rootClass).charAt(0));
                                root.join(getAssociatedRootClassFieldNameByType(type, rootClass), alias, JoinMode.LEFT_JOIN);
                                predicates.add(Predicate.eq(String.valueOf(alias).concat(".").concat(resolveIdColumn(rootClass)),
                                        idColumn.getColumnValue()));
                            }
                        });
                        cache(entity.getClass().getName());
                        query(primaryKeySubQuery, type, entity, field);
                    } else if (isForeignKeyAssociationFetchAllowed(field, query, referenceTable)) {
                        // if field is referenced by other primary key, fetch item if necessary
                        if (column != null && column.getColumnValue() != null) {
                            Query associatedSubQuery = getCreator().create(column.getColumnType(), new Filter() {
                                @Override
                                public void filter(Root root, Predicates predicates) {
                                    String alias = String.valueOf(resolveTableName(column.getColumnType()).charAt(0));
                                    predicates.add(Predicate.eq(alias.concat(".").concat(resolveIdColumn(column.getColumnType())), column.getColumnValue()));
                                }
                            });
                            query(associatedSubQuery, column.getColumnType(), entity, field);
                        }
                    } else {
                        if (column != null && column.getColumnValue() != null
                                && column.getColumnType().isAssignableFrom(column.getColumnValue().getClass())) {
                            setFieldValue(field, entity, column.getColumnValue());
                        }
                    }
                } catch (NameResolveException e) {
                    throw new MappingException("Failed to map column name for entity's field", e);
                }
            }
            resultSet.setPopulatedEntity(entity);
        }
    }

    private String getAssociatedRootClassFieldNameByType(Class<?> clazz, Class<?> type) {
        for (Field field : clazz.getDeclaredFields()) {
            field.setAccessible(true);
            if (field.isAnnotationPresent(ManyToMany.class) && ReflectionUtils.getFieldType(field).isAssignableFrom(type)) {
                return field.getName();
            }
            if (field.isAnnotationPresent(OneToOne.class) && ReflectionUtils.getFieldType(field).isAssignableFrom(type)) {
                return field.getName();
            }
        }

        return null;
    }

    private static void setFieldValue(Field field, Object entity, Object result) {
        try {
            field.set(entity, result);
        } catch (IllegalAccessException e) {
            throw new MappingException("Could not set value: " + result + " to field: "
                    + field.getName(), e);
        }
    }

    private <E> E instantiate(Class<?> clazz) throws ConversionException {
        try { // on beans try using default constructor
            return (E) clazz.newInstance();
        } catch (Exception e) {
            throw new MappingException("Failed to initialize class: " + clazz.getName()
                    + ", missing default constructor", e);
        }
    }

    private <E> Collection<E> resultSetListToCollection(List<ResultSet> resultSets, Class<?> type) {
        List<E> elements = new ArrayList<>();
        for (ResultSet set : resultSets) {
            elements.add((E) set.getPopulatedEntity());
        }
        if (Set.class.isAssignableFrom(type)) {
            if (HashSet.class.isAssignableFrom(type)) {
                return new HashSet<>(elements);
            } else if (TreeSet.class.isAssignableFrom(type)) {
                return new TreeSet<>(elements);
            } else {
                throw new MappingException("Given type does not inherit: " + HashSet.class.getName() + " nor " + TreeSet.class.getName());
            }
        }
        if (List.class.isAssignableFrom(type)) {
            if (LinkedList.class.isAssignableFrom(type)) {
                return new LinkedList<>(elements);
            } else {
                return new ArrayList<>(elements);
            }
        }

        return null;
    }

    private static boolean isForeignKeyAssociationFetchAllowed(Field field, Query query, String table) {
        return (field.isAnnotationPresent(ManyToOne.class) && field.getAnnotation(ManyToOne.class).fetch() == Fetch.EAGER)
                || (field.isAnnotationPresent(ManyToOne.class) && checkQueryContainsTable(query, table))
                || (field.isAnnotationPresent(OneToOne.class) && field.getAnnotation(OneToOne.class).fetch() == Fetch.EAGER)
                    && StringUtils.isBlank(field.getAnnotation(OneToOne.class).mappedBy())
                || (field.isAnnotationPresent(OneToOne.class) && checkQueryContainsTable(query, table));
    }

    private static boolean isPrimaryKeyAssociationFetchAllowed(Field field, Query query, String table) {
        return ((field.isAnnotationPresent(ManyToMany.class) && field.getAnnotation(ManyToMany.class).fetch() == Fetch.EAGER))
                || (field.isAnnotationPresent(ManyToMany.class) && checkQueryContainsTable(query, table))
                || (field.isAnnotationPresent(OneToOne.class) && field.getAnnotation(OneToOne.class).fetch() == Fetch.EAGER
                    && !StringUtils.isBlank(field.getAnnotation(OneToOne.class).mappedBy()))
                || (field.isAnnotationPresent(OneToOne.class) && !StringUtils.isBlank(field.getAnnotation(OneToOne.class).mappedBy())
                    && checkQueryContainsTable(query, table));
    }

    private static boolean checkQueryContainsTable(Query query, String table) {
        return (!StringUtils.isBlank(table) && query.getSql().contains(table));
    }
}
