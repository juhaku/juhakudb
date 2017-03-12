package db.juhaku.juhakudb.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by juhaku on 5/12/15.
 *<p>This annotation is used to provide class level notation of database table.</p>
 *
 * @author Juha Kukkonen
 *
 * @since 1.0.2
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(value = {ElementType.TYPE})
public @interface Entity {
    String name() default "";
}
