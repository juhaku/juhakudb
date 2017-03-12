package db.juhaku.juhakudb.core.android;

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
                return true;
            }
        }

        return false;
    }
}
