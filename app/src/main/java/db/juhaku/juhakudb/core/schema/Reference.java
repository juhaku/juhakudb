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

import java.io.Serializable;
import java.lang.reflect.Field;

import db.juhaku.juhakudb.core.NameResolver;
import db.juhaku.juhakudb.exception.NameResolveException;
import db.juhaku.juhakudb.util.ReflectionUtils;

/**
 * Created by juha on 23/12/15.
 *<p>Reference is java representation for reference between two tables in database. This normally used
 * in schema creation.</p>
 * @author juha
 * @see Schema
 *
 * @since 1.0.2
 */
public class Reference implements Serializable {

    private String columnName;
    private String referenceTableName;
    private String referenceColumnName;

    /**
     * Initialize new reference by given parameters.
     *
     * @param columnName String value of current tables column name that refers to {@link #getReferenceColumnName()}
     * @param referenceTableName String value of referenced table name.
     * @param referenceColumnName String value ot referenced column name that is referred by {@link #getColumnName()}.
     */
    public Reference(String columnName, String referenceTableName, String referenceColumnName) {
        this.columnName = columnName;
        this.referenceTableName = referenceTableName;
        this.referenceColumnName = referenceColumnName;
    }

    /**
     * Initialize new reference from given column name to given field. Field is used to resolve
     * referenced table name and referenced column name.
     *
     * @param columnName String current tables column name to refer from.
     * @param field Reflection objects field that is used to resolve referenced table and column.
     * @throws NameResolveException if exception occurs on resolving table and column name.
     *
     * @since 1.0.2
     */
    public Reference(String columnName, Field field) throws NameResolveException {
        this.columnName = columnName;

        Class<?> resolveClass = ReflectionUtils.getFieldType(field);
        this.referenceTableName = NameResolver.resolveName(resolveClass);
        this.referenceColumnName = NameResolver.resolveIdName(resolveClass);
    }

    /**
     * Get current tables column name that refers to another column ({@link #getReferenceColumnName()}).
     *
     * @return String value of column name.
     *
     * @since 1.0.2
     */
    public String getColumnName() {
        return columnName;
    }

    /**
     * Set current tables column name that refers to {@link #getReferenceColumnName()}.
     *
     * @param columnName String value of the column name.
     *
     * @since 1.0.2
     */
    void setColumnName(String columnName) {
        this.columnName = columnName;
    }

    /**
     * Get referenced table name.
     *
     * @return String value of table name that is referred.
     *
     * @since 1.0.2
     */
    public String getReferenceTableName() {
        return referenceTableName;
    }

    /**
     * Set referenced table name.
     *
     * @param referenceTableName String value of table name that is referenced.
     *
     * @since 1.0.2
     */
    void setReferenceTableName(String referenceTableName) {
        this.referenceTableName = referenceTableName;
    }

    /**
     * Get referenced column name that is referred by {@link #getColumnName()}.
     *
     * @return String value of referenced column name.
     *
     * @since 1.0.2
     */
    public String getReferenceColumnName() {
        return referenceColumnName;
    }

    /**
     * Set referenced column name that is referred by {@link #getColumnName()}.
     *
     * @param referenceColumnName String value of the referenced column name.
     *
     * @since 1.0.2
     */
    void setReferenceColumnName(String referenceColumnName) {
        this.referenceColumnName = referenceColumnName;
    }

    /**
     * Transform reference to DDL SQL.
     *
     * @return String value containing DDL of the reference.
     *
     * @since 1.0.2
     */
    String toDDL() {
        return new StringBuilder("FOREIGN KEY(").append(columnName)
                .append(") REFERENCES ").append(referenceTableName).append("(")
                .append(referenceColumnName).append(")").toString();
    }
}
