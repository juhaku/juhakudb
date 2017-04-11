package db.juhaku.juhakudb.filter;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import db.juhaku.juhakudb.exception.IllegalJoinException;
import db.juhaku.juhakudb.util.ReflectionUtils;

/**
 * Created by juha on 19/04/16.
 *
 * @author juha
 *
 * @since 1.2.0-SNAPSHOT
 */
public class Root<T> {

    private Class<?> model;

    private List<Root<T>> joins;

    public Root(Class<?> model) {
        this.model = model;
        joins = new ArrayList<>();
    }

    public Root<T> join(String target, JoinMode joinMode) {
        return join(target, null, joinMode);
    }

    public Root<T> join(String target, String alias, JoinMode joinMode) {
        //trim prefix when searching field.
        if (target.contains(".")) {
            target = target.substring(target.indexOf(".") + 1);
        }
        Join join = new Join(target, alias, joinMode, resolveJoinModal(this.model, target), false);
        joins.add(join);

        return join;
    }

    public Root<T> fetch(String target, JoinMode joinMode) {
        return fetch(target, null, joinMode);
    }

    public Root<T> fetch(String target, String alias, JoinMode joinMode) {
        //trim prefix when searching field.
        if (target.contains(".")) {
            target = target.substring(target.indexOf(".") + 1);
        }
        Join join = new Join(target, alias, joinMode, resolveJoinModal(this.model, target), true);
        this.joins.add(join);

        return join;
    }

    /**
     * Get list of joins added to this root. Joins can either be regular joins or fetch joins.
     * @return instance of {@link ArrayList} containing joins added.
     *
     * @since 1.2.0-SNAPSHOT
     */
    List<Root<T>> getJoins() {
        if (joins == null) {
            joins = new ArrayList<>();
        }

        return joins;
    }

    /**
     * Get model class of this root object.
     * @return instance of {@link Class} representing class of database model entity.
     *
     * @since 1.2.0-SNAPSHOT
     */
    Class<?> getModel() {
        return model;
    }

    /**
     * Resolves model class for next joint.
     *
     * @param model Class<?> instance of current model.
     * @param target String value of target model property in current model.
     * @return Class<?> value of resolved model class from current model.
     *
     * @since 1.2.0-SNAPSHOT
     *
     * @hide
     */
    private static Class<?> resolveJoinModal(Class<?> model, String target) {
        Field targetField = ReflectionUtils.findField(model, target);
        if (targetField == null) {
            throw new IllegalJoinException("Could not form a join to: " + target + " no such field in: " + model);
        }

        return ReflectionUtils.getFieldType(targetField);
    }


    /**
     * Join represents a relation in database between two tables.
     *
     * @author juha
     * @since 1.2.0-SNAPSHOT
     */
    class Join extends Root<T> {
        private String target;
        private String alias;
        private JoinMode joinMode;
        private boolean fetch;

        public Join(String target, String alias, JoinMode joinMode, Class<?> model, boolean fetch) {
            super(model);
            this.target = target;
            this.alias = alias;
            this.joinMode = joinMode;
            this.fetch = fetch;
        }

        public String getTarget() {
            return target;
        }

        public String getAlias() {
            return alias;
        }

        public JoinMode getJoinMode() {
            return joinMode;
        }

        public boolean isFetch() {
            return fetch;
        }

        public Class<?> getModel() {
            return super.model;
        }
    }
}
