package confrontaVoli;

import java.util.stream.Stream;

public class QueryByMaxIndirectPrice implements Query{
    private final int maxPrice;

    public QueryByMaxIndirectPrice(int price){
        this.maxPrice = price;
    }

    @Override
    public Stream<Flight> matches(Stream<Flight> sf){
        return sf.filter(f -> f.getIndirectPrice() <= this.maxPrice);
    }
}
