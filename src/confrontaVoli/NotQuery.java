package confrontaVoli;

import java.util.List;
import java.util.stream.Stream;

public class NotQuery implements Query{
    private final Query query;

    public NotQuery(Query q){
        this.query = q;
    }
    @Override
    public Stream<Flight> matches(Stream<Flight> sf){
        List<Flight> copyOfSf = sf.toList();
        Stream.Builder<Flight> sb1 = Stream.builder();
        Stream.Builder<Flight> sb2 = Stream.builder();
        for(Flight f : copyOfSf){
            sb1.add(f);
            sb2.add(f);
        }
        Stream<Flight> sf1 = sb1.build();
        Stream<Flight> sf2 = sb2.build();

        List<Flight> filteredStream = query.matches(sf1).toList();
        return sf2.filter(f -> noneMatch(filteredStream, f));
    }

    private boolean noneMatch(List<Flight> fl, Flight f){
        for(Flight x : fl){
            if(x.equals(f))
                return false;
        }
        return true;
    }

}
