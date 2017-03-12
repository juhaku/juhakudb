package db.juhaku.juhakudb.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by juhaku on 5/12/15.
 *<p>Annotation that provides mapping for column name in database to field in this class.</p>
 *
 * @author juhaku
 *
 * @since 1.0.2
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(value = {ElementType.FIELD})
public @interface Column {
    String name() default "";
}
