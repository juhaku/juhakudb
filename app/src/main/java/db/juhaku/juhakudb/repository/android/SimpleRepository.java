package db.juhaku.juhakudb.repository.android;

import db.juhaku.juhakudb.core.android.EntityManager;

/**
 * Created by juha on 15/03/17.
 * <p>Implementation moved to {@link SimpleAndroidRepository}. This class is to keep
 * code consistent between minor updates.</p>
 *
 * <p>This class will be removed in further functional update!</p>
 *
 * @author juha
 *
 * @since 1.1.3-SNAPSHOT
 */
@Deprecated
public abstract class SimpleRepository<K, T> extends SimpleAndroidRepository<K, T> {

    public SimpleRepository(EntityManager entityManager) {
        super(entityManager);
    }
}
