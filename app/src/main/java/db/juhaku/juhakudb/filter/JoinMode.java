package db.juhaku.juhakudb.filter;

/**
 * Created by juha on 24/04/16.
 *
 * @author juha
 */
public enum JoinMode {

    INNER_JOIN("INNER JOIN"), LEFT_JOIN("LEFT JOIN"), FULL_JOIN("FULL JOIN");

    private String value;

    JoinMode(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
