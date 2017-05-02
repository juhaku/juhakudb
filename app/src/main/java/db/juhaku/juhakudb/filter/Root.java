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

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import db.juhaku.juhakudb.exception.IllegalJoinException;
import db.juhaku.juhakudb.util.ReflectionUtils;

/**
 * Created by juha on 19/04/16.
 *
 * <p>Root is a tree like object for managing join relation between tables for sql queries and
 * entity conversions.</p>
 *
 * @author juha
 *
 * @since 1.2.0
 */
public class Root<T> {

    private Class<?> model;

    private List<Root<T>> joins;

    /**
     * Create new root for given model class. Model class must be an entity in database. Newly created
     * root will be the starting point of joins to sql queries.
     * @param model {@link Class} of model class of database entity.
     *
     * @since 1.2.0
     */
    Root(Class<?> model) {
        this.model = model;
        joins = new ArrayList<>();
    }

    /**
     * Create regular join from this root to the target field. This method is same as calling
     * {@link #join(String, String, JoinMode)} with alias as null.
     *
     * @param target String value of target field in root object to join to.
     * @param joinMode {@link JoinMode} to use with the join operation.
     * @return The join that was made by calling of this method. Root of join is now previously set
     * target field.
     *
     * @since 1.2.0
     */
    public Root<T> join(String target, JoinMode joinMode) {
        return join(target, null, joinMode);
    }

    /**
     * Create regular join from this root to the target field.
     *
     * @param target String value of target field in root object to join to.
     * @param alias String value of additional alias for join operation.
     * @param joinMode {@link JoinMode} to use with the join operation.
     * @return The join that was made by calling of this method. Root of join is now previously set
     * target field.
     *
     * @since 1.2.0
     */
    public Root<T> join(String target, String alias, JoinMode joinMode) {
        //trim prefix when searching field.
        if (target.contains(".")) {
            target = target.substring(target.indexOf(".") + 1);
        }
        Join join = new Join(target, alias, joinMode, resolveJoinModel(this.model, target), false);
        joins.add(join);

        return join;
    }

    /**
     * Create fetch join from this root to the target field. Fetch joins will be returned with the
     * same sql query than the root of sql query. This method is same as calling
     * {@link #fetch(String, String, JoinMode)} with alias as null.
     *
     * @param target String value of target field in root object to join to.
     * @param joinMode {@link JoinMode} to use with the join operation.
     * @return The join that was made by calling of this method. Root of join is now previously set
     * target field.
     *
     * @since 1.2.0
     */
    public Root<T> fetch(String target, JoinMode joinMode) {
        return fetch(target, null, joinMode);
    }

    /**
     * Create fetch join from this root to the target field. Fetch joins will be returned with the
     * same sql query than the root of sql query.
     *
     * @param target String value of target field in root object to join to.
     * @param alias String value of additional alias for join operation.
     * @param joinMode {@link JoinMode} to use with the join operation.
     * @return The join that was made by calling of this method. Root of join is now previously set
     * target field.
     *
     * @since 1.2.0
     */
    public Root<T> fetch(String target, String alias, JoinMode joinMode) {
        //trim prefix when searching field.
        if (target.contains(".")) {
            target = target.substring(target.indexOf(".") + 1);
        }
        Join join = new Join(target, alias, joinMode, resolveJoinModel(this.model, target), true);
        this.joins.add(join);

        return join;
    }

    /**
     * Get list of joins added to this root. Joins can either be regular joins or fetch joins.
     * @return instance of {@link ArrayList} containing joins added.
     *
     * @since 1.2.0
     */
    public List<Root<T>> getJoins() {
        if (joins == null) {
            joins = new ArrayList<>();
        }

        return Collections.unmodifiableList(joins); // return read-only list
    }

    /**
     * Get model class of this root object.
     * @return instance of {@link Class} representing class of database model entity.
     *
     * @since 1.2.0
     */
    public Class<?> getModel() {
        return model;
    }

    /**
     * Resolves model class for next joint.
     *
     * @param model Class<?> instance of current model.
     * @param target String value of target model property in current model.
     * @return Class<?> value of resolved model class from current model.
     *
     * @since 1.2.0
     *
     * @hide
     */
    private static Class<?> resolveJoinModel(Class<?> model, String target) {
        Field targetField = ReflectionUtils.findField(model, target);
        if (targetField == null) {
            throw new IllegalJoinException("Could not form a join to: " + target + ", no such field in: " + model);
        }

        return ReflectionUtils.getFieldType(targetField);
    }


    /**
     * Join represents a relation in database between two tables.
     *
     * @author juha
     * @since 1.2.0
     */
    public class Join extends Root<T> {
        private String target;
        private String alias;
        private JoinMode joinMode;
        private boolean fetch;

        /**
         * Init new instance of join. Join is not publicly available to initialize instead it should
         * be created via {@link Root}'s fetch or join methods.
         *
         * @param target String target field where to join.
         * @param alias String additional alias for join operation.
         * @param joinMode {@link JoinMode} for the join.
         * @param model Model {@link Class} of entity where join is created to.
         * @param fetch boolean whether joined entity is also fetched with same sql than the root entity.
         *
         * @since 1.2.0
         */
        Join(String target, String alias, JoinMode joinMode, Class<?> model, boolean fetch) {
            super(model);
            this.target = target;
            this.alias = alias;
            this.joinMode = joinMode;
            this.fetch = fetch;
        }

        /**
         * Get target field's name where the join is formed to.
         * @return String target field name.
         *
         * @since 1.2.0
         */
        public String getTarget() {
            return target;
        }

        /**
         * Get alias if provided for join to the target table.
         * @return String alias or null if not provided.
         *
         * @since 1.2.0
         */
        public String getAlias() {
            return alias;
        }

        /**
         * Get join mode used with the join to the target table.
         * @return {@link JoinMode} used.
         *
         * @since 1.2.0
         */
        public JoinMode getJoinMode() {
            return joinMode;
        }

        /**
         * If set to true joined table will also be fetched with same sql query than the root of sql query.
         * @return true if join should be fetched; false otherwise.
         *
         * @since 1.2.0
         */
        public boolean isFetch() {
            return fetch;
        }

        /**
         * Get model class of target table.
         * @return {@link Class} of model class of target table.
         *
         * @since 1.2.0
         */
        public Class<?> getModel() {
            return super.model;
        }
    }
}
