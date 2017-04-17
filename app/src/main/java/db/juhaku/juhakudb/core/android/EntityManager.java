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
