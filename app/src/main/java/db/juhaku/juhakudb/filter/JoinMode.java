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
package db.juhaku.juhakudb.filter;

/**
 * Created by juha on 24/04/16.
 *
 * <p>Define join mode for joins defined in {@link Root}. Join mode represents type of a join to
 * be made between two tables.</p>
 *
 * @author juha
 */
public enum JoinMode {

    /**
     * Inner join creates join between tables for matched elements in both table.
     */
    INNER_JOIN("INNER JOIN"),

    /**
     * Left join returns matched elements from right side of the join and all the records from left
     * side of the join.
     */
    LEFT_JOIN("LEFT JOIN"),

    /**
     * Full join creates join between two tables for every record in both tables. Nevertheless there
     * is a match in tables or not.
     */
    FULL_JOIN("FULL JOIN");

    private String value;

    JoinMode(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
