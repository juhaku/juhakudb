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
package db.juhaku.juhakudb.filter;

import java.lang.reflect.Array;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import db.juhaku.juhakudb.util.StringUtils;

/**
 * Created by juha on 16/04/16.
 *
 * @author juha
 */
public class Predicate {

    private String in;
    private String eq;
    private String is;
    private String between;
    private String like;
    private String clause;

    private Object[] args;

    /**
     * Describe parameter placeholder for SQL queries. Typically "?".
     */
    static final String PARAM_PLACE_HOLDER = "?";
    static final String PARAM_EQUALS = " = ";
    static final String PARAM_NOT_EQUAL = " != ";


    private Predicate() {
        // Not instantiatable.
    }

    private void addArgs(Object... args) {
        this.args = args;
    }

    String[] getArgs() {
        if (args == null) {
            args = new Object[0];
        }

        String[] stringArgs = new String[0];
        for (Object arg : args) {
            int len = stringArgs.length;
            String[] newArgs = new String[len + 1];
            System.arraycopy(stringArgs, 0, newArgs, 0, stringArgs.length);
            stringArgs = newArgs;

            // Add custom processing for date formats.
            if (Date.class.isAssignableFrom(arg.getClass())) {
                stringArgs[len] = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(arg);

            } else {
                stringArgs[len] = arg.toString();
            }
        }

        return stringArgs;
    }

    /**
     * Get generated clause for this predicate. E.g. id = ?.
     *
     * @return String value of clause.
     */
    String getClause() {
        if (!StringUtils.isBlank(in)) {
            return in;

        } else if (!StringUtils.isBlank(eq)) {
            return eq;

        } else if (!StringUtils.isBlank(is)) {
            return is;

        } else if (!StringUtils.isBlank(between)) {
            return between;

        } else if (!StringUtils.isBlank(like)) {
            return like;

        } else if (!StringUtils.isBlank(clause)) {
            return clause;
        }

        return null;
    }

    static Predicate in(String field, Object... args) {
        Predicate predicate = new Predicate();
        StringBuilder inBuilder = new StringBuilder(field);
        inBuilder.append(" IN (");
        for (Object arg : args) {
            inBuilder.append(PARAM_PLACE_HOLDER);
            if (arg != args[args.length - 1]) {
                inBuilder.append(", ");
            }
        }
        inBuilder.append(")");
        predicate.in = inBuilder.toString();
        predicate.addArgs(args);

        return predicate;
    }

    static Predicate eq(String field, Object arg) {
        return operatorPredicate(field, arg, PARAM_EQUALS);
    }

    static Predicate not(Predicate predicate) {
        if (!StringUtils.isBlank(predicate.in)) {
            predicate.in = predicate.in.replace("IN", "NOT IN");

            return predicate;
        } else if (!StringUtils.isBlank(predicate.eq) && predicate.eq.contains(PARAM_EQUALS)) {
            predicate.eq = predicate.eq.replace(PARAM_EQUALS, PARAM_NOT_EQUAL);

            return predicate;
        } else if (!StringUtils.isBlank(predicate.is)) {
            predicate.is = predicate.is.replace("IS NULL", "IS NOT NULL");

            return predicate;
        } else if (!StringUtils.isBlank(predicate.between)) {
            predicate.between = predicate.between.replace("BETWEEN", "NOT BETWEEN");

            return predicate;
        } else if (!StringUtils.isBlank(predicate.like)) {
            predicate.like = " NOT".concat(predicate.like);

            return predicate;
        } else {
            return null;
        }
    }

    static Predicate isNull(String field) {
        Predicate predicate = new Predicate();
        predicate.is = field.concat(" IS NULL");

        return predicate;
    }

    static Predicate between(String field, Object arg0, Object arg1) {
        Predicate predicate = new Predicate();

        StringBuilder between = new StringBuilder(field);
        between.append(" BETWEEN ").append(PARAM_PLACE_HOLDER).append(" AND ")
                .append(PARAM_PLACE_HOLDER);

        predicate.between = between.toString();
        predicate.addArgs(arg0, arg1);

        return predicate;
    }

    static Predicate gt(String field, Object arg) {
        return operatorPredicate(field, arg, " > ");
    }

    static Predicate ge(String field, Object arg) {
        return operatorPredicate(field, arg, " >= ");
    }

    static Predicate lt(String field, Object arg) {
        return operatorPredicate(field, arg, " < ");
    }

    static Predicate le(String field, Object arg) {
        return operatorPredicate(field, arg, " <= ");
    }

    static Predicate like(String field, Object arg) {
        Predicate predicate = new Predicate();

        StringBuilder like = new StringBuilder(field);
        like.append(" LIKE ").append(PARAM_PLACE_HOLDER);

        predicate.like = like.toString();
        predicate.addArgs(arg);

        return predicate;
    }

    static Predicate sqlPredicate(String sql, Object... args) {
        Predicate predicate = new Predicate();
        predicate.clause = sql;
        predicate.addArgs(args);

        return predicate;
    }

    static Junction disjunction() {
        return new Junction(BooleanOperator.OR);
    }

    static Junction conjunction() {
        return new Junction(BooleanOperator.AND);
    }

    private static Predicate operatorPredicate(String field, Object arg, String operator) {
        Predicate predicate = new Predicate();

        StringBuilder eqBuilder = new StringBuilder();
        eqBuilder.append(field).append(operator).append(PARAM_PLACE_HOLDER);
        predicate.eq = eqBuilder.toString();
        predicate.addArgs(arg);

        return predicate;
    }

    /**
     * Check whether given value is symbol used by sql queries.
     *
     * @param value String value to check.
     * @return boolean true if given value is symbol; false otherwise.
     *
     * @since 1.2.0
     */
    static boolean isSymbol(String value) {
        return value.equals(">") || value.equals("<") || value.equals(">=") || value.equals("<=")
                || value.equals(PARAM_PLACE_HOLDER) || value.equals(PARAM_NOT_EQUAL.trim())
                || value.equals(PARAM_EQUALS.trim()) || value.equals("*");
    }

    /**
     * Junction is parentheses grouped clause with one or more arguments. Junction can either
     * be disjunction or conjunction. This is handled via {@link BooleanOperator} what defines
     * type of junction.<br><br>
     *
     * E.g. (a AND b AND c, ...) or (a OR b OR c, ...).
     *
     * @since 2.1.0
     */
    static class Junction extends Predicate {

        private List<Predicate> predicates;
        private BooleanOperator operator;

        Junction(BooleanOperator operator) {
            this.operator = operator;
            this.predicates = new ArrayList<>();
        }

        /**
         * Get current junction operator used to combine clauses.
         *
         * @return {@link BooleanOperator} what is either AND or OR.
         *
         * @since 2.1.0
         */
        BooleanOperator getOperator() {
            return operator;
        }

        /**
         * Get clauses added to this junction. These clauses will be combined to sql separated with
         * operator provided to this junction. See {@link #getOperator()}.
         *
         * @return List of predicates of this junction.
         *
         * @since 2.1.0
         */
        List<Predicate> getPredicates() {
            return predicates;
        }
    }

    /**
     * Predicate boolean operator is used in SQL queries to combine search criteria.
     *
     * @since 2.1.0
     */
    enum BooleanOperator {

        AND(" AND "), OR(" OR ");

        private String value;

        BooleanOperator(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

}
