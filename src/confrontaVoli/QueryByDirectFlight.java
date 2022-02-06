package confrontaVoli;

import java.util.function.Predicate;
import java.util.stream.Stream;

public class QueryByDirectFlight implements Query {
    private final boolean isDirect;

    public QueryByDirectFlight(boolean isDirect){
        this.isDirect = isDirect;
    }

    @Override
    public Stream<Flight> matches(Stream<Flight> sf){
        Predicate<Flight> p;
        if(this.isDirect){
            p = f -> f.getDirectPrice() != 0;
        } else {
            p = f -> f.getIndirectPrice() != 0;
        }
        return sf.filter(p);
    }
}
