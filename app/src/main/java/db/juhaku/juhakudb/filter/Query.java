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

/**
 * Created by juha on 17/04/16.
 *
 * <p>Wrapper class to wrap SQL query and possible parameters of the query together.</p>
 *
 * @author juha
 */
public class Query {

    private String sql;
    private String[] args;
    private Root<?> root;

    public Query(String sql, String[] args) {
        this.sql = sql;
        this.args = args;
    }

    /**
     * Get formed SQL.
     * @return String sql.
     */
    public String getSql() {
        return sql;
    }

    /**
     * Get args for sql. If sql contains ? they will be replaced with these args.
     * @return String[] array of args to be used in sql query.
     */
    public String[] getArgs() {
        return args;
    }

    /**
     * Get root of sql query. Root forms tree of join operations.
     * @return Instance of {@link Root}.
     *
     * @since 1.2.0
     */
    public Root<?> getRoot() {
        return root;
    }

    /**
     * Set root of sql query. Root forms tree of join operations.
     * @return Instance of {@link Root}.
     *
     * @since 1.2.0
     */
    public void setRoot(Root<?> root) {
        this.root = root;
    }

    @Override
    public String toString() {
        return super.toString().concat(":").concat(sql);
    }
}
