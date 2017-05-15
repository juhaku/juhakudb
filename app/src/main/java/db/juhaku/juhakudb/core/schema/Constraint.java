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
package db.juhaku.juhakudb.core.schema;

import java.io.Serializable;

import db.juhaku.juhakudb.util.StringUtils;

/**
 * Created by juha on 15/05/17.
 *
 * <p>This class represents index or unique constraint for specified columns in specified table
 * at database.</p>
 *
 * @author Juha Kukkonen
 *
 * @since 2.0.2-SNAPSHOT
 */
public class Constraint implements Serializable {

    private String name;
    private boolean unique;
    private String tableName;
    private String[] columns;

    public Constraint(String name, boolean unique, String tableName, String... columns) {
        this.name = name;
        this.unique = unique;
        this.tableName = tableName;
        this.columns = columns;
    }

    @Override
    public String toString() {
        return new StringBuilder("CREATE ").append(unique ? "UNIQUE " : "").append("INDEX ")
                .append(name).append(" ON ").append(tableName).append("(").append(StringUtils.arrayToString(columns))
                .append(")").toString();
    }
}
