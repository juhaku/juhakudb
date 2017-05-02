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
package db.juhaku.juhakudb.core.android;

import android.content.ContentValues;
import android.database.Cursor;
import android.util.Log;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;

import javax.persistence.Entity;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Transient;

import db.juhaku.juhakudb.core.NameResolver;
import db.juhaku.juhakudb.exception.ConversionException;
import db.juhaku.juhakudb.exception.NameResolveException;
import db.juhaku.juhakudb.filter.Root;
import db.juhaku.juhakudb.filter.Root.Join;
import db.juhaku.juhakudb.util.ReflectionUtils;
import db.juhaku.juhakudb.util.StringUtils;

/**
 * Created by juha on 13/04/16.
 *<p>This class provides conversion between entities and database tables.</p>
 * @author juha
 *
 * @since 1.0.2
 */
public class EntityConverter {

    private static AtomicInteger index = new AtomicInteger();

    /**
     * Convert cursor of SQL query result to list of entities. Root stands for root of SQL
     * query containing joins to other tables if defined.
     *
     * <p>Entities are converted distinctly for root entity.</p>
     *
     * @param cursor {@link Cursor} containing SQL query result.
     * @param root {@link Root} of SQL query.
     * @return List of converted entities from cursor's returned rows.
     *
     * @throws ConversionException if any exception occurs during conversion.
     *
     * @since 1.2.0
     */
    public <T> List<T> convertCursorToEntityList(Cursor cursor, Root<?> root) throws ConversionException {
        List<T> entities = new ArrayList<>();

        while (cursor.moveToNext()) {
            // convert the main object first.
            T entity = convertCursorToEntity(cursor, root.getModel());
            T foundEntity = findEntityById(ReflectionUtils.getIdFieldValue(entity), entities);

            /*
             * If entity is not found add it to the list, otherwise do not add new one. Transform
             * distinctly for root entity.
             */
            if (foundEntity == null) {
                entities.add(entity);
            }

            // convert joins from this model class.
            alterEntityConvertJoins(cursor, root, foundEntity == null ? entity : foundEntity);

            // reset index.
            index.set(0);
        }

        return entities;
    }

    /**
     * Find existing entity from entities list by id of the entity.
     *
     * @param id Object id value of the entity to look for existing entity.
     * @param entities Collection of entities to look for entity.
     * @return Found entity or null if was not found.
     *
     * @since 1.2.0
     *
     * @hide
     */
    private static <T> T findEntityById(Object id, Collection<T> entities) {
        if (id != null) {
            for (T entity : entities) {
                if (id.equals(ReflectionUtils.getIdFieldValue(entity))) {

                    return entity;
                }
            }
        }

        return null;
    }

    /**
     * Alter entity's field with fetch join values from root. If root contains fetch joins they are
     * converted and placed to entity's corresponding field.
     *
     * @param cursor {@link Cursor}'s row to be converted to entity and placed to entity's field.
     * @param root {@link Root} of joins that are going to be altered to the entity.
     * @param entity {@link Object} that is being altered with conversion objects.
     *
     * @since 1.2.0
     *
     * @hide
     */
    private <T> void alterEntityConvertJoins(Cursor cursor, Root<?> root, T entity) {
        for (Root r : root.getJoins()) {
            Join join = (Join) r;

            if (join.isFetch()) {

                // convert field entity and add it to the object.
                T fieldEntity = convertCursorToEntity(cursor, join.getModel());

                Field targetField = ReflectionUtils.findField(root.getModel(), join.getTarget());

                // Take the id of the converted entity
                Object id = ReflectionUtils.getIdFieldValue(fieldEntity);

                //TODO support maps?

                /*
                 * Determine whether field is a collection or object and act accordingly.
                 * Update the collection fields and replace other values.
                 *
                 * This is because collections must be gradually altered since each row from database
                 * result has its own value to collection.
                 */
                if (Collection.class.isAssignableFrom(targetField.getType())) {
                    Collection value = ReflectionUtils.getFieldValue(entity, targetField);

                    if (value == null) {
                        if (List.class.isAssignableFrom(targetField.getType())) {
                            value = instantiateByDefaultConstructor(ArrayList.class);

                        } else if (Set.class.isAssignableFrom(targetField.getType())) {
                            value= instantiateByDefaultConstructor(TreeSet.class);

                        } else {

                            value = instantiateByDefaultConstructor(targetField.getType());
                        }

                        ReflectionUtils.setFieldValue(targetField, entity, value);
                    }

                    /*
                     * Do found checking only for entities, other typed values does not have joins
                     * forward so they can be safely added to the collection.
                     */
                    if (fieldEntity.getClass().isAnnotationPresent(Entity.class)) {

                        // If converted entity is not found from the collection add it.

                        /*
                         * If id of the entity is not null add the entity otherwise skip it as it is
                         * an empty row from database caused by fetch join.
                         */
                        if (findEntityById(id, value) == null && id != null) {
                            value.add(fieldEntity);
                        }

                    } else {
                        value.add(fieldEntity);
                    }

                } else {
                    /*
                     * If id of the entity is not null add the entity otherwise skip it as it is
                     * an empty row from database caused by fetch join.
                     */
                    if (id != null) {
                        ReflectionUtils.setFieldValue(targetField, entity, fieldEntity);
                    }
                }

                // If join has joins to even further convert them as well.
                if (!join.getJoins().isEmpty()) {
                    Object parentEntity = ReflectionUtils.getFieldValue(join.getTarget(), entity);

                    if (Collection.class.isAssignableFrom(targetField.getType())) {

                        // Find the actual parent from the collection.
                        Object parent = findEntityById(ReflectionUtils.getIdFieldValue(fieldEntity), (Collection<Object>) parentEntity);

                        alterEntityConvertJoins(cursor, join, parent == null ? fieldEntity : parent);
                    } else {

                        alterEntityConvertJoins(cursor, join, parentEntity == null ? fieldEntity : parentEntity);
                    }
                }
            }
        }
    }

    /**
     * Convert's cursor's row to entity of model class provided.
     *
     * @param cursor {@link Cursor}'s row to be converted to entity of model class.
     * @param model {@link Class} of database model where to convert cursor's row.
     * @return Fully converted entity from cursor's row.
     *
     * @since 1.2.0
     *
     * @hide
     */
    private <T> T convertCursorToEntity(Cursor cursor, Class<?> model) {
        // instantiate model
        Object entity = instantiateByDefaultConstructor(model);

        String[] names = cursor.getColumnNames();
        int entityIndex = index.get();
        int fieldCount = countDeclaredFields(model);

        for (Field field : model.getDeclaredFields()) {
            boolean accessible = field.isAccessible();
            field.setAccessible(true);

            /*
             * Ignore fields that has primary key as reverse join because there is no such column
             * in database thus nothing to do.
             */
            if (hasPrimaryKeyJoin(field)) {
                continue;
            }

            String name = resolveName(field);

            // Double check that column name in index matches to the required column name
            int fieldIndex = getColumnIndex(entityIndex, fieldCount, names, name);

//            if (!name.equals(names[index.get()])) {
//                throw new ConversionException("Field's name and indexed table column name does not match: " + name + " <> " + names[index.get()]);
//            }

            Class<?> type = ReflectionUtils.getFieldType(field);
            if (type.isAnnotationPresent(Entity.class)) {

                /*
                 * For entities get id fields type in order to obtain correct id value.
                 */
                Field idField = ReflectionUtils.findIdField(type);
                Object value = getColumnValue(cursor, ReflectionUtils.getFieldType(idField), fieldIndex);

                // Add entity with value to the mapping entity if value is found from database query.
                if (value != null) {
                    // Instantiate new entity
                    Object fieldEntity = instantiateByDefaultConstructor(type);

                    ReflectionUtils.setFieldValue(ReflectionUtils.findIdField(fieldEntity.getClass()), fieldEntity, value);
                    ReflectionUtils.setFieldValue(field.getName(), entity, fieldEntity);
                }

            } else {
                // Get the value and add a new resource to result set.
                Object value = getColumnValue(cursor, type, fieldIndex);

                ReflectionUtils.setFieldValue(field.getName(), entity, value);

            }
            index.incrementAndGet();

            field.setAccessible(accessible); // restore original status
        }

        return (T) entity;
    }

    /**
     * Find the index of the column for the name. This is mandatory as Java cannot assure the order
     * of the declared fields in objects. Especially dalvik cannot provide them in order.
     *
     * <p>Name is searched from array of names with scope of index to index + fieldCount.</p>
     *
     * @param index Int value of current index of columns for entity.
     * @param fieldCount Int value of declared fields in entity.
     * @param names String array of names in total.
     * @param name String value of name that should be from array.
     * @return Int index of the column in array.
     * @throws ConversionException If field is not found from array of names within scope.
     *
     * @since 2.0.1
     *
     * @hide
     */
    private static int getColumnIndex(int index, int fieldCount, String[] names, String name) throws ConversionException {
        if ((index + fieldCount) <= names.length) {

            for (int i = index; i < index + fieldCount; i++) {

                if (names[i].equals(name)) {
                    return i;
                }
            }
        }

        throw new ConversionException("Name: " + name + " could not be found from names: " + StringUtils.arrayToString(names)
                + " with scope from: " + index + " to: " + (index + fieldCount));
    }

    /**
     * Resolves types name silently. If error will occur during name resolving error will be thrown
     * silently. This is preferred behaviour since processing should not be allowed further if
     * resolving fails for reason.
     *
     * @param type T type either {@link Class} of database entity or {@link Field} of entity class.
     * @return String value of resolved name.
     *
     * @since 1.2.0
     *
     * @hide
     */
    private static <T> String resolveName(T type) {
        try {
            return NameResolver.resolveName(type);
        } catch (NameResolveException e) {
            throw new ConversionException("Failed to resolve type's name", e);
        }
    }

    /**
     * Initialize new instance of class. Class must have default constructor available.
     * If no default constructor is provided initialization will fail and conversion will stop.
     *
     * @param type {@link Class} to instantiate.
     * @return Newly created instance of provided class.
     *
     * @since 1.2.0
     *
     * @hide
     */
    public static  <T> T instantiateByDefaultConstructor(Class<?> type) {
        T model = ReflectionUtils.instantiateByDefaultConstructor(type);

        if (model == null) {
            throw new ConversionException("Failed to initialize type: " + type.getName()
                    + ", missing default constructor");
        }

        return model;
    }

    public List<ResultSet> convertCursorToCustomResultSetList(Cursor cursor) throws ConversionException {
        List<ResultSet> retVal = new ArrayList<>();

        while (cursor.moveToNext()) {
            retVal.add(cursorToCustomResultSet(cursor));
        }
        cursor.close();

        return retVal;
    }

    public ResultSet cursorToCustomResultSet(Cursor cursor) throws ConversionException {
        ResultSet resultSet = new ResultSet();
        int cols = cursor.getColumnCount();
        for (int i = 0; i < cols; i++) {
            String name = cursor.getColumnName(i);
            resultSet.add(null, name, getColumnValue(cursor, null, i), null);
        }

        return resultSet;
    }

    /*
     * Get column value as given class.
     */
    private <T> T getColumnValue(Cursor cursor, Class<?> clazz, int index) {
//        int index = cursor.getColumnIndex(columnName);
//        if (index < 0 || cursor.getCount() == 0) {
//            return null;
//        }
        int type = cursor.getType(index);

        if (type == Cursor.FIELD_TYPE_FLOAT) {

            Float val = cursor.getFloat(index);
            if (clazz == null) {
                return (T) val;
            } else {
                return (T) constructType(clazz, val.toString());
            }

        } else if (type == Cursor.FIELD_TYPE_INTEGER) {

            Integer val = cursor.getInt(index);
            if (clazz == null) {
                return (T) val;
            } else if (Boolean.class.isAssignableFrom(clazz) || Boolean.TYPE.isAssignableFrom(clazz)) {
                return (T) (val == 0 ? Boolean.FALSE : Boolean.TRUE);
            } else {
                return (T) constructType(clazz, val.toString());
            }

        } else if (type == Cursor.FIELD_TYPE_BLOB) {

            return (T) cursor.getBlob(index);

        } else {

            String val = cursor.getString(index);

            if (clazz != null && Date.class.isAssignableFrom(clazz)) {
                try {

                    if (val != null) {
                        return (T) new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").parse(val);
                    } else {
                        return null;
                    }

                } catch (ParseException e) {
                    Log.w(getClass().getName(), "incompatible date: " + val + " returning null");
                    return null;
                }

            } else if (clazz != null && Enum.class.isAssignableFrom(clazz)) {

                Class<? extends Enum> enVal = (Class<? extends Enum>) clazz;
                if (val != null) {

                    return (T) Enum.valueOf(enVal, val);

                } else {

                    return null;
                }

            } else {

                return (T) val;

            }
        }
    }

    /*
     * Constructs value and return as given class.
     */
    private <T> T constructType(Class<?> clazz, T value) {
        try {
            if (clazz.isAnnotationPresent(Entity.class)) {
                clazz = ReflectionUtils.getFieldType(ReflectionUtils.findIdField(clazz));
            }
            Constructor<?> constructor = null;
            for (Constructor c : clazz.getDeclaredConstructors()) {
                if (c.getParameterTypes().length == 1 && String.class.isAssignableFrom(c.getParameterTypes()[0])) {
                    constructor = c;
                    break;
                }
            }
            if (constructor != null) {

                return (T) constructor.newInstance(value.toString());
            } else {
                Log.w(getClass().getName(), "incompatible type: " + value.getClass() + " with value: " + value + " for class: "
                        + clazz + ", returning null");

                return null;
            }
        } catch (Exception e) {
            throw new ConversionException("Failed to construct type: " + value.getClass() + " with value: " + value
                    + " for class: " + clazz, e);
        }
    }

    /**
     * Converts entity to content values. {@link ContentValues} is used when entity is stored to
     * database. And in the content values is put column name and and value which will be stored.
     *
     * @param object The entity that is being stored to database.
     * @return instance of ContentValues that are used to store entity to database.
     * @throws ConversionException if any exception occurs during conversion.
     *
     * @since 1.0.2
     */
    public ContentValues entityToContentValues(Object object) throws ConversionException {
        if (!object.getClass().isAnnotationPresent(Entity.class)) {
            throw new ConversionException(Entity.class.getName() + " annotation is missing, are you sure you provided entity?");
        }

        ContentValues values = new ContentValues();

        for (Field field : object.getClass().getDeclaredFields()) {
            field.setAccessible(true);

            // Ignore primary key joins as they do not have column in the current object's table.
            if (hasPrimaryKeyJoin(field)) {
                continue;
            }

            try {
                String columnName;

                /*
                 * If field has foreign key relation to another table and this field has a value add the foreign key
                 * value the the content values.
                 */
                if (field.isAnnotationPresent(ManyToOne.class)
                        || (field.isAnnotationPresent(OneToOne.class) && StringUtils.isBlank(field.getAnnotation(OneToOne.class).mappedBy()))) {
                    columnName = NameResolver.resolveName(field);

                    Object value = getIdFieldValue(object, field);

                    // Id is either long or integer.
                    if (value != null) {
                        if (Long.class.isAssignableFrom(value.getClass()) || Long.TYPE.isAssignableFrom(value.getClass())) {
                            values.put(columnName, (Long) value);
                        } else {
                            values.put(columnName, (Integer) value);
                        }
                    }
                    continue;

                } else {
                    // Resolve regular column name of the current object.
                    columnName = NameResolver.resolveName(field);
                }

                if (Integer.class.isAssignableFrom(field.getType()) || Integer.TYPE.isAssignableFrom(field.getType())) {
                    values.put(columnName, (Integer) field.get(object));

                } else if (Short.class.isAssignableFrom(field.getType()) || Short.TYPE.isAssignableFrom(field.getType())) {
                    values.put(columnName, (Short) field.get(object));

                } else if (Boolean.class.isAssignableFrom(field.getType()) || Boolean.TYPE.isAssignableFrom(field.getType())) {
                    values.put(columnName, (Boolean) field.get(object));

                } else if (Long.class.isAssignableFrom(field.getType()) || Long.TYPE.isAssignableFrom(field.getType())) {
                    values.put(columnName, (Long) field.get(object));

                } else if (Float.class.isAssignableFrom(field.getType()) || Float.TYPE.isAssignableFrom(field.getType())) {
                    values.put(columnName, (Float) field.get(object));

                } else if (Double.class.isAssignableFrom(field.getType()) || Double.TYPE.isAssignableFrom(field.getType())) {
                    values.put(columnName, (Double) field.get(object));

                } else if (Byte.class.isAssignableFrom(field.getType()) || Byte.TYPE.isAssignableFrom(field.getType())) {
                    values.put(columnName, (Byte) field.get(object));

                } else if (byte[].class.isAssignableFrom(field.getType())) {
                    values.put(columnName, (byte[]) field.get(object));

                } else if (String.class.isAssignableFrom(field.getType())) {
                    values.put(columnName, (String) field.get(object));

                } else if (Date.class.isAssignableFrom(field.getType())) {
                    Object value = field.get(object);
                    values.put(columnName, value != null ? new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(value) : null);

                } else if (Enum.class.isAssignableFrom(field.getType())) {
                    Object value = field.get(object);
                    values.put(columnName, value != null ? value.toString() : null);
                }

            } catch (RuntimeException e) {
                /*
                 * Catch runtime exceptions so that they are not left behind the scene and re-throw them.
                 */
                throw e;
            } catch (Exception e) {
                throw new ConversionException("Failed to convert entity to content values", e);
            }
        }

        return values;
    }

    /**
     * Get id fields value of associated entity behind given field in object.
     * @param object Object to query fields value from.
     * @param item Field that has type of database entity whose id field values is returned.
     * @return Object the value of id field. Typically is either Long or Integer.
     *
     * @since 1.0.2
     *
     * @hide
     */
    private static Object getIdFieldValue(Object object, Field item) {
        Object val = ReflectionUtils.getFieldValue(object, item);

        if (val != null) {

            return ReflectionUtils.getIdFieldValue(val);
        } else {

            return null;
        }
    }

    /**
     * Check is given field annotated with one of {@link ManyToMany}, {@link OneToMany} or {@link OneToOne}
     * with mapped by value empty.
     *
     * <p>If field is annotated with one of these annotations the join to other table is made from
     * primary key of field's owning class. This means that field with annotation {@link javax.persistence.Id}
     * is being used to make the join.</p>
     *
     * @param field {@link Field} of class to check primary key join for.
     * @return Returns boolean value; true if field has primary key join; false otherwise.
     *
     * @since 1.0.2
     *
     * @hide
     */
    private static boolean hasPrimaryKeyJoin(Field field) {
        return field.isAnnotationPresent(ManyToMany.class) || field.isAnnotationPresent(OneToMany.class)
                || (field.isAnnotationPresent(OneToOne.class) && !StringUtils.isBlank(field.getAnnotation(OneToOne.class).mappedBy()));
    }

    /**
     * Get's count of declared fields in in given class.
     *
     * @param type Instance of {@link Class} type of object which declared fields will be counted.
     *
     * @return count of declared fields or 0 if class does not have declared fields.
     *
     * @since 2.0.1
     *
     * @hide
     */
    private static final int countDeclaredFields(Class<?> type) {
        int count = 0;
        for (Field field : type.getDeclaredFields()) {
            field.setAccessible(true);

            if (field.isAnnotationPresent(Transient.class) || hasPrimaryKeyJoin(field)) {
                continue;
            }

            count++;

            field.setAccessible(false); // return the normal state
        }

        return count;
    }
}
