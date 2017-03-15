package db.juhaku.juhakudb.filter;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import db.juhaku.juhakudb.annotation.ManyToMany;
import db.juhaku.juhakudb.core.schema.Reference;
import db.juhaku.juhakudb.core.schema.Schema;
import db.juhaku.juhakudb.core.NameResolver;
import db.juhaku.juhakudb.exception.NameResolveException;
import db.juhaku.juhakudb.exception.QueryBuildException;
import db.juhaku.juhakudb.util.ReflectionUtils;
import db.juhaku.juhakudb.util.StringUtils;

/**
 * Created by juha on 27/04/16.
 *<p>Association processor resolves associations for database queries that are created
 * via {@link Root} object.</p>
 *
 * @author juha
 *
 * @since 1.0.2
 */
public class AssociationProcessor {

    private Schema schema;
    private Class<?> rootEntity;
    private Map<String, Class<?>> clazzAliasMap;

    /**
     * Initialize new instance of association processor for given schema.
     * @param schema instance of {@link Schema}.
     *
     * @since 1.0.2
     */
    public AssociationProcessor(Schema schema) {
        this(schema, null);
    }

    /**
     * Initialize new instance of association processor for schema and root entity.
     * @param schema instance of {@link Schema}.
     * @param rootEntity Class of root entity.
     * @since 1.0.2
     */
    public AssociationProcessor(Schema schema, Class<?> rootEntity) {
        this.schema = schema;
        this.rootEntity = rootEntity;
        this.clazzAliasMap = new HashMap<>();
    }

    public void setRootEntity(Class<?> rootEntity) {
        this.rootEntity = rootEntity;
    }

    /**
     * Prepare, resolve and create associations between database tables.
     * @param parent instance of {@link Root} that contains resolvable joins.
     * @since 1.0.2
     */
    public synchronized <T> void processAssosiations(Root<T> parent) {
        for (Root<T> childRoot : parent.getJoins()) {
            prepareAssociations(parent, childRoot);
        }
        clazzAliasMap.clear();
    }

    private <T> void prepareAssociations(Root<T> parent, Root<T> child) {
        // resolve root entity and association entity
        String prefix = null;
        if (child.getAssociation().indexOf(".") > -1) {
            prefix = child.getAssociation().substring(0, child.getAssociation().indexOf("."));
        }
        Class<?> rootClazz = rootEntity;
        Field associationField;
        if (prefix == null || (prefix != null && prefix.equalsIgnoreCase("this"))) {
            // join from root entity
            String association = child.getAssociation();
            if (association.indexOf(".") > -1) {
                association = association.substring(association.indexOf(".") + 1);
            }
            associationField = ReflectionUtils.findField(rootClazz, association);
        } else {
            // join from previous entity
            Root<T> root = findJoinByAlias(parent, prefix);
            if (root.getAssociation().indexOf(".") > -1) {
                prefix = root.getAssociation().substring(0, root.getAssociation().indexOf("."));
            }
            Class<?> associateEntity = clazzAliasMap.get(prefix);
            if (associateEntity == null) {
                throw new QueryBuildException("association not defined for prefix: " + prefix);
            }
            rootClazz = associateEntity;
            String association = child.getAssociation(); // default association
            if (!StringUtils.isBlank(prefix)) {
                association = association.substring(association.indexOf(".") + 1);
            }
            associationField = ReflectionUtils.findField(associateEntity, association);
        }

        if (associationField == null) {
            throw new QueryBuildException("association field cannot be determined by name: " + child.getAssociation() + " from parent: " + rootClazz.getName());
        }

        String rootTable;
        String associateTable;
        try {
            Class<?> associateEntity = ReflectionUtils.getFieldType(associationField);
            if (!clazzAliasMap.containsKey(child.getAlias())) {
                clazzAliasMap.put(child.getAlias(), associateEntity);
            }
            rootTable = NameResolver.resolveName(rootClazz);
            associateTable = NameResolver.resolveName(associateEntity);
        } catch (NameResolveException e) {
            throw new QueryBuildException("failed to resolve table names", e);
        }

        // create many to many association
        if (associationField.isAnnotationPresent(ManyToMany.class)) {
            Schema joinTable = findJoinTable(rootTable, associateTable);
            if (joinTable != null) {
                StringBuilder joinBuilder = new StringBuilder();
                Reference rootReference = findReferenceByReferencedTable(joinTable, rootTable);
                String rootJoin = createAssociation(child, rootReference.getReferenceTableName(),
                        rootReference.getReferenceColumnName(), null, joinTable.getName(),
                        rootReference.getColumnName(), generateAlias(joinTable.getName()));
                joinBuilder.append(rootJoin).append(" ");
                Reference associateReference = findReferenceByReferencedTable(joinTable, associateTable);
                String associateJoin = createAssociation(child, joinTable.getName(), associateReference.getColumnName(),
                        generateAlias(joinTable.getName()), associateReference.getReferenceTableName(),
                        associateReference.getReferenceColumnName(), null);
                joinBuilder.append(associateJoin).append(" ");
                child.setJoin(joinBuilder.toString());
                return;
            }
        } else {
            // create owner based association
            String column = associateTable.concat(NameResolver.ID_FIELD_SUFFIX);
            Schema table = findTableByColumnNameAndTableName(associateTable.concat(NameResolver.ID_FIELD_SUFFIX), rootTable);
            if (table == null) {
                column = rootTable.concat(NameResolver.ID_FIELD_SUFFIX);
                table = findTableByColumnNameAndTableName(rootTable.concat(NameResolver.ID_FIELD_SUFFIX), associateTable);
            }
            if (table != null) {
                if (table.getName().equals(rootTable)) {
                    for (Reference reference : table.getReferences()) {
                        if (reference.getColumnName().equals(column)) {
                            String join = createAssociation(child, rootTable, reference.getColumnName(), null,
                                    associateTable, reference.getReferenceColumnName(), child.getAlias());
                            child.setJoin(join.concat(" "));
                            return;
                        }
                    }
                } else {
                    for (Reference reference : table.getReferences()) {
                        if (reference.getColumnName().equals(column)) {
                            String join = createAssociation(child, rootTable, reference.getReferenceColumnName(), null,
                                    associateTable, reference.getColumnName(), child.getAlias());
                            child.setJoin(join.concat(" "));
                            return;
                        }
                    }
                }
            }
        }
        throw new QueryBuildException("failed to build query, referenced table not found with: "
                + rootTable + " and " + associateTable);
    }

    private <T> String createAssociation(Root<T> root, String rootTable, String rootColumn,
                                         String rootAlias, String associateTable, String associateColumn, String associateAlias) {
        if (StringUtils.isBlank(rootAlias)) {
            rootAlias = String.valueOf(rootTable.charAt(0));
        }
        if (StringUtils.isBlank(associateAlias)) {
            associateAlias = String.valueOf(associateTable.charAt(0));
        }

        StringBuilder association = new StringBuilder();
        association.append(root.getJoinMode().getValue()).append(" ")
                .append(associateTable).append(" ").append(associateAlias).append(" ON ")
                .append(associateAlias).append(".").append(associateColumn).append(" = ")
                .append(rootAlias).append(".").append(rootColumn);

        return association.toString();
    }

    private <T> Root<T> findJoinByAlias(Root<T> parent, String alias) {
        for (Root<T> root : parent.getJoins()) {
            if ((root.getAlias().equals(alias))) {
                return root;
            }
        }

        return null;
    }

    private Schema findJoinTable(String rootTableName, String associateTableName) {
        String tableName = rootTableName.concat("_").concat(associateTableName);
        Schema table;
        if ((table = schema.getElement(tableName)) == null) {
            tableName = associateTableName.concat("_").concat(rootTableName);
            return schema.getElement(tableName);
        }

        return table;
    }

    private Schema findTableByColumnNameAndTableName(String name, String tableName) {
        for (Entry<String, Schema> entry : schema.getElements().entrySet()) {
            if (entry.getKey().equals(tableName) && entry.getValue().getElement(name) != null) {
                return entry.getValue();
            }
        }

        return null;
    }

    private String generateAlias(String tableName) {
        StringBuilder aliasBuilder = new StringBuilder(String.valueOf(tableName.charAt(0)));
        int index;
        while ((index = tableName.indexOf("_")) > -1) {
            aliasBuilder.append(tableName.charAt(index + 1));
            tableName = tableName.substring(index + 1);
        }

        return aliasBuilder.toString();
    }

    public Reference findReferenceByReferencedTable(Schema table, String referencedTable) {
        for (Reference reference : table.getReferences()) {
            if (reference.getReferenceTableName().equals(referencedTable)) {
                return reference;
            }
        }

        return null;
    }
}
