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
package db.juhaku.juhakudb.util;

import android.util.Log;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Map;

import javax.persistence.Id;
import javax.persistence.Transient;

import db.juhaku.juhakudb.exception.ConversionException;

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
     * Method looks for specific {@link Field} from given clazz all the way up to the {@link Object} class.
     *
     * @param clazz Class<?> whose field is looked for.
     * @param name String name of the field to look for.
     * @return found {@link Field} otherwise null.
     *
     * @since 1.0.2
     */
    public static Field findField(Class<?> clazz, String name) {
        while (!clazz.isAssignableFrom(Object.class)) {
            for (Field field : clazz.getDeclaredFields()) {
                field.setAccessible(true);
                if (field.getName().equals(name)) {
                    return field;
                }
            }
            clazz = clazz.getSuperclass();
        }

        return null;
    }

    /**
     * Method searches field with annotation {@link Id} through declared fields of given class.
     *
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
     *
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
     * Gets array of all generic types from all super interfaces for the given interface class.
     * @param interf {@link Class} of which super interface's generic types will be returned.
     *
     * @return {@link Class} array containing all the generic types from super interfaces.
     *
     * @since 1.3.0
     */
    public static final <T> Class[] getInterfaceGenericTypes(Class<T> interf) {
        Type[] resolvedTypes = interf.getGenericInterfaces();
        int length = 0;
        for (Type type : resolvedTypes) {
            Type[] types = ((ParameterizedType) type).getActualTypeArguments();

            length = length + types.length;
        }

        Class[] retVal = new Class[length];

        int copied = 0;
        for (Type type : resolvedTypes) {
            Type[] types = ((ParameterizedType) type).getActualTypeArguments();

            System.arraycopy(types, 0, retVal, copied, types.length);
            copied = types.length - 1;
        }

        return retVal;
    }

    /**
     * Get generic type classes with by providing instance of {@link TypedClass}. This can be used to
     * query generic type without class being subclass of generic super class. See more details {@link TypedClass}
     *
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
     *
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
     *
     * @param name String name of the field whose value should be returned.
     * @param o Object to look for field's value from.
     * @return Value of the field or null if field is not found.
     *
     * @since 1.2.0
     */
    public static final <T> T getFieldValue(String name, Object o) {
        Field field = findField(o.getClass(), name);

        if (field != null) {
            return getFieldValue(o, findField(o.getClass(), name));
        }

        return null;
    }

    /**
     * Helper method to retrieve id field's value from given object. Given object must be a database
     * entity with annotation {@link javax.persistence.Entity} and it must have id field
     * annotated with {@link Id}. If requirements are missing null will be returned.
     *
     * @param o Object instance of database entity to get id field's value from.
     * @return Value of id field.
     *
     * @since 1.2.0
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
     * @since 1.2.0
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
     * @since 1.2.0
     */
    public static final <T> void setFieldValue(Field field, Object o, T value) {
        try {
            field.set(o, value);
        } catch (IllegalAccessException e) {
            Log.e(ReflectionUtils.class.getName(), "Failed to set value: " + value + " to object: " + o, e);
        }
    }

    /**
     * Instantiate class via given constructor with given arguments.
     *
     * @param constructor {@link Constructor} to instantiate.
     * @param args Object array or arguments to pass to the constructor.
     * @return Instantiated constructor or null if error occurs during instantiation.
     *
     * @since 1.3.0
     */
    public static final <T> T instantiateConstructor(Constructor constructor, Object... args) {
        try {
            return (T) constructor.newInstance(args);
        } catch (InstantiationException | IllegalStateException | InvocationTargetException | IllegalAccessException e) {
            Log.e(ReflectionUtils.class.getName(), "Failed to initialize constructor: " + constructor + " with args: " + args, e);
        }

        return null;
    }

    /**
     * Instantiate given class with given objects by looking for most suitable constructor. If
     * constructor cannot be found null will be returned.
     *
     * @param clazz {@link Class} to instantiate by constructor that suits for given arguments.
     * @param args Object array or arguments to pass to the constructor.
     * @return Instantiated class or null if error occurs during instantiation.
     *
     * @since 1.3.0
     */
    public static final <T> T instantiateConstructor(Class<?> clazz, Object... args) {
        Constructor constructor = findConstructorByParams(clazz, args);

        if (constructor != null) {
            return instantiateConstructor(constructor, args);
        }

        return null;
    }

    /**
     * Find constructor from given class that has given args as parameter.
     *
     * @param clazz {@link Class} to to look for constructors from.
     * @param args Object array or arguments to pass to the constructor.
     * @return Found {@link Constructor} or null if no constructor with args was found.
     *
     * @since 1.3.0
     */
    public static final <T> T findConstructorByParams(Class<?> clazz, Object... args) {
        for (Constructor constructor : clazz.getDeclaredConstructors()) {

            // Get constructor args.
            Class[] params = constructor.getParameterTypes();

            boolean hasParams = true;
            // Check that length of parameters match in constructor.
            if (params.length == args.length) {
                for (int i = 0; i < params.length ; i ++) {

                    if (params[i].equals(args[i].getClass())) {
                        continue;
                    }

                    // If execution got here parameters did not match.
                    hasParams = false;
                    break;
                }
            }

            // If has params is still true we have correct constructor
            if (hasParams) {
                return (T) constructor;
            }
        }

        return null;
    }

    /**
     * Initialize new instance of class. Class must have default constructor available.
     *
     * @param type {@link Class} to instantiate.
     * @return Newly created instance of provided class or null if default constructor is not found.
     *
     * @since 2.0.1
     */
    public static final <T> T instantiateByDefaultConstructor(Class<?> type) {
        try {
            // on models try using default constructor
            return (T) type.newInstance();
        } catch (Exception e) {
            Log.e(ReflectionUtils.class.getName(), "Failed to initialize model by default constructor", e);

            return null;
        }
    }

}
