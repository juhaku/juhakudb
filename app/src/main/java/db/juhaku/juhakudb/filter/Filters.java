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
package db.juhaku.juhakudb.filter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by juha on 16/04/16.
 *
 * <p>Filters is a grouping of {@link Filter} objects. Filters can be used to create more modular
 * SQL queries against database without all criteria being inside one filter object.</p>
 *
 * @author juha
 */
public class Filters implements Filter {

    private List<Filter> filters;

    /**
     * Instantiates new empty filters grouping.
     */
    public Filters() {
        this.filters = new ArrayList<>();
    }

    /**
     * Instantiates new filters grouping from given array of filter objects. E.g. new Filters(new Filter(){...},
     * new Filter(...){}, ...).
     *
     * @param filters
     */
    public Filters(Filter... filters) {
        this();

        for (Filter filter : filters) {

            // If provided filter is actually another Filters object add all filters from it.
            if (Filters.class.isAssignableFrom(filter.getClass())) {
                this.filters.addAll(((Filters) filter).filters);

            } else {
                this.filters.add(filter);
            }

        }
    }

    /**
     * Add new filter to filters grouping.
     *
     * @param filter Instance of {@link Filter} to add for grouped query.
     *
     * @return Current filters grouping.
     */
    public Filters add(Filter filter) {
        filters.add(filter);

        return this;
    }

    @Override
    public void filter(Root root, PredicateBuilder builder) {
        for (Filter filter : filters) {
            filter.filter(root, builder);
        }
    }

    /**
     * Create new filters grouping of filter objects from provided filter array. This is alternative
     * way for calling constructor {@link #Filters(Filter[])}.
     *
     * @param filters array of {@link Filter} to create grouping from.
     *
     * @return Filters grouping for SQL queries.
     *
     * @since 2.1.0
     */
    public static Filters of(Filter... filters) {
        return new Filters(filters);
    }
}
