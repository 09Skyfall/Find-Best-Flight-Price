package confrontaVoli;

import java.util.stream.Stream;

public class QueryByMaxDirectPrice implements Query {
    private final int maxPrice;

    public QueryByMaxDirectPrice(int price){
        this.maxPrice = price;
    }

    @Override
    public Stream<Flight> matches(Stream<Flight> sf){
        return sf.filter(f -> f.getDirectPrice() <= this.maxPrice);
    }


}
