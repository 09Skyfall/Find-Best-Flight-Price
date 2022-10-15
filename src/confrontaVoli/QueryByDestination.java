package confrontaVoli;

import java.util.ArrayList;
import java.util.stream.Stream;

public class QueryByDestination implements Query {
    private final ArrayList<String> destinations;

    public QueryByDestination(ArrayList<String> destinations){
        this.destinations = destinations;
    }

    @Override
    public Stream<Flight> matches(Stream<Flight> sf){
        return sf.filter(f -> anyMatch(f.getDestination()));
    }

    private boolean anyMatch(String destination) {
        for(String d : this.destinations){
            if (d.equals(destination)){
                return true;
            }
        }
        return false;
    }


}
