package db.juhaku.juhakudb.filter;

import java.util.ArrayList;
import java.util.List;

import db.juhaku.juhakudb.util.ReflectionUtils;
import db.juhaku.juhakudb.util.TypedClass;

/**
 * Created by juha on 19/04/16.
 *
 * @author juha
 */
public class Root<T> {

    private String association;
    private String alias;
    private JoinMode joinMode;
    private String join;

    private List<Root<T>> joins;

    public Root() {
        joins = new ArrayList<>();
    }

    public Root<T> join(String association, String alias, JoinMode joinMode) {
        Root<T> root = new Root<>();
        root.association = association;
        root.alias = alias;
        root.joinMode = joinMode;
        joins.add(root);

        return this;
    }

    public Class<?> getType(TypedClass<T> type) {
        Class<?>[] types = ReflectionUtils.getClassGenericTypes(type);
        return types[0];
    }

    String getAssociation() {
        return association;
    }

    String getAlias() {
        return alias;
    }

    JoinMode getJoinMode() {
        return joinMode;
    }

    String getJoin() {
        return join;
    }

    void setJoin(String join) {
        this.join = join;
    }

    List<Root<T>> getJoins() {
        if (joins == null) {
            joins = new ArrayList<>();
        }

        return joins;
    }
}
