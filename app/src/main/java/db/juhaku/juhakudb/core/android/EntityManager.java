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

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import db.juhaku.juhakudb.core.android.transaction.DeleteTransactionTemplate;
import db.juhaku.juhakudb.core.android.transaction.QueryTransactionTemplate;
import db.juhaku.juhakudb.core.android.transaction.StoreTransactionTemplate;
import db.juhaku.juhakudb.core.android.transaction.TransactionTemplate;
import db.juhaku.juhakudb.core.android.transaction.TransactionTemplateFactory;
import db.juhaku.juhakudb.core.android.transaction.TransactionTemplateFactory.Type;
import db.juhaku.juhakudb.filter.Filter;
import db.juhaku.juhakudb.filter.Query;
import db.juhaku.juhakudb.filter.QueryProcessor;

/**
 * Created by juha on 12/05/16.
 *
 * @author juha
 * @since 1.0.2
 */
public class EntityManager {

    private DatabaseHelper databaseHelper;
    private EntityConverter converter = new EntityConverter();
    private QueryProcessor processor;

    private static TransactionTemplateFactory factory;

    static {
        factory = new TransactionTemplateFactory();
    }

    public EntityManager(DatabaseHelper databaseHelper) {
        this.databaseHelper = databaseHelper;
        this.processor = new QueryProcessor(databaseHelper.getSchema());
    }

    public <K> int delete(Class<?> rootClass, Collection<K> args) {
        return (int) fireDelete(rootClass, Type.DELETE_MULTIPLE, args);
    }

    public <K> int delete(Class<?> rootClass, K type) {
        return (int) fireDelete(rootClass, Type.DELETE, type);
    }

    private <T> Object fireDelete(Class<?> rootClass, Type type, T obj) {
        DeleteTransactionTemplate<T> template = (DeleteTransactionTemplate) factory.getTransactionTemplate(type);
        if (type == Type.DELETE_MULTIPLE) {
            template.setItems((Collection<T>) obj);
        } else {
            template.setItems(Arrays.asList(obj));
        }
        template.setRootClass(rootClass);

        return executeTemplate(template);
    }

    public <T> T store(T object) {
        return (T) ((List) fireStore(Type.STORE, object)).get(0);
    }

    public <T> List<T> store(Collection<T> objects) {
        return (List<T>) fireStore(Type.STORE_MULTIPLE, objects);
    }

    private <T> Object fireStore(Type type, T object) {
        StoreTransactionTemplate<T> template = (StoreTransactionTemplate<T>) factory.getTransactionTemplate(type);
        if (type == Type.STORE) {
            template.setItems(Arrays.asList(object));
            template.setRootClass(object.getClass());
        } else {
            template.setItems((Collection<T>) object);
            template.setRootClass(((Collection<T>) object).iterator().next().getClass());
        }

        return executeTemplate(template);
    }

    public <T> T query(Class<?> rootClass, Query query, ResultTransformer transformer) {
        return (T) fireQuery(rootClass, query, transformer);
    }

    public <T> T query(Class<?> rootClass, Filter filter) {
        return (T) fireQuery(rootClass, processor.createQuery(rootClass, filter), null);
    }

    private Object fireQuery(Class<?> rootClass, Query query, ResultTransformer transformer) {
        QueryTransactionTemplate template = (QueryTransactionTemplate) factory.getTransactionTemplate(Type.QUERY);
        template.setQuery(query);
        template.setTransformer(transformer);
        template.setRootClass(rootClass);

        return executeTemplate(template);
    }

    private Object executeTemplate(TransactionTemplate template) {
        template.setSchema(databaseHelper.getSchema());
        template.setDb(databaseHelper.getDb());
        template.setProcessor(processor);
        template.setConverter(converter);
        template.execute();

        return template.getResult();
    }
}
