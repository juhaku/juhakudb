package db.juhaku.juhakudb.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by juha on 16/04/16.
 *
 * <p>Annotation to provide automatic repository initialization on field which has this annotation.</p>
 * <p>TODO NOT IMPLEMENTED</p>
 *
 * @author juha
 *
 * @since 1.0.2
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface InjectRepository {
}
