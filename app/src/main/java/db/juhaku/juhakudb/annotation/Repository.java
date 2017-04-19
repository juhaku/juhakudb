package db.juhaku.juhakudb.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import db.juhaku.juhakudb.core.android.EntityManager;
import db.juhaku.juhakudb.repository.android.SimpleAndroidRepository;

/**
 * Created by juha on 15/04/16.
 * <p>Interface that is marked with this annotation is treated as a repository.</p>
 *
 * <p>Users may or may not provide their own implementation or repository if wanted. If own
 * implementation is not provided then {@link SimpleAndroidRepository} will be proxied for users. </p>
 *
 * <p>If own implementation is wished to use it should be provided as value to this annotation. e.g.
 * <code>@Repository(MyCustomRepositoryImpl.class).</code></p>
 *
 * @author juha
 *
 * @since 1.0.2
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Repository {
    Class<? extends SimpleAndroidRepository> value() default NoRepository.class;

    /**
     * No repository class represents not initializable repository. This is the default
     * option. This basically marks the annotation so that no custom repository implementation is
     * being used and default one should be used instead.
     *
     * @since 1.3.0
     */
    class NoRepository<K, T> extends SimpleAndroidRepository<K, T> {
        public NoRepository(EntityManager entityManager) {
            super(entityManager);
            throw new UnsupportedOperationException("No repository cannot be instantiated!");
        }

        public NoRepository(EntityManager entityManager, Class<T> persistentClass) {
            super(entityManager, persistentClass);
            throw new UnsupportedOperationException("No repository cannot be instantiated!");
        }
    }
}
