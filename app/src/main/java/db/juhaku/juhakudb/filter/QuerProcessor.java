package db.juhaku.juhakudb.filter;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import db.juhaku.juhakudb.core.NameResolver;
import db.juhaku.juhakudb.core.schema.Schema;
import db.juhaku.juhakudb.exception.NameResolveException;
import db.juhaku.juhakudb.exception.QueryBuildException;
import db.juhaku.juhakudb.filter.Root.Join;
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
                String alias = aliasMap.get(join.getModel());

                if (StringUtils.isBlank(alias)) {
                    alias = generateAlias(resolveTableName(join.getModel()));
                    aliasMap.put(join.getModel(), alias);

                    return alias;
                } else {

                    return alias;
                }
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

            return aliasBuilder.toString();
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

        void processJoins(Root<?> root, StringBuilder sql) {
            Class<?> model = root.getModel();

            String rootAlias = Alias.forModel(model);
            String rootTable = resolveTableName(model);

            for (Root<?> r : root.getJoins()) {
                Join join = (Join) r;

                sql.append(" ").append(join.getJoinMode().getValue()).append(" ")
                        .append(rootAlias).append(".").append(rootTable).append(" = ")
                        .append(Alias.forJoin(join)).append(".").append(resolveTableName(join.getModel()));

                if (!join.getJoins().isEmpty()) {
                    processJoins(join, sql);
                }
            }
        }
    }
}
