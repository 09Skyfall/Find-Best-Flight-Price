package confrontaVoli;

import java.util.ArrayList;

public class FlightDatabase {
    private ArrayList<Flight> fl;

    public FlightDatabase(){
        this.fl = new ArrayList<>();
    }

    void add(Flight f){
        this.fl.add(f);
    }
    void remove(Flight f){
        if(this.isDuplicate(f)) {
            this.fl.remove(f);
        }
    }
    ArrayList<Flight> getDatabase(){
        return this.fl;
    }
    //todo: implementare un iterator
    boolean isDuplicate(Flight f){
        return this.fl.stream().anyMatch(flight -> flight.equals(f));
    }
}
