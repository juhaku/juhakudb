package db.juhaku.juhakudb.filter;

import java.util.List;
import java.util.StringTokenizer;

import db.juhaku.juhakudb.core.NameResolver;
import db.juhaku.juhakudb.core.schema.Schema;
import db.juhaku.juhakudb.exception.NameResolveException;
import db.juhaku.juhakudb.exception.QueryBuildException;
import db.juhaku.juhakudb.filter.Predicate.Conjunction;
import db.juhaku.juhakudb.filter.Predicate.Disjunction;
import db.juhaku.juhakudb.filter.Predicate.Junction;
import db.juhaku.juhakudb.util.ReservedWords;
import db.juhaku.juhakudb.util.StringUtils;

/**
 * Created by juha on 19/04/16.
 *
 * @author juha
 */
public class QueryCreator {

    private Schema schema;
    private AssociationProcessor processor;

    public QueryCreator(Schema schema) {
        this.schema = schema;
        this.processor = new AssociationProcessor(schema);
    }

    public <T> Query create(Class<?> clazz, Filter filter) {
        processor.setRootEntity(clazz);
        String currentTableName;
        try {
            currentTableName = NameResolver.resolveName(clazz);
        } catch (NameResolveException e) {
            throw new QueryBuildException("Failed to create query for entity: " + clazz, e);
        }
        Root<T> root = new Root<>();
        Predicates predicates = new Predicates();
        StringBuilder sql = new StringBuilder(generateSelect(currentTableName));
        createAssociations(filter, sql, root, predicates);
        String[] args = new String[0];
        args = createWhere(args, sql, predicates, currentTableName);
        String order = predicates.getSort();
        String page = predicates.getPage();
        if (!StringUtils.isBlank(order)) {
            sql.append(order);
        }
        if (!StringUtils.isBlank(page)) {
            sql.append(page);
        }

        return new Query(sql.toString(), args);
    }

    private <T> void createAssociations(Filter filter, StringBuilder sql, Root<T> root, Predicates predicates) {
        filter.filter(root, predicates);
        processor.processAssosiations(root);

        for (Root<T> child : root.getJoins()) {
            sql.append(child.getJoin());
        }

        if (!predicates.getPredicates().isEmpty()) {
            sql.append("WHERE ");// used with raw query
        }
    }

    private String[] createWhere(String[] args, StringBuilder sql, Predicates predicates, String tableName) {
        for (Predicate child : predicates.getPredicates()) {
            if (child instanceof Junction) {
                sql.append("(");
                for (Predicate junction : ((Junction) child).getPredicates()) {
                    sql.append(formatClause(junction.getClause(), tableName));
                    if (child instanceof Disjunction
                            && !isLast(junction, ((Junction) child).getPredicates())) {
                        sql.append(" OR ");
                    } else if (child instanceof Conjunction
                            && !isLast(junction, ((Junction) child).getPredicates())) {
                        sql.append(" AND ");
                    }
                    args = addArgsToArray(args, junction.getArgs());
                }
                sql.append(")");
                if (!isLast(child, predicates.getPredicates())) {
                    sql.append(" AND ");
                }
            } else {
                sql.append(formatClause(child.getClause(), tableName));
                if (!isLast(child, predicates.getPredicates())) {
                    sql.append(" AND ");
                }
                args = addArgsToArray(args, child.getArgs());
            }
        }

        return args;
    }

    public <T> Query createWhereClause(Filter filter) {
        Root<T> root = new Root<>();
        Predicates predicates = new Predicates();
        filter.filter(root, predicates);
        String[] args = new String[0];
        StringBuilder sql = new StringBuilder();
        args = createWhere(args, sql, predicates, null);

        return new Query(sql.toString(), args);
    }

    private boolean isLast(Predicate predicate, List<Predicate> predicates) {
        return predicates.get(predicates.size() - 1) == predicate;
    }

    private String generateSelect(String tableName) {
        Schema table = schema.getElement(tableName);
        String alias = defaultAlias(tableName);
        StringBuilder select = new StringBuilder("SELECT ");
        int loop = 0;
        for (String column : table.getElements().keySet()) {
            select.append(alias).append(".").append(column);
            loop ++;
            if (loop != table.getElements().keySet().size()) {
                select.append(", ");
            }
        }
        select.append(" FROM ").append(tableName).append(" ").append(alias).append(" ");

        return select.toString();
    }

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
     *     <li>Formats clause's "this" prefixes as default alias used for the table if table name is provided.</li>
     *     <li>Formats empty prefixed columns with default alias used for the table if table name is provided.</li>
     *     <li>Formats .id fields as ._id since Android primary key field is prefixed with "_" underscore.</li>
     * </ul>
     *
     * @param clause String sql clause to format aliases.
     * @param tableName String value of table name to take alias from. Can be null if no formatting is required.
     * @return String value of clause formatted if table name was provided otherwise returns the original
     * clause.
     * @hide
     *
     * @since 1.1.2-SNAPSHOT
     */
    private String formatClause(String clause, String tableName) {
        String formatted = clause;

        // format "this" prefixes to use default alias
        if (tableName != null) {
            formatted = clause.replace("this", defaultAlias(tableName));
        }

        // format .id fields to match correct primary key by adding "_" underscore in front.
        formatted = formatted.replace(".id", "._id");

        StringTokenizer tokens = new StringTokenizer(formatted, " ");
        StringBuilder formattedBuilder = new StringBuilder();
        while (tokens.hasMoreElements()) {
            String token = tokens.nextToken();

            // check that token is not a reserved word nor symbol
            if (!token.equals(Predicate.PARAM_EQUALS.trim()) && !token.equals(Predicate.PARAM_NOT_EQUAL.trim())
                    && !token.equals(Predicate.PARAM_PLACE_HOLDER) && !ReservedWords.has(token)) {

                // if table name is provided and token does not contain alias separator "." add default alias
                if (!token.contains(".") && !token.startsWith("(") && !token.startsWith(Predicate.PARAM_PLACE_HOLDER) && tableName != null) {
                    formattedBuilder.append(defaultAlias(tableName).concat(".").concat(token));
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
     * Generate default alias for table by taking first character of table.
     * @param tableName String value of table name.
     * @return String default alias.
     * @hide
     * @since 1.1.2-SNAPSHOT
     */
    private String defaultAlias(String tableName) {
        return String.valueOf(tableName.charAt(0));
    }
}
