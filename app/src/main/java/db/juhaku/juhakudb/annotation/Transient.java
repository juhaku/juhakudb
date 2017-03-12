package db.juhaku.juhakudb.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by juhaku on 5/12/15.
 * <p>Annotation that specifies field as transient so it wont be stored in database.</p>
 *
 * @author Juha Kukkonen
 *
 * @since 1.0.2
 */
@Retention(value = RetentionPolicy.RUNTIME)
@Target(value = {ElementType.FIELD})
public @interface Transient {
}
