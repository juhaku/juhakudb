/**
MIT License

Copyright (c) 2018 juhaku

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
 * Created by juha on 23/12/15.
 *<p>Utility class for String.</p>
 * @author juha
 *
 * @since 1.0.2
 */
public class StringUtils {

    /**
     * Checks is provided String null or otherwise empty by containing only white space.
     * @param arg0 String value to check.
     * @return Returns true if provided String is blank otherwise false.
     */
    public static final boolean isBlank(String arg0) {
        return !(arg0 != null && arg0.trim().length() > 0);
    }

    /**
     * Converts array of objects to comma separated String.
     *
     * @param array Object array of values to convert.
     * @return Stringified array.
     *
     * @since 2.0.1
     */
    public static final String arrayToString(Object... array) {
        StringBuilder builder = new StringBuilder();

        int index = 0;
        for (Object str : array) {
            builder.append(str);

            if (index < array.length - 1) {
                builder.append(",");
            }
            index++;
        }

        return builder.toString();
    }
}
