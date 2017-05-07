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

import java.util.LinkedList;
import java.util.List;

/**
 * Created by juha on 04/05/17.
 *
 * <p>Expression stands for function e.g. lower(value) or value for SQL queries. This expression
 * can be used in predicates and in predicate builder for creating advanced queries against
 * database.</p>
 *
 * @author Juha Kukkonen
 *
 * @since 2.0.2-SNAPSHOT
 */
public class Expression {

    private String value;

    private Expression() {
        // Not instantiable.
    }

    /**
     * Value resolved for this expression.
     *
     * @return String value of expression.
     *
     * @since 2.0.2-SNAPSHOT
     */
    String getValue() {
        return value;
    }

    /**
     * Create expression of value. Useful for wrapping given value as an expression for predicate
     * builder for creating criteria for SQL query.
     *
     * @param arg Object value to be converted to expression.
     *
     * @return Expression of provided argument.
     *
     * @since 2.0.2-SNAPSHOT
     */
    public static Expression of(Object arg) {
        Expression expression = new Expression();
        expression.value = arg.toString();

        return expression;
    }

    /**
     * Creates MIN(arg) function expression. Min expression is used to get smallest value.
     *
     * @param arg Object arg to convert to expression.
     * @return {@link Expression} for min function.
     *
     * @since 2.0.2-SNAPSHOT
     */
    public static Expression min(Object arg) {
        return of(function("MIN", arg));
    }

    /**
     * Creates multi MIN(arg, arg, arg, ...) function expression. Multi min expression works similar
     * to normal min expression but it selects smallest value from provided array instead of column of database.
     *
     * @param args Object argument array to convert to multi min expression.
     * @return {@link Expression} for min function.
     *
     * @since 2.0.2-SNAPSHOT
     */
    public static Expression min(Object... args) {
        return of(function("MIN", arrayToString(args)));
    }

    /**
     * Creates MAX(arg) function expression. Max expression is used to get biggest value.
     *
     * @param arg Object arg to convert to expression.
     * @return {@link Expression} for max function.
     *
     * @since 2.0.2-SNAPSHOT
     */
    public static Expression max(Object arg) {
        return of(function("MAX", arg));
    }

    /**
     * Creates multi MAX(arg, arg, arg, ...) function expression. Multi max expression works similar
     * to normal max expression but it selects biggest value from provided array instead of column of database.
     *
     * @param args Object argument array to convert to multi max expression.
     * @return {@link Expression} for max function.
     *
     * @since 2.0.2-SNAPSHOT
     */
    public static Expression max(Object... args) {
        return of(function("MAX", arrayToString(args)));
    }

    /**
     * Creates AVG(arg) function expression. Avg expression will calculate average value.
     *
     * @param arg Object arg to convert to expression.
     * @return {@link Expression} for avg function.
     *
     * @since 2.0.2-SNAPSHOT
     */
    public static Expression avg(Object arg) {
        return of(function("AVG", arg));
    }

    /**
     * Creates SUM(arg) function expression. Sum expression calculates total.
     *
     * @param arg Object arg to convert to expression.
     * @return {@link Expression} for sum function.
     *
     * @since 2.0.2-SNAPSHOT
     */
    public static Expression sum(Object arg) {
        return of(function("SUM", arg));
    }

    /**
     * Creates COUNT(arg) function expression. Count expression will calculate number of times for
     * values or rows.
     *
     * @param arg Object arg to convert to expression.
     * @return {@link Expression} for count function.
     *
     * @since 2.0.2-SNAPSHOT
     */
    public static Expression count(Object arg) {
        return of(function("COUNT", arg));
    }

    /**
     * Creates LOWER(arg) function expression. Lower expression converts argument to lower letters.
     *
     * @param arg Object arg to convert to expression.
     * @return {@link Expression} for lower function.
     *
     * @since 2.0.2-SNAPSHOT
     */
    public static Expression lower(Object arg) {
        return of(function("LOWER", arg));
    }

    /**
     * Creates LOWER(arg) function expression for each element in given object array.
     * Lower expression converts argument to lower letters.
     *
     * @param args Object array of arguments to convert to expressions.
     * @return List of {@link Expression} for lower function.
     *
     * @since 2.0.2-SNAPSHOT
     */
    public static List<Expression> lower(Object... args) {
        return objectArrayToFunctionExpression("LOWER", args);
    }

    /**
     * Creates UPPER(arg) function expression. Upper expression converts argument to upper letters.
     *
     * @param arg Object arg to convert to expression.
     * @return {@link Expression} for upper function.
     *
     * @since 2.0.2-SNAPSHOT
     */
    public static Expression upper(Object arg) {
        return of(function("UPPER", arg));
    }

    /**
     * Creates UPPER(arg) function expression for each element in given object array.
     * Upper expression converts argument to upper letters.
     *
     * @param args Object array of arguments to convert to expressions.
     * @return List of {@link Expression} for upper function.
     *
     * @since 2.0.2-SNAPSHOT
     */
    public static List<Expression> upper(Object... args) {
        return objectArrayToFunctionExpression("UPPER", args);
    }

    /**
     * Creates COALESCE(arg, arg, arg, ...) function expression for given args list. Coalesce expression
     * function returns first not null element from argument list.
     *
     * @param args Object arg to convert to expression.
     * @return {@link Expression} for coalesce function.
     *
     * @since 2.0.2-SNAPSHOT
     */
    public static Expression coalesce(Object... args) {
        return of(function("COALESCE", arrayToString(args)));
    }

    /**
     * Creates ABS(arg) function expression. Abs expression will convert argument's absolute value. E.g.
     * for -1 it would be 1.
     *
     * @param arg Object arg to convert to expression.
     * @return {@link Expression} for abs function.
     *
     * @since 2.0.2-SNAPSHOT
     */
    public static Expression abs(Object arg) {
        return of(function("ABS", arg));
    }

    /**
     * Creates ABS(arg) function expression for each element in given object array.
     * Abs expression will convert argument's absolute value. E.g. for -1 it would be 1.
     *
     * @param args Object array of arguments to convert to expressions.
     * @return List of {@link Expression} for abs function.
     *
     * @since 2.0.2-SNAPSHOT
     */
    public static List<Expression> abs(Object... args) {
        return objectArrayToFunctionExpression("ABS", args);
    }

    /**
     * Creates LENGTH(arg) function expression. Length expression will calculate length of object.
     *
     * @param arg Object arg to convert to expression.
     * @return {@link Expression} for length function.
     *
     * @since 2.0.2-SNAPSHOT
     */
    public static Expression length(Object arg) {
        return of(function("LENGTH", arg));
    }

    /**
     * Creates LENGTH(arg) function expression for each element in given object array.
     * Length expression will calculate length of object.
     *
     * @param args Object array of arguments to convert to expressions.
     * @return List of {@link Expression} for length function.
     *
     * @since 2.0.2-SNAPSHOT
     */
    public static List<Expression> length(Object... args) {
        return objectArrayToFunctionExpression("LENGTH", args);
    }

    /**
     * Creates TRIM(arg) function expression. Trim function will trim whitespace from both ends of
     * given argument.
     *
     * @param arg Object arg to convert to expression.
     * @return {@link Expression} for trim function.
     *
     * @since 2.0.2-SNAPSHOT
     */
    public static Expression trim(Object arg) {
        return of(function("TRIM", arg));
    }

    /**
     * Creates TRIM(arg, letter) function expression. Trim with letter will trim all occurrences of
     * given letter from both ends of argument. E.g. for function TRIM('aaaMessageaaa', 'a') output
     * would become: Message.
     *
     * @param arg Object arg to convert to expression.
     * @return {@link Expression} for trim function.
     *
     * @since 2.0.2-SNAPSHOT
     */
    public static Expression trim(Object arg, String letter) {
        return of(function("TRIM", arrayToString(arg, letter)));
    }

    /**
     * Converts object array to a function expression list.
     *
     * <p>Each object in array will be converted to expression with given function. E.g.
     * lower(name).</p>
     *
     * @param function String value of function to convert objects to.
     * @param args Object array to convert to expressions.
     * @return List of expression converted from value array.
     *
     * @since 2.0.2-SNAPSHOT
     *
     * @hide
     */
    private static List<Expression> objectArrayToFunctionExpression(String function, Object... args) {
        List<Expression> expressions = new LinkedList<>();
        for (Object arg : args) {
            expressions.add(of(function(function, arg.toString())));
        }

        return expressions;
    }

    /**
     * Converts provided object array to comma separated string. E.g. value1, value2, value3.
     *
     * @param args Array of object to transform to string.
     * @return Comma separated string.
     *
     * @since 2.0.2-SNAPSHOT
     *
     * @hide
     */
    private static String arrayToString(Object... args) {
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
     * @since 2.0.2-SNAPSHOT
     *
     * @hide
     */
    private static String function(String function, Object arg) {
        return new StringBuilder(function).append("(").append(arg).append(")").toString();
    }

    @Override
    public String toString() {
        return getValue();
    }
}
