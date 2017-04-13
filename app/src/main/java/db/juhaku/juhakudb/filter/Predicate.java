package db.juhaku.juhakudb.filter;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
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

    private Object[] args;

    static final String PARAM_PLACE_HOLDER = "?";
    static final String PARAM_EQUALS = " = ";
    static final String PARAM_NOT_EQUAL = " != ";


    private Predicate() {
        // not intializable by outside world.
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
            stringArgs[len] = arg.toString();
        }

        return stringArgs;
    }

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
        }

        return null;
    }

    public static Predicate in(String field, Object... args) {
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

    public static Predicate in(String field, Collection<Object> args) {
        return in(field, args.toArray((Object[]) Array.newInstance(args.iterator().next().getClass(), args.size())));
    }

    public static Predicate eq(String field, Object arg) {
        return operatorPredicate(field, arg, PARAM_EQUALS);
    }

    public static Predicate not(Predicate predicate) {
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

    public static Predicate isNull(String field) {
        Predicate predicate = new Predicate();
        predicate.is = "IS NULL ".concat(field);

        return predicate;
    }

    public static Predicate between(String field, Object arg0, Object arg1) {
        Predicate predicate = new Predicate();

        StringBuilder between = new StringBuilder(field);
        between.append(" BETWEEN ").append(PARAM_PLACE_HOLDER).append(" AND ")
                .append(PARAM_PLACE_HOLDER);
        predicate.between = between.toString();
        predicate.addArgs(arg0, arg1);

        return predicate;
    }

    public static Predicate gt(String field, Object arg) {
        return operatorPredicate(field, arg, " > ");
    }

    public static Predicate ge(String field, Object arg) {
        return operatorPredicate(field, arg, " >= ");
    }

    public static Predicate lt(String field, Object arg) {
        return operatorPredicate(field, arg, " < ");
    }

    public static Predicate le(String field, Object arg) {
        return operatorPredicate(field, arg, " <= ");
    }

    public static Predicate like(String field, Object arg) {
        Predicate predicate = new Predicate();

        StringBuilder like = new StringBuilder(field);
        like.append(" LIKE ").append(PARAM_PLACE_HOLDER);

        predicate.like = like.toString();
        predicate.addArgs(arg);

        return predicate;
    }

//    public Predicate ilike(String field, Object arg) {
//        Predicate predicate = getLastPredicate(like(field, arg));
//        predicate.like = predicate.like.replace("LIKE", "ILIKE");
//
//        return predicate;
//    }

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
     * @since 1.2.0-SNAPSHOT
     */
    static boolean isSymbol(String value) {
        return value.equals(">") || value.equals("<") || value.equals(">=") || value.equals("<=")
                || value.equals(PARAM_PLACE_HOLDER) || value.equals(PARAM_NOT_EQUAL.trim())
                || value.equals(PARAM_EQUALS.trim());
    }

    public static Disjunction disjunction() {

        return new Disjunction();
    }

    public static Conjunction conjunction() {

        return new Conjunction();
    }

    public interface Junction {

        Junction add(Predicate predicate);

        List<Predicate> getPredicates();
    }

    public static class Disjunction extends Predicate implements Junction {

        private List<Predicate> predicates;

        Disjunction() {
            this.predicates = new ArrayList<>();
        }

        @Override
        public Disjunction add(Predicate predicate) {
            this.predicates.add(predicate);

            return this;
        }

        @Override
        public List<Predicate> getPredicates() {
            return predicates;
        }
    }

    public static class Conjunction extends Predicate implements Junction {

        private List<Predicate> predicates;

        Conjunction() {
            this.predicates = new ArrayList<>();
        }

        @Override
        public Conjunction add(Predicate predicate) {
            this.predicates.add(predicate);

            return this;
        }

        @Override
        public List<Predicate> getPredicates() {
            return predicates;
        }
    }
}
