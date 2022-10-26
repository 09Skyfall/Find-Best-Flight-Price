package confrontaVoli;

import java.util.Calendar;

public class SearchQueryMinPeriodOfStay implements SearchQuery {
        private final int minPeriodOfStay;

        public SearchQueryMinPeriodOfStay(int minPeriodOfStay){ this.minPeriodOfStay = minPeriodOfStay; }

        @Override
        public boolean matches(Calendar departureDate, Calendar returnDate) {
            int minPeriodOfStayInMs = this.minPeriodOfStay * 86400000;
            return returnDate.getTimeInMillis() - departureDate.getTimeInMillis() >= minPeriodOfStayInMs;
        }
}


