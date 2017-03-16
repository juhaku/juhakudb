package db.juhaku.juhakudb.filter;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import db.juhaku.juhakudb.filter.Root.Joint;
import db.juhaku.juhakudb.util.StringUtils;

/**
 * Created by juha on 17/03/17.
 *
 * @author juha
 *
 * @since
 */
public class QuerProcessor {

    public Query createQuery(Class<?> modelClass, Filter filter) {
        // initialize root and predicates for joins and restrictions
        Root<?> root = new Root<>(modelClass);
        Predicates predicates = new Predicates();

        // create joins and restrictions
        filter.filter(root, predicates);



        return null;
    }

    private void createSelect(Root<?> root, StringBuilder sql) {
        for (Root r : root.getJoins()) {
            Joint joint = (Joint) r;
            if (joint.isFetch()) {
                //TODO add to select
            }
        }
    }

    public Query createWhere(Class<?> modelClass, Filter<?> filter) {
        return null;
    }

    public static class Alias {

        private static AtomicInteger count = new AtomicInteger();

        private static Map<Class<?>, String> aliasMap = new HashMap<>();

        public static String forJoint(Joint joint) {

            /*
             * If alias is empty and it does not exist in cache add new one.
             */
            if (StringUtils.isBlank(joint.getAlias())) {
                String alias = aliasMap.get(joint.getModel());

                if (StringUtils.isBlank(alias)) {

                    return generateAlias("");
                } else {

                    return alias;
                }
            } else {

                return joint.getAlias();
            }
        }

        private static String generateAlias(String tableName) {
            StringBuilder aliasBuilder = new StringBuilder(String.valueOf(tableName.charAt(0)));
            int index;
            while ((index = tableName.indexOf("_")) > -1) {
                aliasBuilder.append(tableName.charAt(index + 1));
                tableName = tableName.substring(index + 1);
            }

            return aliasBuilder.toString();
        }

        public static void clearCache() {
            if (aliasMap != null) {
                aliasMap.clear();
            }
            if (count != null) {
                count.set(0);
            }
        }

    }

    private static class JoinProcessor {

    }
}
