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
package db.juhaku.juhakudb.core.android;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by juha on 29/05/16.
 *
 * @author juha
 *
 * @since 1.0.2
 */
public class ResultSet {

    private Map<String, Result> queryResults;
    private Object populatedEntity;
    private AtomicInteger order = new AtomicInteger();

    public ResultSet() {
        queryResults = new HashMap<>();
    }

    public void add(Class<?> columnType, String columnName, Object columnValue, String fieldName) {
        queryResults.put(columnName, new Result(columnType, columnName, columnValue, fieldName));
    }

    public Result get(String columnName) {
        return queryResults.get(columnName);
    }

    public List<Result> getResults() {
        ArrayList<Result> results = new ArrayList<>();
        for (Entry<String, Result> entry : queryResults.entrySet()) {
            results.add(entry.getValue());
        }
        Collections.sort(results, new Comparator<Result>() {
            @Override
            public int compare(Result lhs, Result rhs) {
                return lhs.index.intValue() < rhs.index.intValue() ? -1 : 1;
            }
        });

        return results;
    }

    public Object getPopulatedEntity() {
        return populatedEntity;
    }

    public void setPopulatedEntity(Object populatedEntity) {
        this.populatedEntity = populatedEntity;
    }

    public class Result {

        private Class<?> columnType;

        private String columnName;

        private Object columnValue;

        private String fieldName;

        private Integer index;

        public Result(Class<?> columnType, String columnName, Object columnValue, String fieldName) {
            this.columnType = columnType;
            this.columnName = columnName;
            this.columnValue = columnValue;
            this.fieldName = fieldName;
            this.index = order.incrementAndGet();
        }

        public Class<?> getColumnType() {
            return columnType;
        }

        public String getColumnName() {
            return columnName;
        }

        public Object getColumnValue() {
            return columnValue;
        }

        public String getFieldName() {
            return fieldName;
        }
    }
}
