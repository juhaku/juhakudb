/**
MIT License

Copyright (c) 2018 juhaku

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
package db.juhaku.juhakudb.core.schema;

import android.util.Log;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.concurrent.atomic.AtomicInteger;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;

import db.juhaku.juhakudb.core.NameResolver;
import db.juhaku.juhakudb.exception.NameResolveException;
import db.juhaku.juhakudb.exception.SchemaInitializationException;
import db.juhaku.juhakudb.util.ReflectionUtils;
import db.juhaku.juhakudb.util.ReservedWords;
import db.juhaku.juhakudb.util.StringUtils;

/**
 * Created by juha on 7/4/15.
 * <p>Factory class that provides new database schemas. Call {@link #getSchema(String, Class[])}
 * to query new database schema for table name and with tables provided by class array.</p>
 *
 * @author Juha Kukkonen
 *
 * @since 1.0.2
 */
public class SchemaFactory {

    private AtomicInteger keys = new AtomicInteger();

    /**
     * Generates new instance of database schema {@link Schema} by given database name and class array
     * as tables of the database.
     *
     * @param dbName String value of database name.
     * @param tables Class[] of classes as tables in database.
     * @return Returns new instance of database {@link Schema} tree.
     * @throws SchemaInitializationException if any initialization exception occurs.
     *
     * @since 1.0.2
     */
    public Schema getSchema(String dbName, Class<?>[] tables) throws SchemaInitializationException {
        Schema schema = new Schema();
        schema.setName(dbName);
        for (Class<?> table : tables) {
            schema.addTable(createTable(table, schema, null));
        }

        return schema;
    }

    private Schema createTable(Class<?> table, Schema schema, String mappedBy) throws SchemaInitializationException {
        String tableName = resolveName(table);

        // check table name before it is created.
        checkReservedWords(tableName);

        Schema dbTable;
        if (schema.getElement(tableName) == null) {
            dbTable = new Schema();
            dbTable.setName(tableName);
            dbTable.setOrder(keys.incrementAndGet());
            for (Field column : table.getDeclaredFields()) {
                column.setAccessible(true);
                if (column.isAnnotationPresent(Transient.class)) {
                    continue;
                }
                if (column.isAnnotationPresent(ManyToMany.class)) {
                    createJoinTable(table, tableName, column, schema);
                    continue;
                } else if (column.isAnnotationPresent(ManyToOne.class)) {
                    Schema col = createColumn(column);
                    dbTable.addColumn(col);
                    try {
                        dbTable.getReferences().add(new Reference(col.getName(), column));
                    } catch (NameResolveException e) {
                        Log.e(getClass().getName(), "Failed to create schema", e);
                        throw new SchemaInitializationException("Schema initialization failed", e);
                    }
                    continue;
                } else if (column.isAnnotationPresent(OneToMany.class)) {
                    schema.addTable(createTable(ReflectionUtils.getFieldType(column), schema, null));
                    continue;
                } else if (column.isAnnotationPresent(OneToOne.class)) {
                    if (!StringUtils.isBlank(column.getAnnotation(OneToOne.class).mappedBy())) {
                        schema.addTable(createTable(column.getType(), schema, column.getAnnotation(OneToOne.class).mappedBy()));
                    }
                    continue;
                } else {
                    dbTable.addColumn(createColumn(column));
                }
            }

            // Add indexes and unique constraints.
            if (table.isAnnotationPresent(Table.class)) {
                addIndexesToTable(table.getAnnotation(Table.class), dbTable);
            }

        } else {
            dbTable = schema.getElement(tableName);
            dbTable.setOrder(keys.incrementAndGet());
        }
        if (!StringUtils.isBlank(mappedBy)) {
            Field referencedField = ReflectionUtils.findField(table, mappedBy);

            if (referencedField == null) {
                throw new SchemaInitializationException("Field not found by mappedBy value: " + mappedBy);
            }

            String referenceColumnName = resolveName(referencedField);

            Schema col = createNamedColumn(referenceColumnName, "INTEGER");
            dbTable.addColumn(col);

            try {
                dbTable.getReferences().add(new Reference(referenceColumnName, referencedField));
            } catch (NameResolveException e) {
                Log.e(getClass().getName(), "Failed to create schema", e);
                throw new SchemaInitializationException("Schema initialization failed", e);
            }
        }

        return dbTable;
    }

    private static void addIndexesToTable(Table tableDef, Schema dbTable) {
        for (UniqueConstraint ctx : tableDef.uniqueConstraints()) {
            dbTable.getConstraints().add(new Constraint(ctx.name(), true, dbTable.getName(), ctx.columnNames()));
        }

        for (Index index : tableDef.indexes()) {
            dbTable.getConstraints().add(new Constraint(index.name(), index.unique(), dbTable.getName(), index.columnList()));
        }
    }

    private Schema createColumn(Field column) throws SchemaInitializationException {
        Schema dbColumn = new Schema();
        String columnName = resolveName(column);

        // check column name before it is created.
        checkReservedWords(columnName);

        dbColumn.setName(columnName);
        dbColumn.setType(resolveType(column));
        if (column.isAnnotationPresent(Id.class)) {
            dbColumn.setExtensions("PRIMARY KEY");
        }

        return dbColumn;
    }

    /*
     * Join table is only for many to many relations.
     */
    private void createJoinTable(Class<?> table, String tableName, Field column, Schema schema) throws SchemaInitializationException {
        String referenceColName = resolveName(column);

        // Resolve real name for entity class of column.
        String referenceTableName = resolveName(ReflectionUtils.getFieldType(column));

        Schema joinTable = getJoinTable(schema, tableName, referenceTableName);
        if (joinTable != null) {
            joinTable.setOrder(keys.incrementAndGet());
            return;
        } else {
            joinTable = new Schema();
        }

        String joinTableName = new StringBuilder(tableName).append("_").append(referenceTableName).toString();
        joinTable.setName(joinTableName);
        joinTable.setOrder(keys.incrementAndGet());
        String type = "INTEGER";
        String firstJoinColName = tableName.concat(NameResolver.ID_FIELD_SUFFIX);

        joinTable.addColumn(createNamedColumn(firstJoinColName, type));
        joinTable.addColumn(createNamedColumn(referenceColName, type));

        String firstJoinIdTable = resolveIdColumn(table);

        String secondJoinIdTable = resolveIdColumn(ReflectionUtils.getGenericFieldType(column));

        joinTable.getReferences().add(new Reference(firstJoinColName, tableName, firstJoinIdTable));
        joinTable.getReferences().add(new Reference(referenceColName, referenceTableName, secondJoinIdTable));

        schema.addTable(joinTable);
    }

    /*
     * Avoid duplicates on many to many relation tables by fetching already existing table.
     */
    private static Schema getJoinTable(Schema schema, String tableName, String referenceColName) {
        String name = new StringBuilder(tableName).append("_").append(referenceColName).toString();
        Schema joinTable;
        if ((joinTable = schema.getElement(name)) == null) {
            name = new StringBuilder(referenceColName).append("_").append(tableName).toString();
            return schema.getElement(name);
        } else {
            return joinTable;
        }
    }

    /*
     * Named column is used for references.
     */
    private Schema createNamedColumn(String name, String type) {
        Schema namedColumn = new Schema();
        namedColumn.setName(name);
        namedColumn.setType(type);

        return namedColumn;
    }

    /*
     * Resolve type of column for database tables.
     */
    private static String resolveType(Field field) {
        Class<?> type = field.getType();
        if (type.isAssignableFrom(Integer.class) || type.isAssignableFrom(int.class)
                || type.isAssignableFrom(Short.class) || type.isAssignableFrom(short.class)
                || type.isAssignableFrom(Long.class) || type.isAssignableFrom(long.class)
                || type.isAssignableFrom(Byte.class) || type.isAssignableFrom(byte.class)
                || type.isAssignableFrom(BigInteger.class) || type.isAnnotationPresent(Entity.class)) {

            return "INTEGER";
        } else if (type.isAssignableFrom(Double.class) || type.isAssignableFrom(double.class)
                || type.isAssignableFrom(Float.class) || type.isAssignableFrom(float.class)) {

            return "REAL";
        } else if (type.isAssignableFrom(BigDecimal.class) || type.isAssignableFrom(Boolean.class)
                || type.isAssignableFrom(boolean.class)) {

            return "NUMERIC";
        } else if (type.isAssignableFrom(byte[].class)) {

            return "BLOB";
        }

        return "TEXT";
    }

    /**
     * Check reserved words from name of table or column before it is created. Checking will be executed against
     * reserved words and if name is not included in reserved words no exception will be thrown.
     *
     * @param name String value of name to be checked against reserved words.
     * @throws SchemaInitializationException if name is found from reserved words the schema creation
     * cannot be proceed.
     *
     * @since 1.1.3
     *
     * @hide
     */
    private static void checkReservedWords(String name) throws SchemaInitializationException {
        if (ReservedWords.has(name)) {
            throw new SchemaInitializationException("Found illegal word from table name or column name. " +
                    " Given value: " + name);
        }
    }

    /**
     * Resolve name for table or column.
     *
     * @param type T type which is entity class or {@link Field} to resolve name from.
     * @return String resolved name.
     * @throws SchemaInitializationException if any error occurs during name resolving.
     *
     * @since 1.2.0
     *
     * @hide
     */
    private static <T> String resolveName(T type) throws SchemaInitializationException {
        try {

            return NameResolver.resolveName(type);

        } catch (NameResolveException e) {

            Log.e(SchemaFactory.class.getName(), "Failed to create schema", e);
            throw new SchemaInitializationException("Schema initialization failed", e);
        }
    }

    /**
     * Resolve id column name for given class. Id column is annotated with {@link Id} annotation.
     * If annotation from class is not found error will be thrown.
     *
     * @param model Entity class which id column name will be resolved.
     * @return String id column name.
     * @throws SchemaInitializationException if any error occurs during name resolving.
     *
     * @since 1.2.0
     *
     * @hide
     */
    private static String resolveIdColumn(Class<?> model) throws SchemaInitializationException {
        try {

            return NameResolver.resolveIdName(model);

        } catch (NameResolveException e) {

            throw new SchemaInitializationException("Failed to create schema", e);
        }
    }

}
