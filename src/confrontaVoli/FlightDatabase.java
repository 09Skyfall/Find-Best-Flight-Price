package confrontaVoli;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.stream.Stream;

public class FlightDatabase {
    private ArrayList<Flight> fl;

    public FlightDatabase(){
        this.fl = new ArrayList<>();
    }
    public FlightDatabase(ArrayList<Flight> l){
        this.fl = l;
    }

    void add(Flight f){
        this.fl.add(f);
    }
    Stream<Flight> getDatabase(){
        return this.fl.stream();
    }

    public String toJSON(){
        Iterator<Flight> it = this.fl.iterator();
        String json = "{\n \"flights\": [\n";

        while(it.hasNext()){
            json += it.next().toString() + (it.hasNext() ? ",\n" : "\n");
        }
        return json + " ]\n}";
    }

    public boolean isEmpty() {
        return this.fl.isEmpty();
    }
    public int size() {
        return this.fl.size();
    }
}
