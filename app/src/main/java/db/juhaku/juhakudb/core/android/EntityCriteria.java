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

import android.util.Log;

import javax.persistence.Entity;

import db.juhaku.juhakudb.core.Criteria;

/**
 * Created by juha on 16/04/16.
 *<p>This criteria is used to resolve entities for database schema creation.</p>
 * @author juha
 *
 * @since 1.0.2
 */
public class EntityCriteria implements Criteria<String> {

    private String[] packages;

    public EntityCriteria(String[] packages) {
        this.packages = packages;
    }

    @Override
    public boolean meetCriteria(String type) {
        for (String path : packages) {
            if (type.startsWith(path)) {

                // If class is not an enum and it has Entity annotation it should be entity.
                Class<?> clazz;
                if ((clazz = initializeClass(type)) != null) {
                    if (!Enum.class.isAssignableFrom(clazz) && clazz.isAnnotationPresent(Entity.class)) {

                        return true;
                    }
                }

            }
        }

        return false;
    }

    private static Class<?> initializeClass(String name) {
        try {
            return Class.forName(name);
        } catch (Exception e) {
            Log.e(EntityCriteria.class.getName(), "Could not initialize class by name: " +
                    name + ", null returned!", e);
        }

        return null;
    }
}
