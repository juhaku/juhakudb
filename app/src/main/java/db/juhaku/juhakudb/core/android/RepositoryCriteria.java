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
package db.juhaku.juhakudb.core.android;

import android.util.Log;

import db.juhaku.juhakudb.annotation.Repository;
import db.juhaku.juhakudb.core.Criteria;

/**
 * Created by juha on 16/04/16.
 *<p>This criteria is used to resolve repository locations.</p>
 * @author juha
 *
 * @since 1.0.2
 */
public class RepositoryCriteria implements Criteria<String> {

    private String[] paths;

    public RepositoryCriteria(String... paths) {
        this.paths = paths;
    }

    @Override
    public boolean meetCriteria(String type) {
        for (String path : paths) {
            if (type.startsWith(path)) {
                try {
                    Class<?> clazz = Class.forName(type);
                    if (clazz.isInterface() && clazz.isAnnotationPresent(Repository.class)) {
                        Log.d(getClass().getName(), "path:" + path + " type:" + type);
                        Class<?> impl = clazz.getAnnotation(Repository.class).value();
                        if (impl == null) {
                            Log.e(getClass().getName(), "No implementation provided, implementation is mandatory");
                            return false;
                        }
                        return true;
                    }
                } catch (ClassNotFoundException e) {
                    Log.w(getClass().getName(), "class could not be initialized, ", e);
                    return false;
                }
            }
        }

        return false;
    }
}
