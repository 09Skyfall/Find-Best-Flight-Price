package confrontaVoli;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.*;
import java.text.DateFormat;
import java.util.*;
import java.net.http.*;
import java.util.List;
import java.util.stream.Collectors;

import static java.lang.System.exit;

//TODO: launch exception if http request returns "redirect, reason: blocked"

public class API {
    private static final String prefix = "https://www.skyscanner.it/g/browse-view-bff/dataservices/browse/v3/bvweb/IT/EUR/it-IT/destinations/VENI/";
    private static final String suffix = "/?apikey=8aa374f4e28e4664bf268f850f767535";

    public static void main(String[] args) throws IOException, InterruptedException, RequestBlockedException {
        //Query q1 = new QueryByMaxPeriodOfStay(Period.ofDays(2));
        //Query q2 = new QueryByMaxDirectPrice(50);
        //Query q3 = new QueryByMaxIndirectPrice(50);
        //ArrayList<Query> ql = new ArrayList<>(); ql.add(q1); // ql.add(q2); ql.add(q3);
        //Query andQuery = new AndQuery(ql);

        Calendar endDate = Calendar.getInstance(); endDate.add(Calendar.DAY_OF_MONTH, 20);
        List<Flight> fl = findBestMatch(null, null, endDate, null);
        for(Flight f : fl){
            System.out.println(f);
        }


    }

    /**
     *
     * @param q
     * @param startDate
     * @param endDate (REQUIRED non null)
     * @param destination: destination the flight should be directed to. If null, any destination will be searched for
     * @return list of flights matching given query.
     * @throws IOException
     * @throws InterruptedException
     */
    public static ArrayList<Flight> findBestMatch(@Nullable Query q, @Nullable Calendar startDate, @NotNull Calendar endDate, @Nullable String destination)
                                                                throws IOException, InterruptedException, RequestBlockedException {
        if (startDate == null) startDate = Calendar.getInstance();
        if(destination == null) destination = "anywhere";
        FlightDatabase fdb = populateDatabase(startDate, endDate, destination);

        if (q == null) {
            return fdb.getDatabase();
        } else {
            return q.matches(fdb.getDatabase().stream()).collect(Collectors.toCollection(ArrayList<Flight>::new));
        }
    }
    /**
     * @param startDate: (REQUIRED non null) date from which flights should be searched for.
     * @param endDate: (REQUIRED non null) date until which flights should be searched for
     * @param destination: (REQUIRED non null) destination the flight should be directed to.
     * @return a database containing flight with all possible combinations of dates, ranging from startDate
     *         and endDate. Each flight's return date should be no longer than maxRange days after its departure date
     * @throws IOException
     * @throws InterruptedException
     */
    private static FlightDatabase populateDatabase(@NotNull Calendar startDate, @NotNull Calendar endDate, @NotNull String destination)
                                                                    throws IOException, InterruptedException, RequestBlockedException {
        FlightDatabase fdb = new FlightDatabase();
        for(; !startDate.after(endDate); startDate.add(Calendar.DAY_OF_MONTH, 1)){
            for( Calendar returnDate = (Calendar) startDate.clone();  !returnDate.after(endDate); returnDate.add(Calendar.DAY_OF_MONTH, 1)){

                String depDate = (DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.JAPAN)).format(startDate.getTime());
                String retDate = (DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.JAPAN)).format(returnDate.getTime());
                depDate = depDate.replace('/', '-');
                retDate = retDate.replace('/', '-');

                String url = prefix + destination + "/" + depDate + "/" + retDate + suffix;
                String body = getBody(url);
                if (body.contains("blocked")) throw new RequestBlockedException("The GET request has been blocked");

                List<Flight> lf = createFlights(body, startDate.getTime(), returnDate.getTime());
                for(Flight f : lf){
                    if (!(f.getDirectPrice() == 0 && f.getIndirectPrice() == 0)) {
                        fdb.add(f);
                    }
                }
            }
        }
        return fdb;
    }

    /**
     * @param body: string containing all flights information
     * @param depDate: departure date of flights
     * @param retDate: return date of flights
     * @return list of flights extrapolated from body, with departure date = depDate and return date = retDate
     */
    private static ArrayList<Flight> createFlights(String body, Date depDate, Date retDate) {
        ArrayList<Flight> flightsList = new ArrayList<>();

        //skips first part of body
        body = body.substring(body.indexOf('{')+1);

        while(body.indexOf('{') != -1) {
            String flightString = body.substring(body.indexOf('{') + 1, body.indexOf('}'));
            Flight f = Flight.parse(flightString).withDepDate(depDate).withRetDate(retDate).build();
            flightsList.add(f);

            body = body.substring(body.indexOf('}')+1);
        }
        return flightsList;
    }

    /**
     * @param url URL containing all flights information
     * @return string containing all flights information
     * @throws IOException
     * @throws InterruptedException
     */
    private static String getBody(String url) throws IOException, InterruptedException{
        HttpClient client = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .setHeader("user-agent","Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:96.0) Gecko/20100101 Firefox/96.0")
                .GET()
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        return response.body();
    }
}
