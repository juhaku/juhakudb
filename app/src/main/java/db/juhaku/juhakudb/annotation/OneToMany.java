package db.juhaku.juhakudb.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import db.juhaku.juhakudb.core.Cascade;
import db.juhaku.juhakudb.core.Fetch;

/**
 * Created by juha on 22/12/15.
 * <p>Annotation to indicate one to may relation between entities. Should be placed above collection
 * of entities field.</p>
 * @author juha
 *
 * @since 1.0.2
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface OneToMany {
    Cascade cascade() default Cascade.STORE;

    Fetch fetch() default Fetch.LAZY;
}
