package confrontaVoli;

import java.time.DateTimeException;
import java.time.Period;
import java.util.stream.Stream;


public class QueryByMaxPeriodOfStay implements Query{
    private final Period maxPeriod;

    public QueryByMaxPeriodOfStay(Period p){
        if (p.getDays() <= 0) throw new DateTimeException("Maximum period of stay should be formatted in days.\nDays have to be >= 1");
        this.maxPeriod = p;
    }
    @Override
    public Stream<Flight> matches(Stream<Flight> sf){
        float msInADay = 86400000;
        return sf.filter(f -> Math.floor((f.getRetDate().getTime() - f.getDepDate().getTime())/msInADay) <= this.maxPeriod.getDays());
    }
}
