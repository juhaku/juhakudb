package db.juhaku.juhakudb.filter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import db.juhaku.juhakudb.filter.Predicate.Conjunction;
import db.juhaku.juhakudb.filter.Predicate.Disjunction;
import db.juhaku.juhakudb.filter.Predicate.Junction;


/**
 * Created by juha on 03/05/17.
 *
 * <p>This class provides builder functions to create WHERE clause of a SQL query.</p>
 *
 * @author Juha Kukkonen
 *
 * @since
 */
public class PredicateBuilder {

    private List<Predicate> predicates;
    private List<Sort> orders;
    private Integer pageSize;
    private Integer page;

    /**
     * Instantiate new instance of predicate builder for creating advanced WHERE clauses.
     *
     * @since
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
     * @since
     */
    List<Predicate> getPredicates() {
        return predicates;
    }

    /**
     * Get order by clause for SQL.
     *
     * @return String value of order by.
     *
     * @since
     */
    String getSort() {
        return makeSort();
    }

    /**
     * Get limit clause for SQL.
     *
     * @return String value of limit clause.
     *
     * @since
     */
    String getPage() {
        return makePage();
    }

    /**
     * Add AND to the SQL.
     *
     * @return Predicate builder for current WHERE clause.
     *
     * @since
     */
    public PredicateBuilder and() {
        getPredicates().add(Predicate.and());

        return this;
    }

    /**
     * Add OR to the SQL.
     *
     * @return Predicate builder for current WHERE clause.
     *
     * @since
     */
    public PredicateBuilder or() {
        getPredicates().add(Predicate.or());

        return this;
    }

    /**
     * Add sorting for given columns by {@link Order}.
     *
     * @return Predicate builder for current WHERE clause.
     *
     * @since
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
     * @since
     */
    public PredicateBuilder setPageSize(Integer pageSize) {
        this.pageSize = pageSize;

        return this;
    }

    /**
     * Set current page number for sql query. If this is being used the page size also must be
     * provided. See {@link #setPageSize(Integer)}.
     *
     * @param page Integer current page.
     *
     * @return Predicate builder for current WHERE clause.
     *
     * @since
     */
    public PredicateBuilder setPage(Integer page) {
        this.page = page;

        return this;
    }

    /**
     * Creates IN statement for WHERE clause. This is equal to {@link #in(String, Collection)} but
     * it is provided as easy access for arrays.
     *
     * @param column String value of column to create in for.
     * @param args Array of args that are being substituted with question marks (?).
     *
     * @return Predicate builder for current WHERE clause.
     *
     * @since
     */
    public PredicateBuilder in(String column, Object... args) {
        getPredicates().add(Predicate.in(column, args));

        return this;
    }

    /**
     * Creates IN statement for WHERE clause. E.g. id IN (?,?).
     *
     * @param field String field to create in statement for.
     * @param args Collection containing args that will be replaced by question marks (?) in query.
     *
     * @return Predicate builder for current WHERE clause.
     *
     * @since
     */
    public PredicateBuilder in(String field, Collection<Object> args) {
        getPredicates().add(Predicate.in(field, args));

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
     * @since
     */
    public PredicateBuilder eq(String field, Object arg) {
        getPredicates().add(Predicate.eq(field, arg));

        return this;
    }

    /**
     * Create inverse statement to WHERE clause. E.g. {@code builder.not(builder.in(name, "john", "matt"));}
     *
     * <p>This above statement will be translated to: NOT IN (?,?) instead of IN (?,?).</p>
     *
     * @param statement Predicate builder of which latest statement is being inverted.
     *
     * @return Predicate builder for current WHERE clause.
     *
     * @since
     */
    public PredicateBuilder not(PredicateBuilder statement) {
        // Inverse last statement in predicate builder.
        getPredicates().add(Predicate.not(statement.getPredicates().get(statement.getPredicates().size() - 1)));

        return this;
    }

    /**
     * Creates is null statement for given field. E.g. name IS NULL.
     *
     * @param field String field to be checked is being null or not.
     *
     * @return Predicate builder for current WHERE clause.
     *
     * @since
     */
    public PredicateBuilder isNull(String field) {
        getPredicates().add(Predicate.isNull(field));

        return this;
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
     * @since
     */
    public PredicateBuilder between(String field, Object arg0, Object arg1) {
        getPredicates().add(Predicate.between(field, arg0, arg1));

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
     * @since
     */
    public PredicateBuilder gt(String field, Object arg) {
        getPredicates().add(Predicate.gt(field, arg));

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
     * @since
     */
    public PredicateBuilder ge(String field, Object arg) {
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
     * @since
     */
    public PredicateBuilder lt(String field, Object arg) {
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
     * @since
     */
    public PredicateBuilder le(String field, Object arg) {
        getPredicates().add(Predicate.le(field, arg));

        return this;
    }

    /**
     * Creates like statement for provided field. E.g. field LIKE ?. Like statement can contain
     * wild carts in the beginning and/or at the end of provided argument value.
     *
     * @param field String name of the field to create like statement for.
     * @param arg Object argument to be substituted with ?.
     *
     * @return Predicate builder for current WHERE clause.
     *
     * @since
     */
    public PredicateBuilder like(String field, Object arg) {
        getPredicates().add(Predicate.like(field, arg));

        return this;
    }

    public JunctionBuilder conjunction() {
        Conjunction conjunction = Predicate.conjunction();
        getPredicates().add(conjunction);

        return new JunctionBuilder(conjunction);
    }

    public JunctionBuilder disjunction() {
        Disjunction disjunction = Predicate.disjunction();
        getPredicates().add(disjunction);

        return new JunctionBuilder(disjunction);
    }

    /**
     * Generate order by statement for WHERE clause.
     *
     * @return String containing order by statement.
     *
     * @since
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
     * @since
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
     * @since
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
     * @since
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
