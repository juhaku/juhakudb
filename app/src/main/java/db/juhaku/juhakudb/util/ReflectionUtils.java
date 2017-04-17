package db.juhaku.juhakudb.util;

import android.util.Log;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Map;

import db.juhaku.juhakudb.annotation.Id;
import db.juhaku.juhakudb.annotation.Repository;

/**
 * Created by juha on 06/04/16.
 * <p>This class provides utilities for making reflection easier.</p>
 *
 * @author juha
 *
 * @since 1.0.2
 */
public class ReflectionUtils {

    /**
     * Method looks for specific {@link Field} from given clazz.
     * @param clazz Class<?> whose field is looked for.
     * @param name String name of the field to look for.
     * @return found {@link Field} otherwise null.
     *
     * @since 1.0.2
     */
    public static Field findField(Class<?> clazz, String name) {
        for (Field field : clazz.getDeclaredFields()) {
            field.setAccessible(true);
            if (field.getName().equals(name)) {
                return field;
            }
        }

        return null;
    }

    /**
     * Method searches field with annotation {@link Id} through declared fields of given class.
     * @param clazz instance of entity class to look for id field.
     * @return {@link Field} if found; otherwise null.
     *
     * @since 1.0.2
     */
    public static Field findIdField(Class<?> clazz) {
        for (Field field : clazz.getDeclaredFields()) {
            field.setAccessible(true);
            if (field.isAnnotationPresent(Id.class)) {
                return field;
            }
        }

        return null;
    }

    private static boolean isGeneric(Class<?> clazz) {
        return Collection.class.isAssignableFrom(clazz) || Map.class.isAssignableFrom(clazz);
    }

    /**
     * Returns field type of generic fields of given class. e.g. for List&lt;Type&gt; the Type is returned.
     * @param type instance of {@link Field}.
     * @return Class representing generic type of given field. If field does not have generic type
     * null will be returned.
     *
     * @since 1.0.2
     */
    public static final <T> Class<?> getGenericFieldType(T type) {
        if (type.getClass().isAssignableFrom(Field.class) && isGeneric(((Field) type).getType())) {
            Type resolveType = ((Field) type).getGenericType();
            Class<?> resolveClazz = (Class<?>) ((ParameterizedType) resolveType)
                    .getActualTypeArguments()[0];

            return resolveClazz;
        } else {
            return null;
        }
    }

    /**
     * Returns field type for given {@link Field}. If field is generic then generic type will be returned
     * if field type is object then the object will be returned.
     * @param type instance of {@link Field}.
     * @return Class representing type of the field.
     *
     * @since 1.0.2
     */
    public static final <T> Class<?> getFieldType(T type) {
        Class<?> resolveClass;
        if ((resolveClass = getGenericFieldType(type)) == null) {
            resolveClass = ((Field) type).getType();
        }

        return resolveClass;
    }

    /**
     * Get generic types of class. If class is not generic sub class of generic super class exception
     * will occur.
     * @param clazz Generic sub class of generic super class.
     * @return Class array instance of generic types of super class.
     *
     * @since 1.0.2
     */
    public static final <T> Class[] getClassGenericTypes(Class<T> clazz) {
        Type[] resolvedTypes =  ((ParameterizedType) clazz.getGenericSuperclass()).getActualTypeArguments();
        Class[] retVal = new Class[resolvedTypes.length];
        System.arraycopy(resolvedTypes, 0, retVal, 0, resolvedTypes.length);

        return retVal;
    }

    /**
     * Get generic type classes with by providing instance of {@link TypedClass}. This can be used to
     * query generic type without class being subclass of generic super class. See more details {@link TypedClass}
     * @param typedClass Typed class that provides sub classing for type that need to be returned.
     * @return Class array representing types of typed class.
     *
     * @since 1.0.2
     */
    public static final <T> Class[] getClassGenericTypes(TypedClass<T> typedClass) {
        return getClassGenericTypes(typedClass.getClass());
    }

    /**
     * Get value of field from given object.
     * @param type Object to query field value from.
     * @param field Field to query value from.
     * @return Value of given field in given object or null if exception occurs.
     *
     * @since 1.0.2
     */
    public static final <T> T getFieldValue(Object type, Field field) {
        try {
            field.setAccessible(true);
            return (T) field.get(type);
        } catch (IllegalAccessException e) {
            Log.e(ReflectionUtils.class.getName(), "Failed to access field: " + field.getName() + " " + field);
        }

        return null;
    }

    /**
     * Get field's value by searching field by given name.
     * @param name String name of the field whose value should be returned.
     * @param o Object to look for field's value from.
     * @return Value of the field.
     *
     * @since 1.2.0-SNAPSHOT
     */
    public static final <T> T getFieldValue(String name, Object o) {
        return getFieldValue(o, findField(o.getClass(), name));
    }

    /**
     * Helper method to retrieve id field's value from given object. Given object must be a database
     * entity with annotation {@link db.juhaku.juhakudb.annotation.Entity} and it must have id field
     * annotated with {@link Id}. If requirements are missing null will be returned.
     *
     * @param o Object instance of database entity to get id field's value from.
     * @return Value of id field.
     *
     * @since 1.2.0-SNAPSHOT
     */
    public static final <T> T getIdFieldValue(Object o) {
        return getFieldValue(o, findIdField(o.getClass()));
    }

    /**
     * Search field from given object by the provided name and set the given value to that object.
     * Any error that occurs during execution will be logged.
     *
     * @param name String name of the field to look for.
     * @param o {@link Object} whose field will be modified with given value.
     * @param value T type object that will be placed to the field of given object.
     *
     * @since 1.2.0-SNAPSHOT
     */
    public static final <T> void setFieldValue(String name, Object o, T value) {
        Field field = findField(o.getClass(), name);

        if (field != null) {
            setFieldValue(field, o, value);
        }
    }

    /**
     * Sets given value to the provided {@link Field}. Field must be field of given object.
     * Any error that occurs during execution will be logged.
     *
     * @param field {@link Field} of object that will be modified.
     * @param o {@link Object} whose field will be modified with given value.
     * @param value T type object that will be placed to the field of given object.
     *
     * @since 1.2.0-SNAPSHOT
     */
    public static final <T> void setFieldValue(Field field, Object o, T value) {
        try {
            field.set(o, value);
        } catch (IllegalAccessException e) {
            Log.e(ReflectionUtils.class.getName(), "Failed to set value: " + value + " to object: " + o, e);
        }
    }
}
