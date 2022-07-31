package confrontaVoli;

import java.util.Calendar;

public class SearchQueryMaxPeriodOfStay implements SearchQuery{
    private final int maxPeriodOfStay;

    public SearchQueryMaxPeriodOfStay(int maxPeriodOfStay){ this.maxPeriodOfStay = maxPeriodOfStay; }

    @Override
    public boolean matches(Calendar departureDate, Calendar returnDate) {
        int maxPeriodOfStayInMs = this.maxPeriodOfStay * 86400000;
        return returnDate.getTimeInMillis() - departureDate.getTimeInMillis() <= maxPeriodOfStayInMs;
    }
}
