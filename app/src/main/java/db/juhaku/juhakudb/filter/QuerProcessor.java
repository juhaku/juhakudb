package db.juhaku.juhakudb.filter;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import db.juhaku.juhakudb.annotation.ManyToMany;
import db.juhaku.juhakudb.core.NameResolver;
import db.juhaku.juhakudb.core.schema.Schema;
import db.juhaku.juhakudb.exception.NameResolveException;
import db.juhaku.juhakudb.exception.QueryBuildException;
import db.juhaku.juhakudb.filter.Root.Join;
import db.juhaku.juhakudb.util.ReflectionUtils;
import db.juhaku.juhakudb.util.StringUtils;

/**
 * Created by juha on 17/03/17.
 *
 * @author juha
 *
 * @since 1.1.3-SNAPSHOT
 */
public class QuerProcessor {

    private Schema schema;
    private JoinProcessor joinProcessor = new JoinProcessor();

    public QuerProcessor(Schema schema) {
        this.schema = schema;
    }

    public Query createQuery(Class<?> modelClass, Filter filter) {
        // initialize root and predicates for joins and restrictions
        Root<?> root = new Root<>(modelClass);
        Predicates predicates = new Predicates();

        // create joins and restrictions
        filter.filter(root, predicates);



        return null;
    }

    private void createSelect(Root<?> root, StringBuilder sql) {
        Class<?> model = root.getModel();

        sql.append("SELECT ");
        sql.append(generateSelectForModel(model));

        alterSelect(root, sql);

        sql.append(" FROM ").append(resolveTableName(model)).append(" ").append(Alias.forModel(model)).append(" ");

    }

    private void alterSelect(Root<?> root, StringBuilder sql) {
        for (Root r : root.getJoins()) {
            Join join = (Join) r;
            if (join.isFetch()) {
                sql.append(" ");
                sql.append(generateSelectForModel(join.getModel()));
            }
            if (!root.getJoins().isEmpty()) {
                alterSelect(join, sql);
            }
        }
    }

    private String generateSelectForModel(Class<?> model) {
        Schema table = schema.getElement(resolveTableName(model));
        String alias = Alias.forModel(model);

        StringBuilder select = new StringBuilder();

        Iterator<String> colIterator = table.getElements().keySet().iterator();
        while (colIterator.hasNext()) {

            select.append(alias).append(".").append(colIterator.next());

            if (colIterator.hasNext()) {
                select.append(", ");
            }
        }
//        select.append(" FROM ").append(table.getName()).append(" ").append(alias).append(" ");

        return select.toString();
    }

    /**
     * Resolve table name throwing silently exception if resolving table name will fail.
     * @param model Instance of {@link Class} of model class of entity.
     * @return String name of table resolved for the model class.
     *
     * @since
     */
    private static String resolveTableName(Class<?> model) {
        try {

            return NameResolver.resolveName(model);
        } catch (NameResolveException e) {
            throw new QueryBuildException("Failed to build query, unknown table: " + model.toString() + ", reason: " + e.getMessage(), e);
        }
    }

    public Query createWhere(Class<?> modelClass, Filter<?> filter) {
        return null;
    }

    public static class Alias {

        private static AtomicInteger count = new AtomicInteger();

        private static Map<Class<?>, String> aliasMap = new HashMap<>();

        public static String forJoin(Join join) {

            /*
             * If alias is empty and it does not exist in cache add new one.
             */
            if (StringUtils.isBlank(join.getAlias())) {

                return forModel(join.getModel());
            } else {

                return join.getAlias();
            }
        }

        public static String forModel(Class<?> model) {
            String alias = aliasMap.get(model);

            /*
             * If alias is not cached generate new and place it to cache.
             */
            if (StringUtils.isBlank(alias)) {
                alias = generateAlias(resolveTableName(model));
                aliasMap.put(model, alias);
            }

            return alias;

        }

        private static String generateAlias(String tableName) {
            StringBuilder aliasBuilder = new StringBuilder(String.valueOf(tableName.charAt(0)));
            int index;
            while ((index = tableName.indexOf("_")) > -1) {
                aliasBuilder.append(tableName.charAt(index + 1));
                tableName = tableName.substring(index + 1);
            }

            return aliasBuilder.append(String.valueOf(count.incrementAndGet())).toString();
        }

        public static void clearCache() {
            if (aliasMap != null) {
                aliasMap.clear();
            }
            if (count != null) {
                count.set(0);
            }
        }

    }

    private static class JoinProcessor {

// TODO implement joins
//        left join persons p on p.id = pp.person_id
//
//        left join person_books bp on p.id = pb.person_id left join books b on b.id = bp.book_id

        void createJoins(Root<?> root, StringBuilder sql) {
            Class<?> model = root.getModel();

            String rootAlias = Alias.forModel(model);
            String rootTable = resolveTableName(model);

            for (Root<?> r : root.getJoins()) {
                Join join = (Join) r;

                Field targetField = ReflectionUtils.findField(model, join.getTarget());

                if (targetField.isAnnotationPresent(ManyToMany.class)) {

                } else {

                }

                sql.append(" ").append(join.getJoinMode().getValue()).append(" ")
                        .append(rootAlias).append(".").append(rootTable).append(" = ")
                        .append(Alias.forJoin(join)).append(".").append(resolveTableName(join.getModel()));

                if (!join.getJoins().isEmpty()) {
                    createJoins(join, sql);
                }
            }
        }

        private void middleTableJoin(Root<?> root, StringBuilder sql) {

        }

        private void ownerJoin(Root<?> root, StringBuilder sql) {

        }

        /**
         * Creates join sql with given join mode to target table according given parameters.
         *
         * @param sql Instance of {@link StringBuilder} where to append the sql.
         * @param joinMode Instance of {@link JoinMode} that is being used with this join.
         * @param toTable String name of the table that join is being made.
         * @param fromColumn String name of the from join column.
         * @param fromAlias String alias of from join column.
         * @param toColumn String name of the to join column.
         * @param toAlias String alias of to join column.
         *
         * @since
         *
         * @hide
         */
        private void joinSql(StringBuilder sql, JoinMode joinMode, String toTable, String fromColumn,
                             String fromAlias, String toColumn, String toAlias) {
            sql.append(" ").append(joinMode.getValue()).append(" ").append(toTable)
                    .append(" ").append(toAlias).append(" ON ").append(fromAlias).append(".")
                    .append(fromColumn).append(" = ").append(toAlias).append(".").append(toColumn);
        }
    }
}
