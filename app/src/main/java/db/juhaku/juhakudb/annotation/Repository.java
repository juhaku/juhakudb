package db.juhaku.juhakudb.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import db.juhaku.juhakudb.repository.android.SimpleRepository;

/**
 * Created by juha on 15/04/16.
 * <p>Interface that is marked with this annotation is treated as a repository. This annotation must
 * contain value for implementing repository.</p>
 *
 * @author juha
 *
 * @since 1.0.2
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Repository {
    Class<? extends SimpleRepository> value();
}
