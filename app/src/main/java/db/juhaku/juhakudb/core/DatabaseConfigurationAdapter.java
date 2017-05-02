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
package db.juhaku.juhakudb.core;

import db.juhaku.juhakudb.core.android.DatabaseManager;

/**
 * Created by juha on 04/12/15.
 *<p>This interface should be implemented and passed to {@link DatabaseManager} what handles
 * configuration of the database for user. This interface provides simple and easy way to
 * configure database.</p>
 *
 * @author Juha Kukkonen
 *
 * @since 1.0.2
 */
public interface DatabaseConfigurationAdapter {

    /**
     * Implement this method to provide configuration for database and schema.
     * @param configuration instance of {@link DatabaseConfiguration}
     *
     * @since 1.0.2
     */
    void configure(DatabaseConfiguration configuration);
}
