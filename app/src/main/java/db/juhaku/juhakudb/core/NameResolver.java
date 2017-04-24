package db.juhaku.juhakudb.core;

import java.lang.reflect.Field;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;

import db.juhaku.juhakudb.exception.NameResolveException;
import db.juhaku.juhakudb.util.StringUtils;

/**
 * Created by juha on 16/12/15.
 *<p>Database item name resolver class. This class resolves names for database tables and columns
 * by providing entity's class or fields class for resolve name method.</p>
 *
 * @author Juha Kukkonen
 *
 * @since 1.0.2
 */
public class NameResolver {

    /**
     * Suffix that is generally used with id fields.
     */
    public static final String ID_FIELD_SUFFIX = "_id";

    /**
     * Resolves database item name for given type. E.g. by providing class of database entity method
     * will resolve database table name. And by giving {@link Field} of entity the database table's
     * column name will be determined by given field.
     *
     * @param type T type to provide for name resolving.
     * @return String value representing resolved name.
     * @throws NameResolveException if name is not resolvable. Exception should be caught on calling
     * class.
     *
     * @since 1.0.2
     */
    public static final <T> String resolveName(T type) throws NameResolveException {
        if (type.getClass().isAssignableFrom(Field.class)) {
            return resolveFieldName((Field) type);
        } else {
            return resolveTableName((Class<?>) type);
        }
    }

    /**
     * Resolves id field's name for class of Entity.
     * @param type Class<?> to provide for id field resolving.
     * @return Returns found name.
     * @throws NameResolveException if name is not found, or required annotations is not found.
     *
     * @since 1.0.2
     */
    public static final String resolveIdName(Class<?> type) throws NameResolveException {
        if (type.isAnnotationPresent(Entity.class)) {
            for (Field field : type.getDeclaredFields()) {
                field.setAccessible(true);
                if (field.isAnnotationPresent(Id.class)) {

                    return ID_FIELD_SUFFIX;
                }
            }
        }

        throw new NameResolveException("Annotation (" + Entity.class.getName() + ", "
                + Id.class.getName() + " and " + Column.class.getName()
                + " not found, or name was empty in " + Column.class.getName() + " annotation");
    }

    private static String resolveTableName(Class<?> clazz) throws NameResolveException {
        if (clazz.isAnnotationPresent(Entity.class)) {

            return camelCaseToUnderscored(clazz.getSimpleName());

        } else {
            throw new NameResolveException("Annotation (" + Entity.class.getName() + ") is not " +
                    "provided, cannot resolve name");
        }
    }

    private static String resolveFieldName(Field clazz) throws NameResolveException {
        if (clazz.isAnnotationPresent(Column.class)) {
            String columnName = clazz.getAnnotation(Column.class).name();
            if (!StringUtils.isBlank(columnName)) {
                return columnName;
            } else {
                throw new NameResolveException("name attribute not specified in " +
                        Column.class.getName() + " annotation");
            }
        } else if (clazz.isAnnotationPresent(Id.class)) {
            return ID_FIELD_SUFFIX;
        } else if (clazz.isAnnotationPresent(ManyToMany.class) || clazz.isAnnotationPresent(ManyToOne.class)
                || clazz.isAnnotationPresent(OneToOne.class) || clazz.isAnnotationPresent(OneToMany.class)) {
            return resolveJoinColumnName(clazz);
        } else {

            // Resolve name by the fields name.
            return camelCaseToUnderscored(clazz.getName());
        }
    }

    private static <T> String resolveJoinColumnName(T type) throws NameResolveException {
        return camelCaseToUnderscored(((Field) type).getName()).concat(ID_FIELD_SUFFIX);
    }

    /**
     * Transforms camelCaseText to underscored format e.g. camelCaseText would look like camel_case_text.
     * @param fieldName String field name to transform to underscored format.
     * @return String formatted to underscored format.
     *
     * @since 1.2.0
     *
     * @hide
     */
    private static String camelCaseToUnderscored(String fieldName) {
        StringBuilder underscored = new StringBuilder();
        for (int i = 0; i < fieldName.length(); i ++) {
            char letter = fieldName.charAt(i);

            // Lower the first letter
            if (i == 0 && Character.isUpperCase(letter)) {
                letter = Character.toLowerCase(letter);
            }

            if (Character.isUpperCase(letter) && i > 0) {
                underscored.append("_").append(Character.toLowerCase(letter));

            } else {
                underscored.append(letter);
            }
        }

        return underscored.toString();
    }

    /**
     * Transforms given field name to camelCase format. e.g. user_name would become userName.
     * <p>This is useful when field name is being validated against field in entity class.</p>
     *
     * @param fieldName String field name to transform.
     * @return String transformed field name.
     *
     * @since 1.2.0
     */
    public static String underscoredToCamelCase(String fieldName) {
        /*
         * If fields is not primary key then remove the _id suffix from the field as it is
         * computed anyway thus not really part of field name.
         */
        if (fieldName.length() > 3) {
            fieldName = fieldName.substring(0, fieldName.lastIndexOf(ID_FIELD_SUFFIX));
        }

        StringBuilder camelCased = new StringBuilder();
        for (int i = 0; i < fieldName.length(); i ++) {
            char letter = fieldName.charAt(i);

            // For "_" underscores fasten the cycle and uppercase next character for "_".
            if (letter == '_') {
                i++;
                camelCased.append(Character.toUpperCase(fieldName.charAt(i)));

            } else {
                camelCased.append(letter);
            }
        }

        return camelCased.toString();
    }
}
