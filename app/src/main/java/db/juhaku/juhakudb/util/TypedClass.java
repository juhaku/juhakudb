/**
MIT License

Copyright (c) 2017 juhaku

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
*/
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
