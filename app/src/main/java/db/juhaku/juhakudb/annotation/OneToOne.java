package db.juhaku.juhakudb.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import db.juhaku.juhakudb.core.Cascade;
import db.juhaku.juhakudb.core.Fetch;

/**
 * Created by juha on 22/12/15.
 *<p>Annotation to indicate one to one relation between entities. Should be placed above entity
 * field. Define mappedBy attribute to determine side of ownership.</p>
 *
 * @author juha
 *
 * @since 1.0.2
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface OneToOne {
    String mappedBy() default "";

    Cascade cascade() default Cascade.STORE;

    Fetch fetch() default Fetch.LAZY;
}
