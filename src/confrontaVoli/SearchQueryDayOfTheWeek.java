package confrontaVoli;

import java.util.ArrayList;
import java.util.Calendar;

public class SearchQueryDayOfTheWeek implements SearchQuery{
    private final ArrayList<Integer> days;

    public SearchQueryDayOfTheWeek (int day){
        this.days = new ArrayList<Integer>(day);
    }
    public SearchQueryDayOfTheWeek (ArrayList<Integer> days){
        this.days = days;
    }

    @Override
    public boolean matches(Calendar departureDate, Calendar returnDate) {
        Calendar depDateClone = (Calendar)departureDate.clone();
        while(!depDateClone.after(returnDate)){
            if(!days.contains(depDateClone.get(Calendar.DAY_OF_WEEK)))
                return false;
            depDateClone.add(Calendar.DAY_OF_MONTH, 1);
        }
        return true;
    }
}
