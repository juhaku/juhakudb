package db.juhaku.juhakudb.repository.android;

import db.juhaku.juhakudb.core.android.EntityManager;

/**
 * Created by juha on 13/04/16.
 * <p>Implementation moved to {@link SimpleAndroidRepository}. This class is to keep
 * code consistent between minor updates.</p>
 *
 * <p>This class will be removed in further functional update!</p>
 *
 * @author juha
 *
 * @since 1.0.2
 */
@Deprecated
public abstract class SimpleRepository<K, T> extends SimpleAndroidRepository<K, T> {

    public SimpleRepository(EntityManager entityManager) {
        super(entityManager);
    }
}
