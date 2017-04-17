package db.juhaku.juhakudb.core.android;

import android.util.Log;

import db.juhaku.juhakudb.annotation.Repository;
import db.juhaku.juhakudb.core.Criteria;

/**
 * Created by juha on 16/04/16.
 *<p>This criteria is used to resolve repository locations.</p>
 * @author juha
 *
 * @since 1.0.2
 */
public class RepositoryCriteria implements Criteria<String> {

    private String[] paths;

    public RepositoryCriteria(String... paths) {
        this.paths = paths;
    }

    @Override
    public boolean meetCriteria(String type) {
        for (String path : paths) {
            if (type.startsWith(path)) {
                try {
                    Class<?> clazz = Class.forName(type);
                    if (clazz.isInterface() && clazz.isAnnotationPresent(Repository.class)) {
                        Log.d(getClass().getName(), "path:" + path + " type:" + type);
                        Class<?> impl = clazz.getAnnotation(Repository.class).value();
                        if (impl == null) {
                            Log.e(getClass().getName(), "No implementation provided, implementation is mandatory");
                            return false;
                        }
                        return true;
                    }
                } catch (ClassNotFoundException e) {
                    Log.w(getClass().getName(), "class could not be initialized, ", e);
                    return false;
                }
            }
        }

        return false;
    }
}
