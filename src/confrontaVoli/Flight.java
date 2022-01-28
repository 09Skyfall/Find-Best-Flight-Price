package confrontaVoli;

import java.util.Date;

public class Flight {
    private String name;
    private Date depDate;
    private Date retDate;
    private boolean isDirect;
    private int directPrice;
    private int indirectPrice;

    private Flight(FlightBuilder fb){
        this.name = fb.name;
        this.isDirect = fb.isDirect;
        this.directPrice = fb.directPrice;
        this.indirectPrice = fb.indirectPrice;
        this.depDate = fb.depDate;
        this.retDate = fb.retDate;
    }
    public static FlightBuilder parse(String s) {
        FlightBuilder fb = FlightBuilder.newBuilder();

        int wordCount = 0, length = s.length(), index = 0;
        String key = "", value = "";

        while(index < length){
            char ch = s.charAt(index);
            if(ch == '"' || ch == ':' || ch == ',' || Character.isWhitespace(ch)){ // todo: if it's not alphanumeric then
                index++;
                if (wordCount == 0 && !key.isBlank()){
                    wordCount++;
                }
                if (wordCount == 1 && !value.isBlank()){
                    wordCount++;
                }
            }
            else if (wordCount == 0) {
                key += ch; // todo: StringBuilder??
                index++;
            }
            else if(wordCount == 1){
                value += ch;
                index++;
            }
            else if(wordCount == 2){
                switch(key){
                    case "Direct": fb.withIsDirect(value.equals("true")); break;
                    case "Name": fb.withName(value); break;
                    case "DirectPrice": fb.withDirectPrice(Integer.parseInt(value)); break;
                    case "IndirectPrice": fb.withIndirectPrice(Integer.parseInt(value)); break;
                }
                wordCount = 0;
                key = "";
                value = "";
            }
        }
        return fb;
    }

    // GETTERS
    public int getDirectPrice(){
        return this.directPrice;
    }
    public int getIndirectPrice(){
        return this.indirectPrice;
    }
    public Date getDepDate(){ return this.depDate; }
    public Date getRetDate(){ return this.retDate; }
    public boolean isDirect(){
        return this.isDirect;
    }

    @Override
    public String toString(){
        return String.format("Destination: %s\nDirect price: %d, Indirect price: %d\nDeparture date: %s, Return date: %s\n\n" +
                            "============================================================================", this.name, this.directPrice,
                            this.indirectPrice, this.depDate.toString(), this.retDate.toString());
    }

    public static class FlightBuilder {
        private String name;
        private Date depDate;
        private Date retDate;
        private boolean isDirect;
        private int directPrice;
        private int indirectPrice;

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
        FlightBuilder withDirectPrice(int price){
            this.directPrice = price;
            return this;
        }
        FlightBuilder withIndirectPrice(int price){
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
