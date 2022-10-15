package confrontaVoli;

import org.jetbrains.annotations.NotNull;

import java.util.Calendar;
import java.util.List;

public class SearchQueryAndQuery implements SearchQuery {
    private final List<SearchQuery> queryList;

    public SearchQueryAndQuery(@NotNull List<SearchQuery> ql){
        this.queryList = ql;
    }

    @Override
    public boolean matches(Calendar departureDate, Calendar returnDate) {
        for(SearchQuery sq : this.queryList){
            if(!sq.matches(departureDate, returnDate))
                return false;
        }
        return true;
    }
}
