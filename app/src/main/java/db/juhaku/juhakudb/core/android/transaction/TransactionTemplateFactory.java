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
