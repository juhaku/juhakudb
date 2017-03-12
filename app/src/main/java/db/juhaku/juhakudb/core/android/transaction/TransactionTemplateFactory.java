package db.juhaku.juhakudb.core.android.transaction;

import android.util.Log;

/**
 * Created by juha on 12/05/16.
 *
 * @author juha
 * @since 1.0.2
 */
public class TransactionTemplateFactory {

    public enum Type {
        DELETE, DELETE_MULTIPLE, STORE, STORE_MULTIPLE, QUERY
    }

    public TransactionTemplate getTransactionTemplate(Type type) {
        switch (type) {
            case DELETE:
                return new DeleteTransactionTemplate<>();
            case DELETE_MULTIPLE:
                return new DeleteTransactionTemplate<>();
            case STORE:
                return new StoreTransactionTemplate<>();
            case STORE_MULTIPLE:
                return new StoreTransactionTemplate<>();
            case QUERY:
                return new QueryTransactionTemplate();
            default:
                Log.w(getClass().getName(), "Transaction template type was not recognized: " + type + ", returning null");
                return null;
        }
    }
}
