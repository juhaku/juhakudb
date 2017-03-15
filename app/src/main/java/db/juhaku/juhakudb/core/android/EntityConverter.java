package db.juhaku.juhakudb.core.android;

import android.content.ContentValues;
import android.database.Cursor;
import android.util.Log;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import db.juhaku.juhakudb.annotation.Entity;
import db.juhaku.juhakudb.annotation.ManyToMany;
import db.juhaku.juhakudb.annotation.ManyToOne;
import db.juhaku.juhakudb.annotation.OneToMany;
import db.juhaku.juhakudb.annotation.OneToOne;
import db.juhaku.juhakudb.core.NameResolver;
import db.juhaku.juhakudb.exception.ConversionException;
import db.juhaku.juhakudb.exception.NameResolveException;
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

    private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

    public List<ResultSet> cursorToResultSetList(Cursor cursor, Class<?> rootClass, boolean custom) throws ConversionException {
        List<ResultSet> retVal = new ArrayList<>();
//        for (String col : cursor.getColumnNames()) { //TODO debug log, remove this
//            Log.d(getClass().getName(), "column:" + col);
//        }
        while (cursor.moveToNext()) {
            if (custom) {
                retVal.add(cursorToCustomResultSet(cursor));
            } else {
                retVal.add(cursorToResultSet(cursor, rootClass));
            }
        }
        cursor.close();

        return retVal;
    }

    public ResultSet cursorToResultSet(Cursor cursor, Class<?> rootClass) throws ConversionException {
        ResultSet result = new ResultSet();
        for (Field field : rootClass.getDeclaredFields()) {
            field.setAccessible(true);
            // ignore primary key associations
            if (hasPrimaryKeyAssociation(field)) {
                continue;
            }
            try {
                Class<?> type = ReflectionUtils.getFieldType(field);
                String columnName = NameResolver.resolveName(field);
                Object value = getColumnValue(cursor, columnName, type);
                result.add(type, columnName, value, field.getName());
            } catch (Exception e) {
                throw new ConversionException("Could not convert class: " + rootClass.getName() + " to result set", e);
            }
        }

        return result;
    }

    public ResultSet cursorToCustomResultSet(Cursor cursor) throws ConversionException {
        ResultSet resultSet = new ResultSet();
        int cols = cursor.getColumnCount();
        for (int i = 0; i < cols; i++) {
            String name = cursor.getColumnName(i);
            resultSet.add(null, name, getColumnValue(cursor, name, null), null);
        }

        return resultSet;
    }

    /*
     * Get column value as given class.
     */
    private <T> T getColumnValue(Cursor cursor, String columnName, Class<?> clazz) {
        int index = cursor.getColumnIndex(columnName);
        if (index < 0 || cursor.getCount() == 0) {
            return null;
        }
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
                    return (T) sdf.parse(val);
                } catch (ParseException e) {
                    Log.w(getClass().getName(), "incompatible date: " + val + " returning null");
                    return null;
                }
            } else if (clazz != null && Enum.class.isAssignableFrom(clazz)) {
                Class<? extends Enum> enVal = (Class<? extends Enum>) clazz;
                return (T) Enum.valueOf(enVal, val);
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
            // ignore primary key associations
            if (hasPrimaryKeyAssociation(field)) {
                continue;
            }
            try {
                String columnName;
                if (field.isAnnotationPresent(ManyToOne.class)
                        || (field.isAnnotationPresent(OneToOne.class) && StringUtils.isBlank(field.getAnnotation(OneToOne.class).mappedBy()))) {
                    columnName = NameResolver.resolveName(ReflectionUtils.getFieldType(field)).concat(NameResolver.ID_FIELD_SUFFIX);
                    Object value = getIdFieldValue(object, field);
                    // id is either long or integer
                    if (value != null) {
                        if (Long.class.isAssignableFrom(value.getClass()) || Long.TYPE.isAssignableFrom(value.getClass())) {
                            values.put(columnName, (Long) value);
                        } else {
                            values.put(columnName, (Integer) value);
                        }
                    }
                    continue;
                } else {
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
                } else if (Byte.class.isAssignableFrom(field.getType()) || Byte.TYPE.isAssignableFrom(field.getType())) {
                    values.put(columnName, (Byte) field.get(object));
                } else if (byte[].class.isAssignableFrom(field.getType())) {
                    values.put(columnName, (byte[]) field.get(object));
                } else if (String.class.isAssignableFrom(field.getType())) {
                    values.put(columnName, (String) field.get(object));
                }
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
    private Object getIdFieldValue(Object object, Field item) {
        Object val = ReflectionUtils.getFieldValue(object, item);
        if (val != null) {
            Field idField = ReflectionUtils.findIdField(val.getClass());

            return ReflectionUtils.getFieldValue(val, idField);
        } else {
            return null;
        }
    }

    /**
     * Check does given field of Class have a primary key referenced association.
     * @param field {@link Field} of class to check primary key association for.
     * @return Returns boolean value; true if field has primary key association; false otherwise.
     *
     * @since 1.0.2
     *
     * @hide
     */
    private static boolean hasPrimaryKeyAssociation(Field field) {
        return field.isAnnotationPresent(ManyToMany.class) || field.isAnnotationPresent(OneToMany.class)
                || (field.isAnnotationPresent(OneToOne.class) && !StringUtils.isBlank(field.getAnnotation(OneToOne.class).mappedBy()));
    }
}
