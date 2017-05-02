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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by juha on 24/04/16.
 *
 * @author juha
 */
public class Predicates {

    private List<Predicate> predicates;
    private List<Sort> orders;
    private Integer pageSize;
    private Integer page;

    public Predicates() {
        this.predicates = new ArrayList<>();
        this.orders = new ArrayList<>();
    }

    List<Predicate> getPredicates() {
        return predicates;
    }

    String getSort() {
        return makeSort();
    }

    String getPage() {
        return makePage();
    }

    public Predicates add(Predicate predicate) {
        this.predicates.add(predicate);

        return this;
    }

    public Predicates sort(Order order, String... cols) {
        for (String col : cols) {
            this.orders.add(new Sort(col, order));
        }

        return this;
    }

    public Predicates setPageSize(Integer pageSize) {
        this.pageSize = pageSize;

        return this;
    }

    public Predicates setPage(Integer page) {
        this.page = page;

        return this;
    }

    private String makeSort() {
        String[] asc = new String[0];
        String[] desc = new String[0];
        for (Sort sort : orders) {
            if (sort.order == Order.ASC) {
                int len = asc.length;
                String[] newAsc = new String[len + 1];
                System.arraycopy(asc, 0, newAsc, 0, len);
                asc = newAsc;
                asc[len] = sort.sort;
            } else {
                int len = desc.length;
                String[] newDesc = new String[len + 1];
                System.arraycopy(desc, 0, newDesc, 0, len);
                desc = newDesc;
                desc[len] = sort.sort;
            }
        }
        if (orders.isEmpty()) { // Sanity check
            return null;
        }
        StringBuilder sortBuilder = new StringBuilder(" ORDER BY ");
        if (asc.length > 0) {
            sortBuilder.append(orderBy(Order.ASC, asc));
            if (desc.length > 0) {
                sortBuilder.append(", ");
            }
        }
        if (desc.length > 0) {
            sortBuilder.append(orderBy(Order.DESC, desc));
        }
        orders.clear();

        return sortBuilder.toString();
    }

    private String orderBy(Order order, String... cols) {
        StringBuilder orderBuilder = new StringBuilder();
        for (String col : cols) {
            orderBuilder.append(col);
            if (!col.equals(cols[cols.length - 1])) {
                orderBuilder.append(", ");
            }
        }
        orderBuilder.append(" ").append(order.toString());
        return orderBuilder.toString();
    }

    private String makePage() {
        StringBuilder page = new StringBuilder(" LIMIT ");
        if (pageSize != null && pageSize > 0) {
            page.append(String.valueOf(pageSize));
            if (this.page != null) {
                page.append(" OFFSET ").append(String.valueOf((this.page * pageSize)));
            }

            return page.toString();
        }

        return null;
    }

    private static class Sort {
        private String sort;
        private Order order;

        public Sort(String sort, Order order) {
            this.sort = sort;
            this.order = order;
        }
    }
}
