package confrontaVoli;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.simple.*;
import org.json.simple.parser.JSONParser;

import java.awt.*;
import java.awt.event.InputEvent;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.*;
import java.text.DateFormat;
import java.util.*;
import java.net.http.*;
import java.util.List;
import java.util.stream.Collectors;


public class API {
    private static final String captcha = "https://www.skyscanner.it/sttc/px/captcha-v2/index.html";
    private static final String prefix = "https://www.skyscanner.it/g/browse-view-bff/dataservices/browse/v3/bvweb/IT/EUR/it-IT/destinations/VENI/anywhere/";
    private static final String suffix = "/?apikey=8aa374f4e28e4664bf268f850f767535";

    public static void main(String[] args) throws IOException, InterruptedException {
        // Creazione SearchQuery
        SearchQuery maxPeriodOfStayQuery = new SearchQueryMaxPeriodOfStay(2);
        SearchQuery minPeriodOfStayQuery = new SearchQueryMinPeriodOfStay(1);
        SearchQuery dayOfTheWeekQuery= new SearchQueryDayOfTheWeek(new ArrayList<>(Arrays.asList(Calendar.MONDAY, Calendar.SATURDAY, Calendar.SUNDAY)));
        SearchQuery andSearchQuery = new SearchQueryAndQuery(Arrays.asList(maxPeriodOfStayQuery, dayOfTheWeekQuery, minPeriodOfStayQuery));

        // Creazione FilterQuery
        //Query notDestination = new NotQuery(new QueryByDestination(new ArrayList<>(List.of("Italia"))));
        Query notDestination = (new QueryByDestination(new ArrayList<>(List.of("Albania"))));
        Query directPriceQ = new QueryByMaxDirectPrice(45);
        //Query indirectPriceQ = new QueryByMaxIndirectPrice(50);
        Query directFlightQ = new QueryByDirectFlight(true);
        Query andQuery = new AndQuery(Arrays.asList(notDestination, directPriceQ, directFlightQ));

        // date
        Calendar startDate = Calendar.getInstance();
        startDate.set(2022, Calendar.NOVEMBER, 1 - 1);

        Calendar endDate = Calendar.getInstance();
        endDate.set(2023, Calendar.APRIL, 30);

        FlightDatabase fdb = findBestMatch(andQuery, andSearchQuery, startDate, endDate);

        if(fdb.isEmpty()) {
            System.out.println("Non sono stati trovati voli");
        } else {
            // inizializzazione I/O
            String path = "C:\\\\Users\\Sky\\Desktop\\voli.json";
            File out = createFile(path);

            // scrivi output
            FileWriter fileWriter = new FileWriter(out.getPath());
            fileWriter.append(fdb.toJSON());
            fileWriter.close();

            System.out.format("Sono stati trovati %d voli.", fdb.size());
        }
    }

    private static File createFile(String path) throws IOException{
        String fileFormat = path.substring(path.lastIndexOf('.'));
        File out = null;
        boolean fileCreated = false;
        int i = 0;

        while(!fileCreated) {
            out = new File(path);
            if (out.createNewFile()) {
                System.out.println("File created successfully");
                fileCreated = true;
            } else {
                path = path.substring(0, path.length() - fileFormat.length() - (i == 0? 0 : ("" + i).length())) + (++i) + fileFormat;
            }
        }
        return out;
    }

    /**
     *
     * @param q
     * @param startDate
     * @param endDate (REQUIRED non null, REQUIRED to be not the same instance as startDate)
     * @return flight database that matches the given query.
     * @throws IOException
     * @throws InterruptedException
     */
    public static FlightDatabase findBestMatch(@Nullable Query q, @NotNull SearchQuery sq, @Nullable Calendar startDate, @NotNull Calendar endDate)
            throws IOException, InterruptedException {
        if (startDate == null)
            startDate = Calendar.getInstance();
        startDate.add(Calendar.DAY_OF_MONTH, 1);

        FlightDatabase fdb = populateDatabase(startDate, endDate, sq);

        if (q == null) {
            return fdb;
        } else {
            return new FlightDatabase(q.matches(fdb.getDatabase()).collect(Collectors.toCollection(ArrayList<Flight>::new)));
        }
    }
    /**
     * @param startDate: (REQUIRED non null) date from which flights should be searched for.
     * @param endDate: (REQUIRED non null) date until which flights should be searched for
     * @param sq: (REQUIRED non null)
     * @return a database containing flight with all possible combinations of dates, ranging from startDate
     *         and endDate. Each flight's return date should be no longer than periodOfStayInMs days after its departure date
     * @throws IOException
     * @throws InterruptedException
     */
    private static FlightDatabase populateDatabase(@NotNull Calendar startDate, @NotNull Calendar endDate, @NotNull SearchQuery sq)
            throws IOException, InterruptedException {

        startDate.set(Calendar.HOUR_OF_DAY, 0); startDate.set(Calendar.MINUTE, 0); startDate.set(Calendar.SECOND, 0); startDate.set(Calendar.MILLISECOND, 0);
        endDate.set(Calendar.HOUR_OF_DAY, 0); endDate.set(Calendar.MINUTE, 0); endDate.set(Calendar.SECOND, 0); endDate.set(Calendar.MILLISECOND, 0);

        FlightDatabase fdb = new FlightDatabase();
        int ms = 100;

        System.out.println("Fetching info...\nDeparture Date:    Return Date:");

        for(; !startDate.after(endDate); startDate.add(Calendar.DAY_OF_MONTH, 1)){
            boolean requestBlocked = false;
            for(Calendar returnDate = (Calendar)startDate.clone(); !returnDate.after(endDate); returnDate.add(Calendar.DAY_OF_MONTH, 1)){

                    if(sq.matches(startDate, returnDate)) {
                        String depDate = getDateString(startDate);
                        String retDate = getDateString(returnDate);

                        String url = prefix + depDate + "/" + retDate + suffix;
                        System.out.format("  %s         %s\n", depDate, retDate);

                        JSONObject json;
                        try {
                            json = getBody(url);
                        } catch (RequestBlockedException rbe) {
                            rbe.printStackTrace();
                            requestBlocked = true;
                            break;
                        }

                        List<Flight> lf = createFlights((JSONArray) json.get("PlacePrices"), startDate.getTime(), returnDate.getTime());
                        for (Flight f : lf) {
                            if (!(f.getDirectPrice() == 0 && f.getIndirectPrice() == 0)) {
                                fdb.add(f);
                            }
                        }
                        if (ms < 2000) {
                            ms += 100;
                        }
                        Thread.sleep(ms);

                    }
            }
            if(requestBlocked)
                break;
        }
        return fdb;
    }

    private static String getDateString(Calendar date) {
        String d = (DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.JAPAN)).format(date.getTime());
        return d.replace('/', '-');
    }

    /**
     * @param body: string containing all flights information
     * @param depDate: departure date of flights
     * @param retDate: return date of flights
     * @return list of flights extrapolated from body, with departure date = depDate and return date = retDate
     */
    private static ArrayList<Flight> createFlights(JSONArray flights, Date depDate, Date retDate) {
        ArrayList<Flight> flightsList = new ArrayList<>();

        for(Object flight : flights) {
            Flight f = Flight.parse((JSONObject) flight).withDepDate(depDate).withRetDate(retDate).build();
            flightsList.add(f);
        }

        return flightsList;
    }

    /**
     * @param url URL containing all flights information
     * @return string containing all flights information
     * @throws RequestBlockedException if GET request gets blocked
     * @throws IOException
     * @throws InterruptedException
     */
    private static JSONObject getBody(String url) throws IOException, InterruptedException, RequestBlockedException{

        HttpClient client = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .headers("Accept", "application/json, text/javascript, */*; q=0.01",
                         "Cookie", "_pxhd=gfOu6/RYJ94UrFXKgmuVPEc3NZxzMs4BK2nrKnFTrAtbR69sffLEh1R3C0mm5kiTDgBUrmxtFY0CQ6t0XAAqpg==:HjeZrb1oqD-kzzWFkVmT6-DDou2ToLsNv5Yo9smT3JTRmLIsrEYXh8fnIMA8PqEEAPMPri19/ysU8iTIIhplyg1IHTlb8gK5wsNs22hz/P4=; traveller_context=3f3b8373-a17f-4f23-a025-9c754ab9ea78; ssab=AAExperiment_V9:::b&BD_Flight_DV_OC_Advert_View_Hotels_V1:::a&BD_HotelDetail_RoomTypeGrouping_Desktop_V2:::b&Banana_Radar_Hook_V2:::a&DynamicNav_V5:::a&EUR_flights_dbook_coupon_flow_V2:::b&Migrate_To_New_Location_V5:::a&WPT_Footer_Flags_Version_Rollout_V47:::b&autosuggest_proxy_experiment_split_ap_northeast_1_V3:::a&autosuggest_proxy_experiment_split_ap_southeast_1_V4:::a&autosuggest_proxy_experiment_split_eu_central_1_V5:::a&autosuggest_proxy_experiment_split_eu_west_1_V4:::a&dbook_aege_trafficcontrol_webV1_V3:::a&dbook_basi_trafficcontrol_web_V2:::a&dbook_invo_trafficcontrol_web_V1:::a&dbook_norw_trafficcontrol_webV1_V3:::a&dbook_rexa_trafficcontrol_web_V2:::a&fps_mr_fqs_flights_ranking_haumea_np_V3:::c&global_inline_test_v2_V3:::k&mr_migration_proxy_test_always_on_V4:::a&rts_magpie_soow_data_collection_V8:::budgetscheduled&travel_widgets_loader_path_traffic_allocation_V6:::a; experiment_allocation_id=e3d817d99b716400254b9206bd6b2197763a8cd72c39e2f5d98a91284728f146; abgroup=11508758; ssculture=locale:::it-IT&market:::IT&currency:::EUR; __Secure-ska=dded7952-f4dc-40b0-b11c-cded5bf05af4; device_guid=dded7952-f4dc-40b0-b11c-cded5bf05af4; preferences=3f3b8373a17f4f23a0259c754ab9ea78; gdpr=information&adverts&version:::2; scanner=currency:::EUR&legs:::TSF|2022-02-09|PARI|PARI|2022-02-09|TSF&to&oym:::2202&oday:::07&wy:::0&iym:::2202&iday:::07&from:::VENI; _pxvid=83a18243-5b88-11ec-9dfd-6a4a75655554; ssaboverrides=; _csrf=Sj321ITth2uKg4_ihPBnQpF6",
                         "Referer", "https://www.skyscanner.it/trasporti/voli-da/veni/220207/220207/?adults=2&adultsv2=2&cabinclass=economy&children=0&childrenv2=&inboundaltsenabled=false&infants=0&originentityid=27547373&outboundaltsenabled=false&preferdirects=false&ref=home&rtn=1",
                         "User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:96.0) Gecko/20100101 Firefox/96.0"
                        )
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.body().contains("blocked")) {
            throw new RequestBlockedException("The GET request has been blocked");
        } else {
            return (JSONObject) JSONValue.parse(response.body());
        }
    }
}
