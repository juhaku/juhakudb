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
package db.juhaku.juhakudb.core;

/**
 * Created by juha on 16/04/16.
 *<p>Super interface for filtering items. Items that meet the criteria should be processed differently
 * to items that does not meet.</p>
 * @author juha
 *
 * @since 1.0.2
 */
public interface Criteria<T> {

    /**
     * This method should provide implementation to determine whether required criteria is met.
     *
     * @param type type of object to resolve is criteria met. e.g. String or Integer if there is
     *             certain criteria for them to meet.
     * @return boolean value; true if criteria is met; false otherwise.
     *
     * @since 1.0.2
     */
    boolean meetCriteria(T type);
}
