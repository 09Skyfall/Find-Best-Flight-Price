package confrontaVoli;

import java.util.Calendar;

/**
 * Query to be used during the search for flights, that is before we have any actual information about the flights
 * (before the GET request).
 * Note: the only information available at that moment will be the departure date and return date
 */
public interface SearchQuery {
    boolean matches(Calendar departureDate, Calendar returnDate);
}
