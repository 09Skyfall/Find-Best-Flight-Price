package confrontaVoli;

import org.json.simple.JSONObject;

import java.util.Date;

public class Flight {
    private final String name;
    private final Date depDate;
    private final Date retDate;
    private final boolean isDirect;
    private final long directPrice;
    private final long indirectPrice;

    private Flight(FlightBuilder fb){
        this.name = fb.name;
        this.isDirect = fb.isDirect;
        this.directPrice = fb.directPrice;
        this.indirectPrice = fb.indirectPrice;
        this.depDate = fb.depDate;
        this.retDate = fb.retDate;
    }

    public static FlightBuilder parse(JSONObject flight) {
        FlightBuilder fb = FlightBuilder.newBuilder();

        if (flight.containsKey("IndirectPrice")) {
            fb.withIndirectPrice((long) flight.get("IndirectPrice"));
        }
        if (flight.containsKey("DirectPrice")) {
            fb.withDirectPrice((long) flight.get("DirectPrice"));
        }
        if (flight.containsKey("Name")) {
            fb.withName((String)flight.get("Name"));
        }
        if (flight.containsKey("Direct")) {
            fb.withIsDirect((boolean) flight.get("Direct"));
        }

        return fb;
    }

    // GETTERS
    public String getName() {
        return this.name;
    }
    public long getDirectPrice(){
        return this.directPrice;
    }
    public long getIndirectPrice(){
        return this.indirectPrice;
    }
    public String getDestination() { return this.name; }
    public Date getDepDate(){ return this.depDate; }
    public Date getRetDate(){ return this.retDate; }
    public boolean isDirect(){
        return this.isDirect;
    }

    @Override
    public String toString(){
        return String.format("  {\n   \"destination\": \"%s\",\n" +
                                        "   \"directPrice\": \"%s\",\n" +
                                        "   \"departureDate\": \"%s\",\n" +
                                        "   \"returnDate\": \"%s\"\n  }", this.name, this.directPrice, this.depDate.toString(), this.retDate.toString());
    }
    @Override
    public boolean equals(Object f) {
        if(f.getClass() == Flight.class) {
            return this.name.equals(((Flight)f).getName()) &&
                    this.depDate == ((Flight)f).getDepDate() &&
                    this.retDate == ((Flight)f).getRetDate() &&
                    this.isDirect == ((Flight)f).isDirect() &&
                    this.directPrice == ((Flight)f).getDirectPrice() &&
                    this.indirectPrice == ((Flight)f).getIndirectPrice();
        } else return false;
    }

    // BUILDER
    public static class FlightBuilder {
        private String name;
        private Date depDate;
        private Date retDate;
        private boolean isDirect;
        private long directPrice;
        private long indirectPrice;

        public static FlightBuilder newBuilder(){
           return new FlightBuilder();
        }
        public Flight build(){
            return new Flight(this);
        }

        FlightBuilder withName(String name){
            this.name = name;
            return this;
        }
        FlightBuilder withDirectPrice(long price){
            this.directPrice = price;
            return this;
        }
        FlightBuilder withIndirectPrice(long price){
            this.indirectPrice = price;
            return this;
        }
        FlightBuilder withIsDirect(boolean isDirect){
            this.isDirect = isDirect;
            return this;
        }
        FlightBuilder withDepDate(Date depDate){
            this.depDate = depDate;
            return this;
        }
        FlightBuilder withRetDate(Date retDate){
            this.retDate = retDate;
            return this;
        }

    }
}
