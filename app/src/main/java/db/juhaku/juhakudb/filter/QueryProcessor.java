package db.juhaku.juhakudb.filter;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.concurrent.atomic.AtomicInteger;

import db.juhaku.juhakudb.annotation.ManyToMany;
import db.juhaku.juhakudb.annotation.ManyToOne;
import db.juhaku.juhakudb.annotation.OneToOne;
import db.juhaku.juhakudb.core.NameResolver;
import db.juhaku.juhakudb.core.schema.Reference;
import db.juhaku.juhakudb.core.schema.Schema;
import db.juhaku.juhakudb.exception.IllegalJoinException;
import db.juhaku.juhakudb.exception.NameResolveException;
import db.juhaku.juhakudb.exception.QueryBuildException;
import db.juhaku.juhakudb.filter.Predicate.Conjunction;
import db.juhaku.juhakudb.filter.Predicate.Disjunction;
import db.juhaku.juhakudb.filter.Predicate.Junction;
import db.juhaku.juhakudb.filter.Root.Join;
import db.juhaku.juhakudb.util.ReflectionUtils;
import db.juhaku.juhakudb.util.ReservedWords;
import db.juhaku.juhakudb.util.StringUtils;

/**
 * Created by juha on 17/03/17.
 *
 * <p>This class will process all filter api created queries and turn them into proper sql queries
 * for the database.</p>
 *
 * @author juha
 *
 * @since 1.2.0-SNAPSHOT
 */
public class QueryProcessor {

    private Schema schema;

    /**
     * Initialize new {@link QueryProcessor} with given schema. Schema is used to resolve correct
     * tables for sql queries and joins.
     *
     * @param schema instance of {@link Schema}.
     *
     * @since 1.2.0-SNAPSHOT
     */
    public QueryProcessor(Schema schema) {
        this.schema = schema;
    }

    /**
     * Create full sql query with parameters. Sql query is formatted to {@link Query} object containing
     * sql and parameters in correct order.
     *
     * @param modelClass Instance of {@link Class} of model class of database tables.
     * @param filter {@link Filter} to create select, joins and where statement.
     * @return newly created query.
     *
     * @since 1.2.0-SNAPSHOT
     */
    public Query createQuery(Class<?> modelClass, Filter filter) {
        // initialize root and predicates for joins and restrictions
        Root<?> root = new Root<>(modelClass);
        Predicates predicates = new Predicates();

        // create joins and restrictions
        filter.filter(root, predicates);

        StringBuilder sql = new StringBuilder();
        createSelect(root, sql); // create select statement from root

        createJoins(root, sql);

        if (!predicates.getPredicates().isEmpty()) {
            sql.append("WHERE ");
        }
        String[] args = createWhere(root, sql, predicates); // create where clause from predicates

        String order = predicates.getSort();
        String page = predicates.getPage();
        if (!StringUtils.isBlank(order)) {
            sql.append(order);
        }
        if (!StringUtils.isBlank(page)) {
            sql.append(page);
        }

        Alias.clearCache();

        Query query = new Query(sql.toString(), args);
        query.setRoot(root);

        return query;
    }

    /**
     * Create select statement for the root of query.
     *
     * @param root {@link Root} of query.
     * @param sql Instance of {@link StringBuilder} containing current sql.
     *
     * @since 1.2.0-SNAPSHOT
     *
     * @hide
     */
    private void createSelect(Root<?> root, StringBuilder sql) {
        Class<?> model = root.getModel();

        sql.append("SELECT ");
        sql.append(generateSelectForModel(model, Alias.forModel(model)));

        alterSelect(root, sql);

        sql.append(" FROM ").append(resolveName(model)).append(" ").append(Alias.forModel(model)).append(" ");
    }

    /**
     * Alter the select statement with fetch joins. This is to create more fluent select statements
     * that can return multiple tables with a single query.
     *
     * @param root {@link Root} where to create query from.
     * @param sql {@link StringBuilder} containing current sql.
     *
     * @since 1.2.0-SNAPSHOT
     *
     * @hide
     */
    private void alterSelect(Root<?> root, StringBuilder sql) {
        for (Root r : root.getJoins()) {
            Join join = (Join) r;
            if (join.isFetch()) {
                sql.append(", ");
                sql.append(generateSelectForModel(join.getModel(), Alias.forJoin(join)));
            }
            if (!root.getJoins().isEmpty()) {
                alterSelect(join, sql);
            }
        }
    }

    /**
     * Generates select statement for given model class. Model class must be instance of database
     * entity classes. Select statement will be aliased and all the columns that are in database
     * will be returned.
     *
     * @param model Instance of {@link Class} of model class of database table entities.
     * @param alias String alias for model class.
     * @return String containing select statement without "SELECT" in the beginning for given model.
     *
     * @sine
     *
     * @hide
     */
    private String generateSelectForModel(Class<?> model, String alias) {
        Schema table = schema.getElement(resolveName(model));

        StringBuilder select = new StringBuilder();

        Iterator<String> colIterator = table.getElements().keySet().iterator();
        while (colIterator.hasNext()) {

            select.append(alias).append(".").append(colIterator.next());

            if (colIterator.hasNext()) {
                select.append(", ");
            }
        }

        return select.toString();
    }

    /**
     * Create where statement without "WHERE" in the beginning. Where statement is created from
     * provided predicates.
     *
     * <p>Sql will be updated according the predicates provided and all provided parameters will be
     * returned by the method in correct order. Parameters are returned to support old api.</p>
     *
     * @param root {@link Root} of where statement.
     * @param sql {@link StringBuilder} current sql.
     * @param predicates {@link Predicates} WHERE statement query parameters and criterias.
     * @return String array of parameters from predicates.
     *
     * @since 1.2.0-SNAPSHOT
     *
     * @hide
     */
    private String[] createWhere(Root<?> root, StringBuilder sql, Predicates predicates) {
        String[] args = new String[0];
        String alias = null;

        if (root.getModel() != null) {
            alias = Alias.forModel(root.getModel());
        }

        Iterator<Predicate> junctionIterator = predicates.getPredicates().iterator();

        // Loop through all predicates to create where clause
        while (junctionIterator.hasNext()) {
            Predicate predicate = junctionIterator.next();

            // Create grouping for junctions
            if (predicate instanceof Junction) {
                sql.append("(");
                Iterator<Predicate> predicateIterator = ((Junction) predicate).getPredicates().iterator();

                while (predicateIterator.hasNext()) {
                    Predicate junction = predicateIterator.next();

                    sql.append(formatClause(junction.getClause(), alias));

                    if (predicate instanceof Disjunction && predicateIterator.hasNext()) {
                        sql.append(" OR ");
                    } else if (predicate instanceof Conjunction && predicateIterator.hasNext()) {
                        sql.append(" AND ");
                    }

                    args = addArgsToArray(args, junction.getArgs());
                }
                sql.append(")");

                if (junctionIterator.hasNext()) {
                    sql.append(" AND ");
                }
            } else {
                /*
                 * Non junctions are just added to to where clause.
                 */
                sql.append(formatClause(predicate.getClause(), alias));

                if (junctionIterator.hasNext()) {
                    sql.append(" AND ");
                }
                args = addArgsToArray(args, predicate.getArgs());
            }
        }

        return args;
    }

    /**
     * Append given args to array of args.
     * @param array String[] array to append.
     * @param args String[] to take args from.
     * @return new string array containing previous args and newly appended args.
     *
     * @since 1.2.0-SNAPSHOT
     *
     * @hide
     */
    private String[] addArgsToArray(String[] array, String[] args) {
        String[] newArray = new String[array.length + args.length];
        System.arraycopy(array, 0, newArray, 0, array.length);
        System.arraycopy(args, 0, newArray, array.length, args.length);

        return newArray;
    }

    /**
     * Formats WHERE clause without WHERE text appended to follow rules of database.
     *
     * <ul>
     *     <li>Formats clause's "this" prefixes as alias provided.</li>
     *     <li>Formats empty prefixed columns with alias provided.</li>
     *     <li>Formats .id fields as ._id since Android primary key field is prefixed with "_" underscore.</li>
     * </ul>
     *
     * @param clause String sql clause to format aliases.
     * @param alias String value of alias to use. Can be null if no formatting is required.
     * @return String value of clause formatted if table name was provided otherwise returns the original
     * clause.
     * @hide
     *
     * @since 1.1.2
     */
    private static String formatClause(String clause, String alias) {
        String formatted = clause;

        // format "this" prefixes to use correct alias alias
        if (!StringUtils.isBlank(alias)) {
            formatted = clause.replace("this", alias);
        }

        // format .id fields to match correct primary key by adding "_" underscore in front.
        formatted = formatted.replace(".id", ".".concat(NameResolver.ID_FIELD_SUFFIX));

        StringTokenizer tokens = new StringTokenizer(formatted, " ");
        StringBuilder formattedBuilder = new StringBuilder();
        while (tokens.hasMoreElements()) {
            String token = tokens.nextToken();

            // check that token is not a reserved word nor symbol
            if (!Predicate.isSymbol(token) && !ReservedWords.has(token)) {

                // if token really is column name in database and alias is provided
                if (!token.contains(".") && !token.startsWith("(") && !token.startsWith(Predicate.PARAM_PLACE_HOLDER) && !StringUtils.isBlank(alias)) {
                    if (token.equals("id")) {
                        token = NameResolver.ID_FIELD_SUFFIX;
                    }
                    formattedBuilder.append(alias.concat(".").concat(token));
                } else {
                    formattedBuilder.append(token); // otherwise add token.
                }
            } else {
                formattedBuilder.append(token); // just add token as it does not contain relative information
            }

            // lastly add " " space to text.
            if (tokens.hasMoreElements()) {
                formattedBuilder.append(" ");
            }
        }

        return formattedBuilder.toString();
    }

    /**
     * Resolves table's name or table column's name silently. If any exception will occur then {@link QueryBuildException}
     * will be thrown.
     *
     * @param model Instance of {@link Class} of entity model.
     * @return String table name or column name depending on what is being resolved.
     *
     * @since 1.2.0-SNAPSHOT
     */
    private static <T> String resolveName(T model) {
        try {

            return NameResolver.resolveName(model);
        } catch (NameResolveException e) {
            throw new QueryBuildException("Failed to build query, unknown table: " + model.toString() + ", reason: " + e.getMessage(), e);
        }
    }

    /**
     * Resolves table's primary key column's name for given model class. The model class must be an
     * entity in database. Resolving is made silently and if any error will occur a {@link QueryBuildException}
     * will be thrown.
     *
     * @param model Instance of {@link Class} of model class of entity.
     * @return String primary key column name.
     *
     * @since 1.2.0-SNAPSHOT
     */
    private static String resolvePrimaryKey(Class<?> model) {
        try {
            return NameResolver.resolveIdName(model);
        } catch (NameResolveException e) {
            throw new QueryBuildException("Failed to build query, reason: " + e.getMessage(), e);
        }
    }

    /**
     * Create where clause without "WHERE“ in the beginning of of sql.
     *
     * @param modelClass Instance of {@link Class} of database model class.
     * @param filter {@link Filter} where the where clause is created from.
     * @return Query containing created where clause.
     *
     * @since 1.2.0-SNAPSHOT
     */
    public Query createWhere(Class<?> modelClass, Filter filter) {
        Root<?> root = new Root<>(modelClass);
        Predicates predicates = new Predicates();
        filter.filter(root, predicates);
        StringBuilder sql = new StringBuilder();
        String[] args = createWhere(root, sql, predicates);

        if (modelClass != null) {
            Alias.clearCache();
        }

        return new Query(sql.toString(), args);
    }

    /**
     * Create joins from given root. Root is the first table of query which contains joins to other
     * tables in treelike format.
     *
     * @param root Instance of {@link Root} where to create joins from.
     * @param sql Instance of {@link StringBuilder} that contains current sql.
     *
     * @since 1.2.0-SNAPSHOT
     *
     * @hide
     */
    private void createJoins(Root<?> root, StringBuilder sql) {
        Class<?> model = root.getModel();

        String rootAlias = Alias.forModel(model);
        String rootTable = resolveName(model);

        for (Root<?> r : root.getJoins()) {
            Join join = (Join) r;

            Field targetField = ReflectionUtils.findField(model, join.getTarget());

            if (targetField.isAnnotationPresent(ManyToMany.class)) {

                createMiddleTableJoin(rootAlias, rootTable, join, sql);

            } else if (targetField.isAnnotationPresent(ManyToOne.class) ||
                    (targetField.isAnnotationPresent(OneToOne.class)
                            && StringUtils.isBlank(targetField.getAnnotation(OneToOne.class).mappedBy()))) {

                /*
                 * Join sql for join where root table is the owner of the join.
                 */
                joinSql(sql, join.getJoinMode(), resolveName(join.getModel()),
                        resolvePrimaryKey(model), rootAlias, resolveReverseJoinColumnName(model, join.getModel()),
                        Alias.forJoin(join), true);
            } else {

                /*
                 * Join sql for join where target table is the owner of the join.
                 */

                //TODO if target field has column annotation use it as the name
                joinSql(sql, join.getJoinMode(), resolveName(join.getModel()), resolvePrimaryKey(model),
                        rootAlias, resolveReverseJoinColumnName(join.getModel(), model), Alias.forJoin(join), false);
            }

            if (!join.getJoins().isEmpty()) {
                createJoins(join, sql);
            }
        }
    }

    /**
     * Create middle table join from root table to joined table. Middle table is searched from Schema
     * to maintain integrity. If middle table is not found exception will be thrown.
     *
     * @param rootAlias String alias or root table.
     * @param rootTable String name of root table join will be created from.
     * @param join instance of {@link Join} between root table and joined table.
     * @param sql {@link StringBuilder} containing current sql.
     *
     * @since 1.2.0-SNAPSHOT
     *
     * @hide
     */
    private void createMiddleTableJoin(String rootAlias, String rootTable, Join join, StringBuilder sql) {
        String targetTable = resolveName(join.getModel());

        Schema middleTable = findTableByName(rootTable.concat("_").concat(targetTable));
        if (middleTable == null) {
            middleTable = findTableByName(targetTable.concat("_").concat(rootTable));
        }

        if (middleTable == null) {
            throw new QueryBuildException("Failed to create join from table: " + rootTable + " to " + targetTable + ", no middle table found!");
        }

        String middleTableAlias = Alias.generateAlias(middleTable.getName());

        for (Reference reference : middleTable.getReferences()) {

            if (reference.getReferenceTableName().equals(rootTable)) {
                joinSql(sql, join.getJoinMode(), middleTable.getName(), NameResolver.ID_FIELD_SUFFIX,
                        rootAlias, reference.getColumnName(), middleTableAlias, false);
            }

            if (reference.getReferenceTableName().equals(targetTable)) {
                joinSql(sql, join.getJoinMode(), targetTable, reference.getColumnName(),
                        middleTableAlias, NameResolver.ID_FIELD_SUFFIX, Alias.forJoin(join), false);
            }
        }
    }

    /**
     * Resolves reverse join column name from given model class's table. Reverse join column name
     * is returned if reverse join table name is same as provided reverse join model.
     *
     * <p>Column is resolved by looking it for from {@link Schema} in order to maintain integrity.</p>
     *
     * @param model Instance of {@link Class} of model class of table where the join is made from.
     * @param reverseModel Instance of {@link Class} of reverse join model of table where the join is made to.
     * @return String reverse join column name from join table if found. If not found join processing
     * will fail to an exception.
     *
     * @since 1.2.0-SNAPSHOT
     *
     * @hide
     */
    private String resolveReverseJoinColumnName(Class<?> model, Class<?> reverseModel) {
        Schema table = findTableByName(resolveName(model));
        String reverseJoinTableName = resolveName(reverseModel);

        for (Reference reference : table.getReferences()) {
            if (reference.getReferenceTableName().equals(reverseJoinTableName)) {
                return reference.getColumnName();
            }
        }

        throw new IllegalJoinException("Failed to create join from entity: " + model.getName()
                + " to: " + reverseModel.getName() + " no reverse join column in table: " + table.getName());
    }

    /**
     * Find table by name from Schema.
     *
     * @param tableName String name of table.
     * @return Found table or null if not found.
     *
     * @since 1.2.0-SNAPSHOT
     *
     * @hide
     */
    private Schema findTableByName(String tableName) {
        for (Schema table : Schema.toSet(schema)) {
            if (table.getName().equals(tableName)) {
                return table;
            }
        }

        return null;
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
     * @param reverse boolean value whether reverse join is reversed.
     *
     * @since 1.2.0-SNAPSHOT
     *
     * @hide
     */
    private void joinSql(StringBuilder sql, JoinMode joinMode, String toTable, String fromColumn,
                         String fromAlias, String toColumn, String toAlias, boolean reverse) {
        sql.append(joinMode.getValue()).append(" ").append(toTable)
                .append(" ").append(toAlias).append(" ON ").append(reverse ? toAlias : fromAlias).append(".")
                .append(fromColumn).append(" = ").append(reverse ? fromAlias : toAlias).append(".")
                .append(toColumn).append(" ");
    }

    /**
     * Alias provides centralized way of aliasing sql queries to database tables. It can be used
     * to create alias for entities of database or joins created with filters.
     *
     * <p>All sql queries are aliased via this class to maintain integrity inside queries.</p>
     *
     * @since 1.2.0-SNAPSHOT
     */
    public static class Alias {

        private static AtomicInteger count = new AtomicInteger();

        private static Map<Class<?>, String> aliasMap = new HashMap<>();

        /**
         * Get alias for join. If custom alias is used then it will be returned otherwise alias
         * is first looked from cache. If cached alias is not found new will be generated and set to
         * cache for later usage.
         *
         * @param join Instance of {@link Join} to get alias for.
         * @return String value of alias for join.
         *
         * @since 1.2.0-SNAPSHOT
         */
        public static String forJoin(Join join) {

            /*
             * If alias is empty and it does not exist in cache add new one.
             */
            if (StringUtils.isBlank(join.getAlias())) {

                return forModel(join.getModel());
            } else {
                String alias = join.getAlias();
//                aliasMap.put(join.getModel(), alias);

                return alias;
            }
        }

        /**
         * Get alias for model class. Model class must be a database entity. Alias is first looked
         * from cache. If cached alias is not found new will be generated and set to
         * cache for later usage.
         *
         * @param model Instance of {@link Class} of entity to get alias for.
         * @return String value of alias for entity.
         *
         * @since 1.2.0-SNAPSHOT
         */
        public static String forModel(Class<?> model) {
            String alias = aliasMap.get(model);

            /*
             * If alias is not cached generate new and place it to cache.
             */
            if (StringUtils.isBlank(alias)) {
                alias = generateAlias(resolveName(model));
                aliasMap.put(model, alias);
            }

            return alias;
        }

        /**
         * Generate new unique alias for given table name of database table.
         * @param tableName String name of database table.
         * @return Newly generated alias.
         *
         * @since 1.2.0-SNAPSHOT
         */
        static String generateAlias(String tableName) {
            StringBuilder aliasBuilder = new StringBuilder(String.valueOf(tableName.charAt(0)));
            int index;
            while ((index = tableName.indexOf("_")) > -1) {
                aliasBuilder.append(tableName.charAt(index + 1));
                tableName = tableName.substring(index + 1);
            }

            return aliasBuilder.append(String.valueOf(count.incrementAndGet())).toString();
        }

        /**
         * Clear alias cache. If done, it should not be done during the query building process in order
         * to avoid broken queries.
         *
         * @since 1.2.0-SNAPSHOT
         */
        static void clearCache() {
            if (aliasMap != null) {
                aliasMap.clear();
            }
            if (count != null) {
                count.set(0);
            }
        }

    }
}
