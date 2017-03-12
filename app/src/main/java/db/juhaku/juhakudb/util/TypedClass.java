package db.juhaku.juhakudb.util;

/**
 * Created by juha on 29/04/16.
 *<p>Typed class is used for sub classing a class with generic type which os not sub class
 * itself of generic super class. Sub classing is necessary in order to return generic type of
 * class as generic type can be returned from super class.</p>
 * <code>
 *     Example:<br/><br/>
 *     TestItem&lt;GenericType&gt; item = new TestItem&lt;&gt;();<br/>
 *     Class&lt;GenericType&gt; clazz = ReflectionUtils.getClassGenericTypes(new TypedClass&lt;GenericType&gt;()&#123;&#125;)[0] <br/>
 *
 * </code>
 * See more details from {@link ReflectionUtils#getClassGenericTypes(TypedClass)}
 * @author juha
 * @see ReflectionUtils
 *
 * @since 1.0.2
 */
public abstract class TypedClass<T> {
}
