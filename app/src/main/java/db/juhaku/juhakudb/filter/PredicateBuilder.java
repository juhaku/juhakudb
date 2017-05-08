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
package db.juhaku.juhakudb.filter;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import db.juhaku.juhakudb.filter.Predicate.Junction;


/**
 * Created by juha on 03/05/17.
 *
 * <p>This class provides builder functions to create WHERE clause of a SQL query.</p>
 *
 * @author Juha Kukkonen
 *
 * @since 2.0.2-SNAPSHOT
 */
public class PredicateBuilder {

    private List<Predicate> predicates;
    private List<Sort> orders;
    private Integer pageSize;
    private Integer page;
    private boolean not;

    /**
     * Instantiate new instance of predicate builder for creating advanced WHERE clauses.
     *
     * @since 2.0.2-SNAPSHOT
     */
    public PredicateBuilder() {
        this.predicates = new ArrayList<>();
        this.orders = new ArrayList<>();
    }

    /**
     * Get built predicates.
     *
     * @return List of {@link Predicate}.
     *
     * @since 2.0.2-SNAPSHOT
     */
    List<Predicate> getPredicates() {
        return predicates;
    }

    /**
     * Get order by clause for SQL.
     *
     * @return String value of order by.
     *
     * @since 2.0.2-SNAPSHOT
     */
    String getSort() {
        return makeSort();
    }

    /**
     * Get limit clause for SQL.
     *
     * @return String value of limit clause.
     *
     * @since 2.0.2-SNAPSHOT
     */
    String getPage() {
        return makePage();
    }

//    /**
//     * Add AND to the SQL.
//     *
//     * @return Predicate builder for current WHERE clause.
//     *
//     * @since 2.0.2-SNAPSHOT
//     */
//    public PredicateBuilder and() {
//        getPredicates().add(Predicate.and());
//
//        return this;
//    }
//
//    /**
//     * Add OR to the SQL.
//     *
//     * @return Predicate builder for current WHERE clause.
//     *
//     * @since 2.0.2-SNAPSHOT
//     */
//    public PredicateBuilder or() {
//        getPredicates().add(Predicate.or());
//
//        return this;
//    }

    /**
     * Add sorting for given columns by {@link Order}.
     *
     * @return Predicate builder for current WHERE clause.
     *
     * @since 2.0.2-SNAPSHOT
     */
    public PredicateBuilder sort(Order order, String... cols) {
        for (String col : cols) {
            this.orders.add(new Sort(col, order));
        }

        return this;
    }

    /**
     * Set number of max results in one page. If this is filled then set page must be provided also.
     * See {@link #setPage(Integer)}.
     *
     * @param pageSize Integer value of max page result.
     *
     * @return Predicate builder for current WHERE clause.
     *
     * @since 2.0.2-SNAPSHOT
     */
    public PredicateBuilder setPageSize(Integer pageSize) {
        this.pageSize = pageSize;

        return this;
    }

    /**
     * Set current page number for sql query. 0 will be the first page. If this is being used
     * the page size also must be provided. See {@link #setPageSize(Integer)}.
     *
     * @param page Integer current page.
     *
     * @return Predicate builder for current WHERE clause.
     *
     * @since 2.0.2-SNAPSHOT
     */
    public PredicateBuilder setPage(Integer page) {
        this.page = page;

        return this;
    }

    /**
     * Creates IN statement for WHERE clause. This is equal to {@link #in(String, Collection)} but
     * it is provided as easy access for arrays.
     *
     * @param field String value of field to create in for.
     * @param args Array of args that are being substituted with question marks (?).
     *
     * @return Predicate builder for current WHERE clause.
     *
     * @since 2.0.2-SNAPSHOT
     */
    public PredicateBuilder in(String field, Object... args) {
        return in(Expression.of(field), Expression.ofCollection(Arrays.asList(args)));
    }

    /**
     * Creates IN statement for WHERE clause. E.g. id IN (?,?).
     *
     * @param field String field to create in statement for.
     * @param args Collection containing args that will be replaced by question marks (?) in query.
     *
     * @return Predicate builder for current WHERE clause.
     *
     * @since 2.0.2-SNAPSHOT
     */
    public PredicateBuilder in(String field, Collection<Object> args) {
        return in(Expression.of(field), Expression.ofCollection(args));
    }

    /**
     * Creates IN statement for WHERE clause with expressions. E.g. lower(name) IN (lower(?),lower(?)).
     * This method is same as {@link #in(Expression, Expression...)} but is provided as easy access api.
     *
     * @param field Expression to use for first value before IN word.
     * @param args Collection containing args that will be replaced by question marks (?) in query.
     *
     * @return Predicate builder for current WHERE clause.
     *
     * @since 2.0.2-SNAPSHOT
     */
    public PredicateBuilder in(Expression field, Collection<Expression> args) {
        return in(field, convertCollectionToArray(args));
    }

    /**
     * Creates IN statement for WHERE clause with expressions. E.g. lower(name) IN (lower(?),lower(?)).
     *
     * @param field Expression to use for first value before IN word.
     * @param args Collection containing args that will be replaced by question marks (?) in query.
     *
     * @return Predicate builder for current WHERE clause.
     *
     * @since 2.0.2-SNAPSHOT
     */
    public PredicateBuilder in(Expression field, Expression... args) {
        getPredicates().add(Predicate.in(field, args));

        negateIfNecessary();

        return this;
    }

    /**
     * Create equals statement for WHERE clause. E.g. name = ?.
     *
     * @param field String field name to create equals statement for.
     * @param arg Object arg to be substituted with ?.
     *
     * @return Predicate builder for current WHERE clause.
     *
     * @since 2.0.2-SNAPSHOT
     */
    public PredicateBuilder eq(String field, Object arg) {
        return eq(Expression.of(field), Expression.of(arg));
    }

    /**
     * Create equals statement for WHERE clause by expressions. E.g. name = ?. Expression equals
     * statement enables possibility to create more robust equals statements e.g. upper(name) = upper(?).
     *
     * @param field Expression for field to be equal to something.
     * @param arg Expression that is being compared to against the fieldExpression. Value
     *                      will be substituted with ? for SQL query.
     *
     * @return Predicate builder for current WHERE clause.
     *
     * @since 2.0.2-SNAPSHOT
     */
    public PredicateBuilder eq(Expression field, Expression arg) {
        getPredicates().add(Predicate.eq(field, arg));

        negateIfNecessary();

        return this;
    }

    /**
     * Create inverse statement to WHERE clause. Negation works with in, eq, between, isNull and like
     * statements. E.g. {@code builder.not().in(name, "john", "matt");}
     *
     * <p>This above statement will be translated to: NOT IN (?,?) instead of IN (?,?).</p>
     *
     * @return Predicate builder for current WHERE clause.
     *
     * @since 2.0.2-SNAPSHOT
     */
    public PredicateBuilder not() {
        not = true; //Mark next statement to be negated.

        return this;
    }

    /**
     * Creates is null statement for given field. E.g. name IS NULL.
     *
     * @param field String field to be checked is being null or not.
     *
     * @return Predicate builder for current WHERE clause.
     *
     * @since 2.0.2-SNAPSHOT
     */
    public PredicateBuilder isNull(String field) {
        getPredicates().add(Predicate.isNull(field));

        negateIfNecessary();

        return this;
    }

    /**
     * Negates latest predicate if necessary. If not() was called before statement then the
     * statement will be negated. Negation works with in, eq, between, isNull and like statements.
     *
     * @since 2.0.2-SNAPSHOT
     *
     * @hide
     */
    private void negateIfNecessary() {
        if (not) {
            not = false;
            Predicate.not(getPredicates().get(getPredicates().size() - 1));
        }
    }

    /**
     * Creates between statement for given field where field values is between some values. E.g.
     * age BETWEEN ? AND ?.
     *
     * @param field String field name to create between statement for.
     * @param arg0 Object first argument for between statement.
     * @param arg1 Object second argument for between statement.
     *
     * @return Predicate builder for current WHERE clause.
     *
     * @since 2.0.2-SNAPSHOT
     */
    public PredicateBuilder between(String field, Object arg0, Object arg1) {
        return between(Expression.of(field), Expression.of(arg0), Expression.of(arg1));
    }

    /**
     * Creates between statement by expression for given field where field values is between some values. E.g.
     * age BETWEEN ? AND ?. Expression provides possibility to create more robust statements. E.g.
     * sum(age) BETWEEN ? AND ?.
     *
     * @param field String field name to create between statement for.
     * @param arg0 Object first argument for between statement.
     * @param arg1 Object second argument for between statement.
     *
     * @return Predicate builder for current WHERE clause.
     *
     * @since 2.0.2-SNAPSHOT
     */
    public PredicateBuilder between(Expression field, Expression arg0, Expression arg1) {
        getPredicates().add(Predicate.between(field, arg0, arg1));

        negateIfNecessary();

        return this;
    }

    /**
     * Creates greater than statement for given field. E.g. age > ?.
     *
     * @param field String name of the field to create statement for.
     * @param arg Object argument to be substituted with ?.
     *
     * @return Predicate builder for current WHERE clause.
     *
     * @since 2.0.2-SNAPSHOT
     */
    public PredicateBuilder gt(String field, Object arg) {
        return gt(Expression.of(field), Expression.of(arg));
    }

    /**
     * Create greater than for WHERE clause by expressions. E.g. age > ?. Expression greater than
     * statement enables possibility to create more robust greater than statements e.g. max(age) > avg(?).
     *
     * @param field Expression for field to be greater than something.
     * @param arg Expression that is being compared to against the fieldExpression. Value
     *                      will be substituted with ? for SQL query.
     *
     * @return Predicate builder for current WHERE clause.
     *
     * @since 2.0.2-SNAPSHOT
     */
    public PredicateBuilder gt(Expression field, Expression arg) {
        getPredicates().add(Predicate.ge(field, arg));

        return this;
    }

    /**
     * Creates greater than or equal statement for given field. E.g. age >= ?.
     *
     * @param field String name of the field to create statement for.
     * @param arg Object argument to be substituted with ?.
     *
     * @return Predicate builder for current WHERE clause.
     *
     * @since 2.0.2-SNAPSHOT
     */
    public PredicateBuilder ge(String field, Object arg) {
        return ge(Expression.of(field), Expression.of(arg));
    }

    /**
     * Create greater than or equal for WHERE clause by expressions. E.g. age >= ?. Expression ge
     * statement enables possibility to create more robust greater than or equal statements e.g. max(age) >= avg(?).
     *
     * @param field Expression for field to be greater than or equal to something.
     * @param arg Expression that is being compared to against the fieldExpression. Value
     *                      will be substituted with ? for SQL query.
     *
     * @return Predicate builder for current WHERE clause.
     *
     * @since 2.0.2-SNAPSHOT
     */
    public PredicateBuilder ge(Expression field, Expression arg) {
        getPredicates().add(Predicate.ge(field, arg));

        return this;
    }

    /**
     * Creates less than statement for given field. E.g. age < ?.
     *
     * @param field String name of the field to create statement for.
     * @param arg Object argument to be substituted with ?.
     *
     * @return Predicate builder for current WHERE clause.
     *
     * @since 2.0.2-SNAPSHOT
     */
    public PredicateBuilder lt(String field, Object arg) {
        return lt(Expression.of(field), Expression.of(arg));
    }

    /**
     * Create less than for WHERE clause by expressions. E.g. age >= ?. Expression lt
     * statement enables possibility to create more robust less than statements e.g. min(width) < avg(?).
     *
     * @param field Expression for field to be less than something.
     * @param arg Expression that is being compared to against the fieldExpression. Value
     *                      will be substituted with ? for SQL query.
     *
     * @return Predicate builder for current WHERE clause.
     *
     * @since 2.0.2-SNAPSHOT
     */
    public PredicateBuilder lt(Expression field, Expression arg) {
        getPredicates().add(Predicate.lt(field, arg));

        return this;
    }

    /**
     * Creates less than or equal statement for given field. E.g. age <= ?.
     *
     * @param field String name of the field to create statement for.
     * @param arg Object argument to be substituted with ?.
     *
     * @return Predicate builder for current WHERE clause.
     *
     * @since 2.0.2-SNAPSHOT
     */
    public PredicateBuilder le(String field, Object arg) {
        return le(Expression.of(field), Expression.of(arg));
    }

    /**
     * Create less than or equal for WHERE clause by expressions. E.g. age <= ?. Expression le
     * statement enables possibility to create more robust less than or equal statements e.g. min(width) <= avg(?).
     *
     * @param field Expression for field to be less than or equal to something.
     * @param arg Expression that is being compared to against the fieldExpression. Value
     *                      will be substituted with ? for SQL query.
     *
     * @return Predicate builder for current WHERE clause.
     *
     * @since 2.0.2-SNAPSHOT
     */
    public PredicateBuilder le(Expression field, Expression arg) {
        getPredicates().add(Predicate.le(field, arg));

        return this;
    }

    /**
     * Creates like statement for provided field. E.g. field LIKE ?. Like statement can contain
     * wild carts in the beginning and/or at the end of provided argument value.
     *
     * <p>By default like statement is case insensitive.</p>
     *
     * @param field String name of the field to create like statement for.
     * @param arg Object argument to be substituted with ?.
     *
     * @return Predicate builder for current WHERE clause.
     *
     * @since 2.0.2-SNAPSHOT
     */
    public PredicateBuilder like(String field, Object arg) {
        return like(Expression.of(field), arg);
    }

    /**
     * Creates like statement for provided field expression. E.g. trim(field) LIKE ?. Like statement can contain
     * wild carts in the beginning and/or at the end of provided argument value.
     *
     * <p>By default like statement is case insensitive.</p>
     *
     * @param field String name of the field to create like statement for.
     * @param arg Object argument to be substituted with ?.
     *
     * @return Predicate builder for current WHERE clause.
     *
     * @since 2.0.2-SNAPSHOT
     */
    public PredicateBuilder like(Expression field, Object arg) {
        getPredicates().add(Predicate.like(field, arg));

        negateIfNecessary();

        return this;
    }

    /**
     * Create min statement for provided field. E.g. MIN(age).
     *
     * @param field String field name to create statement for.
     *
     * @return Predicate builder for current WHERE clause.
     *
     * @since 2.0.2-SNAPSHOT
     */
    public PredicateBuilder min(String field) {
        getPredicates().add(Predicate.forExpression(Expression.min(field)));

        return this;
    }

    /**
     * Create max statement for provided field. E.g. MAX(age).
     *
     * @param field String field name to create statement for.
     *
     * @return Predicate builder for current WHERE clause.
     *
     * @since 2.0.2-SNAPSHOT
     */
    public PredicateBuilder max(String field) {
        getPredicates().add(Predicate.forExpression(Expression.max(field)));

        return this;
    }

    /**
     * Create avg statement for provided field. E.g. AVG(age).
     *
     * @param field String field name to create statement for.
     *
     * @return Predicate builder for current WHERE clause.
     *
     * @since 2.0.2-SNAPSHOT
     */
    public PredicateBuilder avg(String field) {
        getPredicates().add(Predicate.forExpression(Expression.avg(field)));

        return this;
    }

    /**
     * Create sum statement for provided field. E.g. SUM(age).
     *
     * @param field String field name to create statement for.
     *
     * @return Predicate builder for current WHERE clause.
     *
     * @since 2.0.2-SNAPSHOT
     */
    public PredicateBuilder sum(String field) {
        getPredicates().add(Predicate.forExpression(Expression.sum(field)));

        return this;
    }

    /**
     * Create count statement for provided field. E.g. COUNT(age).
     *
     * @param field String field name to create statement for.
     *
     * @return Predicate builder for current WHERE clause.
     *
     * @since 2.0.2-SNAPSHOT
     */
    public PredicateBuilder count(String field) {
        getPredicates().add(Predicate.forExpression(Expression.count(field)));

        return this;
    }

    /**
     * Create count statement for all fields which will result following statement COUNT(*).
     *
     * @return Predicate builder for current WHERE clause.
     *
     * @since 2.0.2-SNAPSHOT
     */
    public PredicateBuilder count() {
        return count("*");
    }

    public PredicateBuilder sqlPredicate(String sql) {
        return sqlPredicate(sql, (Object[]) null);
    }

    public PredicateBuilder sqlPredicate(String sql, Object... args) {
        getPredicates().add(Predicate.sqlPredicate(sql, args));

        return this;
    }

    public PredicateBuilder sqlPredicate(String sql, Collection<Object> args) {
        return sqlPredicate(sql, convertCollectionToArray(args));
    }

    /**
     * Converts collection of object to Object array.
     *
     * @param args Collection of object to convert.
     *
     * @return Array of same objects that were given in collection.
     *
     * @since 2.0.2-SNAPSHOT
     *
     * @hide
     */
    private static <T> T[] convertCollectionToArray(Collection<T> args) {
        return args.toArray((T[]) Array.newInstance(args.iterator().next().getClass(), args.size()));
    }

    /**
     * Creates new conjunction for current WHERE clause. Conjunction stands for grouped
     * statement which is isolated with parentheses. E.g. (a AND b AND c...). This statement
     * can contain only AND operators. Conjunction cannot contain another junction instead all
     * junctions is to be added to root predicate builder for root WHERE clause.
     *
     * @return New junction builder for current WHERE clause to create isolated criteria for query.
     *
     * @since 2.0.2-SNAPSHOT
     */
    public JunctionBuilder conjunction() {
        Junction junction = Predicate.conjunction();
        getPredicates().add(junction);

        return new JunctionBuilder(junction);
    }

    /**
     * Creates new disjunction for current WHERE clause. Disjunction stands for grouped
     * statement which is isolated with parentheses. E.g. (a OR b OR c...). This statement
     * can contain only OR operators. Disjunction cannot contain another junction instead all
     * junctions is to be added to root predicate builder for root WHERE clause.
     *
     * @return New junction builder for current WHERE clause to create isolated criteria for query.
     *
     * @since 2.0.2-SNAPSHOT
     */
    public JunctionBuilder disjunction() {
        Junction junction = Predicate.disjunction();
        getPredicates().add(junction);

        return new JunctionBuilder(junction);
    }
    /**
     * Generate order by statement for WHERE clause.
     *
     * @return String containing order by statement.
     *
     * @since 2.0.2-SNAPSHOT
     *
     * @hide
     */
    private String makeSort() {
        if (orders.isEmpty()) { // Sanity check.
            return null;
        }

        String[] asc = new String[0];
        String[] desc = new String[0];
        for (Sort sort : orders) {
            if (sort.order == Order.ASC) {
                int len = asc.length;
                String[] newAsc = new String[len + 1];
                System.arraycopy(asc, 0, newAsc, 0, len);
                asc = newAsc;
                asc[len] = sort.sort;
            } else {
                int len = desc.length;
                String[] newDesc = new String[len + 1];
                System.arraycopy(desc, 0, newDesc, 0, len);
                desc = newDesc;
                desc[len] = sort.sort;
            }
        }
        StringBuilder sortBuilder = new StringBuilder(" ORDER BY ");
        if (asc.length > 0) {
            sortBuilder.append(orderBy(Order.ASC, asc));
            if (desc.length > 0) {
                sortBuilder.append(", ");
            }
        }
        if (desc.length > 0) {
            sortBuilder.append(orderBy(Order.DESC, desc));
        }
        orders.clear();

        return sortBuilder.toString();
    }

    /**
     * Helper to create order by according order for given columns.
     *
     * @param order Instance of {@link Order} to use for columns.
     * @param cols String array of columns that is being used as order.
     *
     * @return String for order by statement without order by.
     *
     * @since 2.0.2-SNAPSHOT
     *
     * @hide
     */
    private String orderBy(Order order, String... cols) {
        StringBuilder orderBuilder = new StringBuilder();
        for (String col : cols) {
            orderBuilder.append(col);
            if (!col.equals(cols[cols.length - 1])) {
                orderBuilder.append(", ");
            }
        }
        orderBuilder.append(" ").append(order.toString());
        return orderBuilder.toString();
    }

    /**
     * Generate limit statement for WHERE clause according page size and current page.
     *
     * @return String containing limit clause.
     *
     * @since 2.0.2-SNAPSHOT
     *
     * @hide
     */
    private String makePage() {
        StringBuilder page = new StringBuilder(" LIMIT ");
        if (pageSize != null && pageSize > 0) {
            page.append(String.valueOf(pageSize));
            if (this.page != null) {
                page.append(" OFFSET ").append(String.valueOf((this.page * pageSize)));
            }

            return page.toString();
        }

        return null;
    }

    /**
     * Sort class is wrapper to wrap columns and order together for order by statement.
     *
     * @since 2.0.2-SNAPSHOT
     *
     * @hide
     */
    private static class Sort {
        private String sort;
        private Order order;

        public Sort(String sort, Order order) {
            this.sort = sort;
            this.order = order;
        }
    }

    /**
     * Junction builder provides isolated operator clause for WHERE clause. These can be e.g.
     * (A and B and C...) or (A or B or C...).
     *
     * <p>This is handy for grouping some parts of criteria with parentheses.</p>
     *
     * <p>Junction cannot contain another junction instead it is to be added to root predicate
     * builder.</p>
     *
     * @since 2.0.2-SNAPSHOT
     */
    public static class JunctionBuilder extends PredicateBuilder {

        private Junction junction;

        public JunctionBuilder(Junction junction) {
            this.junction = junction;
        }

        @Override
        List<Predicate> getPredicates() {
            return junction.getPredicates();
        }

        @Override
        public JunctionBuilder conjunction() {
            throw new UnsupportedOperationException("Unsupported operation to add conjunction in junction");
        }

        @Override
        public JunctionBuilder disjunction() {
            throw new UnsupportedOperationException("Unsupported operation to add disjunction in junction");
        }
    }
}
