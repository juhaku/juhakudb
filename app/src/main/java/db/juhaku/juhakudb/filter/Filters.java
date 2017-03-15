package db.juhaku.juhakudb.filter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by juha on 16/04/16.
 *
 * @author juha
 */
public class Filters implements Filter {

    private List<Filter> filters;

    public Filters() {
        this.filters = new ArrayList<>();
    }

    public Filters(Filter... filters) {
        this.filters = new ArrayList<>(Arrays.asList(filters));
    }

    public Filters add(Filter filter) {
        filters.add(filter);

        return this;
    }

    @Override
    public void filter(Root root, Predicates predicates) {
        for (Filter filter : filters) {
            filter.filter(root, predicates);
        }
    }
}
