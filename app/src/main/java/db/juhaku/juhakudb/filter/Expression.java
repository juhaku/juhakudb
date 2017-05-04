package db.juhaku.juhakudb.filter;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by juha on 04/05/17.
 *
 * @author juha
 */
public class Expression {

    private String value;

    Expression() {
    }

    String getValue() {
        return value;
    }

    /**
     * Create expression of value.
     *
     * @param arg Object value to be converted to expression.
     *
     * @return Expression of provided argument.
     *
     * @since
     */
    public static Expression of(Object arg) {
        Expression expression = new Expression();
        expression.value = arg.toString();

        return expression;
    }

    public static Expression lower(String arg) {
        return of(function("LOWER", arg));
    }

    public static List<Expression> lower(Object... args) {
        return of("LOWER", args);
    }

    public static Expression upper(String arg) {
        return of(function("UPPER", arg));
    }

    public static List<Expression> upper(Object... args) {
        return of("UPPER", args);
    }

    public static Expression coalesce(Object... args) {
        return of(function("COALESCE", argsToString(args)));
    }

    public static Expression abs(String arg) {
        return of(function("ABS", arg));
    }

    public static Expression length(String arg) {
        return of(function("LENGTH", arg));
    }

    private static List<Expression> of(String function, Object... args) {
        List<Expression> expressions = new LinkedList<>();
        for (Object arg : args) {
            expressions.add(of(function(function, arg.toString())));
        }

        return expressions;
    }

    static String argsToString(Object... args) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0 ; i < args.length ; i ++) {

            builder.append(args[i]);

            if (i < args.length - 1) {
                builder.append(",");
            }
        }

        return builder.toString();
    }


    /**
     * Create expression by the function for provided argument. E.g. lower(name).
     *
     * @param function String name of SQL function.
     * @param arg String argument e.g. column name in database table or value of column.
     *
     * @return String expression function.
     *
     * @since
     *
     * @hide
     */
    private static String function(String function, String arg) {
        return new StringBuilder(function).append("(").append(arg).append(")").toString();
    }
}
