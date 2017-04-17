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
     * @since 1.2.0-SNAPSHOT
     */
    public Root<?> getRoot() {
        return root;
    }

    /**
     * Set root of sql query. Root forms tree of join operations.
     * @return Instance of {@link Root}.
     *
     * @since 1.2.0-SNAPSHOT
     */
    public void setRoot(Root<?> root) {
        this.root = root;
    }

    @Override
    public String toString() {
        return super.toString().concat(":").concat(sql);
    }
}
