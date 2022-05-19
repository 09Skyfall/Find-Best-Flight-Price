package confrontaVoli;

import java.util.ArrayList;
import java.util.stream.Stream;

public class FlightDatabase {
    private ArrayList<Flight> fl;

    public FlightDatabase(){
        this.fl = new ArrayList<>();
    }

    void add(Flight f){
        this.fl.add(f);
    }
    Stream<Flight> getDatabase(){
        return this.fl.stream();
    }
}
