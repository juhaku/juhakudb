package db.juhaku.juhakudb.filter;

/**
 * Created by juha on 17/04/16.
 *
 * @author juha
 */
public class Query {

    private String sql;
    private String[] args;

    public Query(String sql, String[] args) {
        this.sql = sql;
        this.args = args;
    }

    public String getSql() {
        return sql;
    }

    public String[] getArgs() {
        return args;
    }

    @Override
    public String toString() {
        return super.toString().concat(":").concat(sql);
    }
}
