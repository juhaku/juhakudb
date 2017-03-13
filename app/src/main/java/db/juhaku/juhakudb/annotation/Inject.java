package db.juhaku.juhakudb.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by juha on 16/04/16.
 *
 * <p>Annotation that will inject a repository automatically to class attribute having annotation.</p>
 *
 * @author juha
 *
 * @since 1.0.2
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Inject {
}
