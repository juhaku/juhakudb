package db.juhaku.juhakudb.core.android;

import android.util.Log;

import javax.persistence.Entity;

import db.juhaku.juhakudb.core.Criteria;

/**
 * Created by juha on 16/04/16.
 *<p>This criteria is used to resolve entities for database schema creation.</p>
 * @author juha
 *
 * @since 1.0.2
 */
public class EntityCriteria implements Criteria<String> {

    private String[] packages;

    public EntityCriteria(String[] packages) {
        this.packages = packages;
    }

    @Override
    public boolean meetCriteria(String type) {
        for (String path : packages) {
            if (type.startsWith(path)) {

                // If class is not an enum and it has Entity annotation it should be entity.
                Class<?> clazz;
                if ((clazz = initializeClass(path)) != null) {
                    if (!Enum.class.isAssignableFrom(clazz) && clazz.isAnnotationPresent(Entity.class)) {

                        return true;
                    }
                }

            }
        }

        return false;
    }

    private static Class<?> initializeClass(String name) {
        try {
            return Class.forName(name);
        } catch (Exception e) {
            Log.e(DatabaseManager.class.getName(), "Class could not be initialized by name: " +
                    name + ", table wont be created to database", e);
        }

        return null;
    }
}
