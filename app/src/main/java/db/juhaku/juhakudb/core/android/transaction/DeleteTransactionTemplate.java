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
package db.juhaku.juhakudb.core.android.transaction;

import android.database.Cursor;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import db.juhaku.juhakudb.core.NameResolver;
import db.juhaku.juhakudb.core.android.ResultSet;
import db.juhaku.juhakudb.core.schema.Reference;
import db.juhaku.juhakudb.core.schema.Schema;
import db.juhaku.juhakudb.filter.Filter;
import db.juhaku.juhakudb.filter.PredicateBuilder;
import db.juhaku.juhakudb.filter.Query;
import db.juhaku.juhakudb.filter.Root;

/**
 * Created by juha on 13/05/16.
 *
 * @author juha
 * @since 1.0.2
 */
public class DeleteTransactionTemplate<T> extends TransactionTemplate {

    private Collection<T> items;

    public void setItems(Collection<T> items) {
        this.items = items;
    }

    @Override
    void onTransaction() {
        String tableName = resolveTableName(getRootClass());
        delete(tableName);
        commit();
    }

    private void delete(String tableName) {
        cascade(tableName, items, null);

        int deleted = 0;
        for (final T item : items) {
            Query query = getProcessor().createWhere(null, new Filter() {
                @Override
                public void filter(Root root, PredicateBuilder builder) {
                    builder.eq(resolveIdColumn(getRootClass()), item.toString());
                }
            });
            deleted += getDb().delete(tableName, query.getSql(), query.getArgs());
        }

        setResult(deleted);
    }

    private Query createCountQuery(String table, String column, Object value) {
        StringBuilder sqlBuilder = new StringBuilder("SELECT count(");
        sqlBuilder.append(column).append(") FROM ").append(table).append(" WHERE ").append(column).append(" = ?");

        return new Query(sqlBuilder.toString(), new String[]{value.toString()});
    }

    private long executeCountQuery(Query query) {
        Cursor result = getDb().rawQuery(query.getSql(), query.getArgs());
        result.moveToFirst();
        ResultSet resultSet = getConverter().cursorToCustomResultSet(result);

        return ((Integer) resultSet.getResults().get(0).getColumnValue()).longValue();
    }

    private void cascade(String tableName, Collection<T> items, T parentItem) {
        List<String> references = findReferences(tableName);
        for (String referenceTable : references) {
            for (T item : items) {

                // Find reverse join column from reverse join table where join is formed to primary key table
                String columnName = resolveReferenceColumnIdName(referenceTable, tableName);

                Query count = createCountQuery(referenceTable, columnName, item);
                if (executeCountQuery(count) > 0) {
                    Reference manyToMany = getManyToManyReference(referenceTable, columnName);
                    if (manyToMany == null) {
                        cascade(referenceTable, getCascadingReferenceValues(referenceTable, NameResolver.ID_FIELD_SUFFIX, columnName, item), item);
                    } else if (!isCached(manyToMany.getReferenceTableName())) {
                        cache(tableName); // cache parent table!
                        cascade(manyToMany.getReferenceTableName(), getCascadingReferenceValues(referenceTable, manyToMany.getColumnName(), columnName, item), item);
                    }
                    if (manyToMany == null) {
                        int deleted = executeDelete(referenceTable, new Object[][]{{columnName, item}});
                        Log.v(getClass().getName(), "executed sub delete for: " + referenceTable + " with column: "
                                + columnName + " having value: " + item + ", affected rows: " + deleted);
                    }
                    if (manyToMany != null && !isCached(tableName)) {
                        int deleted = executeDelete(referenceTable, new Object[][]{{columnName, item}, {manyToMany.getColumnName(), parentItem}});
                        Log.v(getClass().getName(), "executed sub delete for: " + referenceTable + " with column: "
                                + columnName + " having value: " + item + " and :" + manyToMany.getColumnName()
                                + " having value: " + parentItem + ", affected rows: " + deleted);
                    }
                    if (!isCached(tableName)) {
                        // Delete orphans
                        if (executeCountQuery(createCountQuery(referenceTable, columnName, item)) == 0) {
                            int deleted = executeDelete(tableName, new Object[][]{{NameResolver.ID_FIELD_SUFFIX, item}});
                            Log.v(getClass().getName(), "executed sub delete for: " + tableName + " with column: "
                                    + NameResolver.ID_FIELD_SUFFIX + " having value: " + item + ", affected rows: " + deleted);
                        }
                    }
                }
            }
            cache(tableName);
        }
    }

    private String resolveReferenceColumnIdName(String tableName, String reverseJoinTable) {
        Schema table = getSchema().getElement(tableName);

        if (table != null) {
            for (Reference reference : table.getReferences()) {
                if (reference.getReferenceTableName().equals(reverseJoinTable)) {
                    return reference.getColumnName();
                }
            }
        }

        return null;
    }

    private List<String> findReferences(String tableName) {
        List<String> referencedTables = new ArrayList<>();
        for (Schema table : Schema.toSet(getSchema())) {
            for (Reference reference : table.getReferences()) {
                if (reference.getReferenceTableName().equals(tableName)) {
                    referencedTables.add(table.getName());
                }
            }
        }

        return referencedTables;
    }

    private int executeDelete(final String table, final Object[][] columnValues) {
        Query query = getProcessor().createWhere(null, new Filter() {
            @Override
            public void filter(Root root, PredicateBuilder builder) {
                for (Object[] column : columnValues) {
                    builder.eq(column[0].toString(), column[1]);
                }
            }
        });

        return getDb().delete(table, query.getSql(), query.getArgs());
    }

    private Reference getManyToManyReference(String tableName, String columnName) {
        for (Schema table : Schema.toSet(getSchema())) {
            if (table.getName().equals(tableName)) {
                if (table.getElement(NameResolver.ID_FIELD_SUFFIX) != null) {
                    return null;
                }
                for (Reference reference : table.getReferences()) {
                    if (!reference.getColumnName().equals(columnName)) {
                        return reference;
                    }
                }
            }
        }

        return null;
    }

    private Collection<T> getCascadingReferenceValues(String table, String searchColumn, String column, Object value) {
        StringBuilder sqlBuilder = new StringBuilder("SELECT ").append(searchColumn)
                .append(" FROM ").append(table).append(" WHERE ").append(column).append(" = ?");

        Query query = new Query(sqlBuilder.toString(), new String[]{value.toString()});

        Cursor result = getDb().rawQuery(query.getSql(), query.getArgs());
        List<ResultSet> resultSets = getConverter().convertCursorToCustomResultSetList(result);
        List<T> referencedIds = new ArrayList<>();
        for (ResultSet resultSet : resultSets) {
            // We are expecting only one column, which is id column
            referencedIds.add((T) resultSet.getResults().get(0).getColumnValue());
        }

        return referencedIds;
    }
}
