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
 * Created by juha on 14/03/17.
 * <p>Utility enum for mapping reserved words for SQL queries and ORM mapping.</p>
 *
 * @author juha
 * @since 1.1.2
 */
public enum ReservedWords {

    LEFT, JOIN, RIGHT, INNER, FULL, ON, FROM, SELECT, WHERE, AND, OR, IN, NOT, LIKE,
    IS, NULL, BETWEEN, ASC, DESC, LIMIT, ORDER, BY, OFFSET, MIN, MAX, AVG, SUM, COUNT,
    LOWER, UPPER, ABS, COALESCE, LENGTH, TRIM;

    /**
     * Check if given word belongs to reserved words. Word match is searched case insensitive.
     * @param word String value of word to look for.
     * @return true if reserved words has given word; false otherwise.
     * @since 1.1.2
     */
    public static boolean has(String word) {
        for (ReservedWords reservedWord : ReservedWords.values()) {
            if (reservedWord.toString().equalsIgnoreCase(word)) {
                return true;
            }
        }

        return false;
    }
}
