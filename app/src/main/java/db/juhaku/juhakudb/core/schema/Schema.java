package db.juhaku.juhakudb.core.schema;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import db.juhaku.juhakudb.core.DatabaseConfiguration;
import db.juhaku.juhakudb.exception.SchemaInitializationException;
import db.juhaku.juhakudb.util.StringUtils;

/**
 * Created by juha on 7/3/15.
 * <p>Schema is object tree providing database tables as objects.</p>
 *
 * @author Juha Kukkonen
 *
 * @since 1.0.2
 */
public class Schema implements Serializable {

    /**
     * Enum for different DDL types.
     */
    public enum DDL {
        /**
         * DDL to create table.
         */
        CREATE,
        /**
         * DDL to drop table.
         */
        DROP;

        /**
         * Generates alter table DDL for given table with given column.
         * @param table Schema table to alter with.
         * @param column Schema column to add.
         * @return String value containing alter table DDL.
         */
        public static String alterTable(Schema table, Schema column) {
            return new StringBuilder("ALTER TABLE ").append(table.getName())
                    .append(" ADD COLUMN ").append(column.getName()).append(" ")
                    .append(column.getType()).append(" ").append(column.getExtensions()).toString();
        }
    }

    private static SchemaFactory factory;
    private static SchemaComparator schemaComparator;

    private String name;
    private String type;
    private String extensions;
    private Map<String, Schema> tables;
    private Schema parent;
    private List<Reference> references;
    private Integer order;

    static {
        factory = new SchemaFactory();
        schemaComparator = new SchemaComparator();
    }

    /**
     * Generate new instance of schema of current database.
     *
     * @param configuration instance of {@link DatabaseConfiguration} to configure schema for.
     * @param entities Array of entities to create schema for.
     * @return new database schema
     * @throws SchemaInitializationException if some exception occurs during initialization.
     *
     * @since 1.0.2
     */
    public static Schema newInstance(DatabaseConfiguration configuration, Class<?>[] entities)
            throws SchemaInitializationException {
        return factory.getSchema(configuration.getName(), entities);
    }

    /**
     * Get elements name. Can be table name as well as column name.
     * @return String value of name for current element.
     *
     * @since 1.0.2
     */
    public String getName() {
        return name;
    }

    /**
     * Set element name. If element is table works as table name if element is column work as
     * columns name.
     *
     * @param name String value of the element name.
     *
     * @since 1.0.2
     */
    void setName(String name) {
        this.name = name;
    }

    /**
     * Get database column type of current element. See {@link #setType(String)}.
     *
     * @return String value of type.
     *
     * @since 1.0.2
     */
    String getType() {
        return type;
    }

    /**
     * Set database column type that is available in currently used database. E.g. INTEGER.
     *
     * @param type String value of the type to put for the element.
     *
     * @since 1.0.2
     */
    void setType(String type) {
        this.type = type;
    }

    /**
     * Get extensions of database column. See {@link #setExtensions(String)}.
     *
     * @return String value of extensions provided for the column.
     *
     * @since 1.0.2
     */
    String getExtensions() {
        return extensions;
    }

    /**
     * Set extensions for database column. E.g PRIMARY KEY.
     *
     * @param extensions String value of extensions.
     *
     * @since 1.0.2
     */
    void setExtensions(String extensions) {
        this.extensions = extensions;
    }

    /**
     * Get list of references of this table in database. References is not used with column elements.
     *
     * @return List containing references of the table. If table has no references empty list is
     * returned.
     *
     * @since 1.0.2
     */
    public List<Reference> getReferences() {
        if (references == null) {
            references = new ArrayList<>();
        }
        return references;
    }

    /**
     * Set list of references to this database table.
     *
     * @param references List containing references.
     * @see Reference
     *
     * @since 1.0.2
     */
    void setReferences(List<Reference> references) {
        this.references = references;
    }

    /**
     * Get elements of current of current element. If current element is root of the database
     * this returns all the tables. If this element is table all columns is returned.
     *
     * @return Map containing elements associated as child elements to this element.
     *
     * @since 1.0.2
     */
    public Map<String, Schema> getElements() {
        if (tables == null) {
            tables = new TreeMap<>();
        }
        return tables;
    }

    /**
     * Get current order of database table. If calling element is table column null is returned.
     * This is used to provide sorting order for tables.
     *
     * @return Integer value of order.
     *
     * @since 1.0.2
     */
    public Integer getOrder() {
        return order;
    }

    /**
     * Set order for current database table. This is used to sort tables by
     * {@link SchemaComparator}.
     *
     * @param order Integer value of order.
     *
     * @since 1.0.2
     */
    void setOrder(Integer order) {
        this.order = order;
    }

    /**
     * Get parent element of this element.
     *
     * @return instance of Schema.
     *
     * @since 1.0.2
     */
    public Schema getParent() {
        return parent;
    }

    /**
     * Set parent Schema element for this element.
     *
     * @param parent Schema parent element.
     *
     * @since 1.0.2
     */
    void setParent(Schema parent) {
        this.parent = parent;
    }

    /**
     * Add table to current schema root.
     *
     * @param element Schema as table element.
     *
     * @since 1.0.2
     */
    void addTable(Schema element) {
        getElements().put(element.getName(), element);
        element.setParent(this);
    }

    /**
     * Add column to current table element.
     *
     * @param element Schema as column element.
     *
     * @since 1.0.2
     */
    void addColumn(Schema element) {
        getElements().put(element.getName(), element);
        element.setParent(this);
    }

    /**
     * Get element by element name. If current element is root of schema table is looked for.
     * If current element is table then column is looked for.
     *
     * @param element String name of element to look for.
     *
     * @return Found Schema element otherwise null.
     *
     * @since 1.0.2
     */
    public Schema getElement(String element) {
        return getElements().get(element);
    }

    /**
     * Transforms current Schema element to DDL SQL according given
     * {@link Schema.DDL} enum.
     *
     * @param ddl DDL to provide transformation type.
     *
     * @return String containing current element transformed to DDL.
     *
     * @since 1.0.2
     */
    public String toDDL(DDL ddl) {
        StringBuilder ddlBuilder = new StringBuilder();
        switch (ddl) {
            case CREATE:
                createDDL(ddlBuilder);
                break;
            case DROP:
                dropDDL(ddlBuilder);
                break;
        }
        return ddlBuilder.toString();
    }

    private void createDDL(StringBuilder ddlBuilder) {
        ddlBuilder.append("CREATE TABLE IF NOT EXISTS " + this.getName() + " (");
        for (Entry<String, Schema> columnEntry : this.getElements().entrySet()) {
            Schema column = columnEntry.getValue();
            ddlBuilder.append(column.getName()).append(" ").append(column.getType());
            if (column.getExtensions() != null) {
                ddlBuilder.append(" ").append(column.getExtensions());
            }
            ddlBuilder.append(", ");
        }
        for (Reference reference : this.getReferences()) {
            ddlBuilder.append(reference.toDDL()).append(", ");
        }
        ddlBuilder.replace(ddlBuilder.length() - 2, ddlBuilder.length(), ""); // remove last comma (,)
        ddlBuilder.append(");");
    }

    private void dropDDL(StringBuilder ddlBuilder) {
        ddlBuilder.append("DROP TABLE " + this.getName() + ";");
    }

    /**
     * Comparator to provide comparison for schemas.
     * Schema table elements are compared by their {@link #getOrder()}.
     */
    private static class SchemaComparator implements Comparator<Schema>, Serializable {
        @Override
        public int compare(Schema lhs, Schema rhs) {
            if (lhs == null || lhs.order == null) {
                return -1;
            }
            if (rhs == null || rhs.order == null) {
                return 1;
            }

            return lhs.order.compareTo(rhs.order);
        }
    }

    /**
     * Converts root Schema to set containing only ordered tables. This is convenient for
     * database creation. <br><br>If provided schema is not root empty set will be returned.
     *
     * @param schema root Schema of database.
     * @return Set with ordered tables of the root schema.
     *
     * @since 1.0.2
     */
    public static Set<Schema> toSet(Schema schema) {
        Set<Schema> tableSet = new TreeSet<>(schemaComparator);
        if (schema.getParent() == null) { // only root schema is allowed
            for (Entry<String, Schema> entry : schema.tables.entrySet()) {
                tableSet.add(entry.getValue());
            }
        }

        return tableSet;
    }
}
