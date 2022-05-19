package confrontaVoli;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.awt.event.InputEvent;
import java.io.BufferedWriter;
import java.io.IOException;
import java.lang.reflect.Array;
import java.net.*;
import java.text.DateFormat;
import java.time.Period;
import java.util.*;
import java.net.http.*;
import java.util.List;
import java.util.stream.Collectors;


public class API {
    private static final String captcha = "https://www.skyscanner.it/sttc/px/captcha-v2/index.html";
    private static final String prefix = "https://www.skyscanner.it/g/browse-view-bff/dataservices/browse/v3/bvweb/IT/EUR/it-IT/destinations/VENI/anywhere/";
    private static final String suffix = "/?apikey=8aa374f4e28e4664bf268f850f767535";

    public static void main(String[] args) throws IOException, InterruptedException, RequestBlockedException {
        // Creazione Query
        ArrayList<String> destinations = new ArrayList<>();
            destinations.add("Francia");
            destinations.add("Irlanda");
        Query destinationQ = new QueryByDestination(destinations);
        Query directPriceQ = new QueryByMaxDirectPrice(50);
        Query indirectPriceQ = new QueryByMaxIndirectPrice(50);
        Query directFlightQ = new QueryByDirectFlight(true);
        ArrayList<Query> queryList = new ArrayList<>();
            queryList.add(destinationQ);
            queryList.add(directPriceQ);
            queryList.add(indirectPriceQ);
            queryList.add(directFlightQ);
        Query andQuery = new AndQuery(queryList);

        Calendar startDate = Calendar.getInstance();
            startDate.add(Calendar.DAY_OF_MONTH, 1);
        Calendar endDate = Calendar.getInstance();
            endDate.add(Calendar.DAY_OF_MONTH, 90);

        List<Flight> fl = findBestMatch(andQuery, null, endDate, 2);

        System.out.println("\n\n");
        int nFlights = 0;
        for(Flight f : fl){
            nFlights += 1;
            System.out.println(f);
        }
        System.out.format("Sono stati trovati %d voli.", nFlights);
    }

    /**
     *
     * @param q
     * @param startDate
     * @param endDate (REQUIRED non null, REQUIRED to be not the same instance as startDate)
     * @param maxPeriodOfStay:
     * @return list of flights matching given query.
     * @throws IOException
     * @throws InterruptedException
     */
    public static ArrayList<Flight> findBestMatch(@Nullable Query q, @Nullable Calendar startDate, @NotNull Calendar endDate, @NotNull int maxPeriodOfStay)
            throws IOException, InterruptedException, RequestBlockedException {
        if (startDate == null)
            startDate = Calendar.getInstance();
        startDate.add(Calendar.DAY_OF_MONTH, 1);

        FlightDatabase fdb = populateDatabase(startDate, endDate, maxPeriodOfStay*86400000);

        if (q == null) {
            return fdb.getDatabase().collect(Collectors.toCollection(ArrayList<Flight>::new));
        } else {
            return q.matches(fdb.getDatabase()).collect(Collectors.toCollection(ArrayList<Flight>::new));
        }
    }
    /**
     * @param startDate: (REQUIRED non null) date from which flights should be searched for.
     * @param endDate: (REQUIRED non null) date until which flights should be searched for
     * @param maxPeriodOfStayInMs: (REQUIRED non null)
     * @return a database containing flight with all possible combinations of dates, ranging from startDate
     *         and endDate. Each flight's return date should be no longer than periodOfStayInMs days after its departure date
     * @throws IOException
     * @throws InterruptedException
     */
    private static FlightDatabase populateDatabase(@NotNull Calendar startDate, @NotNull Calendar endDate, @NotNull int maxPeriodOfStayInMs)
            throws IOException, InterruptedException, RequestBlockedException {

        startDate.set(Calendar.HOUR_OF_DAY, 0); startDate.set(Calendar.MINUTE, 0); startDate.set(Calendar.SECOND, 0); startDate.set(Calendar.MILLISECOND, 0);
        endDate.set(Calendar.HOUR_OF_DAY, 0); endDate.set(Calendar.MINUTE, 0); endDate.set(Calendar.SECOND, 0); endDate.set(Calendar.MILLISECOND, 0);

        FlightDatabase fdb = new FlightDatabase();
        int ms = 100;

        System.out.println("Fetching info...\nDeparture Date:    Return Date:");
        for(; !startDate.after(endDate); startDate.add(Calendar.DAY_OF_MONTH, 1)){
            for( Calendar returnDate = (Calendar) startDate.clone();  !returnDate.after(endDate) &&
                (returnDate.getTimeInMillis() - startDate.getTimeInMillis() <= maxPeriodOfStayInMs); returnDate.add(Calendar.DAY_OF_MONTH, 1)){

                    String depDate = (DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.JAPAN)).format(startDate.getTime());
                    String retDate = (DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.JAPAN)).format(returnDate.getTime());
                    depDate = depDate.replace('/', '-');
                    retDate = retDate.replace('/', '-');

                    String url = prefix + depDate + "/" + retDate + suffix;
                    System.out.format("  %s         %s\n", depDate, retDate);
                    String body = getBody(url);

                    List<Flight> lf = createFlights(body, startDate.getTime(), returnDate.getTime());
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
            body = body.substring(body.indexOf('}') + 1);
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
    private static String getBody(String url) throws IOException, InterruptedException, RequestBlockedException{
        HttpClient client = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .headers("Accept", "application/json, text/javascript, */*; q=0.01",
                         "Accept-Language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3",
                         "Cookie", "_pxhd=gfOu6/RYJ94UrFXKgmuVPEc3NZxzMs4BK2nrKnFTrAtbR69sffLEh1R3C0mm5kiTDgBUrmxtFY0CQ6t0XAAqpg==:HjeZrb1oqD-kzzWFkVmT6-DDou2ToLsNv5Yo9smT3JTRmLIsrEYXh8fnIMA8PqEEAPMPri19/ysU8iTIIhplyg1IHTlb8gK5wsNs22hz/P4=; traveller_context=3f3b8373-a17f-4f23-a025-9c754ab9ea78; ssab=AAExperiment_V9:::b&BD_Flight_DV_OC_Advert_View_Hotels_V1:::a&BD_HotelDetail_RoomTypeGrouping_Desktop_V2:::b&Banana_Radar_Hook_V2:::a&DynamicNav_V5:::a&EUR_flights_dbook_coupon_flow_V2:::b&Migrate_To_New_Location_V5:::a&WPT_Footer_Flags_Version_Rollout_V47:::b&autosuggest_proxy_experiment_split_ap_northeast_1_V3:::a&autosuggest_proxy_experiment_split_ap_southeast_1_V4:::a&autosuggest_proxy_experiment_split_eu_central_1_V5:::a&autosuggest_proxy_experiment_split_eu_west_1_V4:::a&dbook_aege_trafficcontrol_webV1_V3:::a&dbook_basi_trafficcontrol_web_V2:::a&dbook_invo_trafficcontrol_web_V1:::a&dbook_norw_trafficcontrol_webV1_V3:::a&dbook_rexa_trafficcontrol_web_V2:::a&fps_mr_fqs_flights_ranking_haumea_np_V3:::c&global_inline_test_v2_V3:::k&mr_migration_proxy_test_always_on_V4:::a&rts_magpie_soow_data_collection_V8:::budgetscheduled&travel_widgets_loader_path_traffic_allocation_V6:::a; experiment_allocation_id=e3d817d99b716400254b9206bd6b2197763a8cd72c39e2f5d98a91284728f146; abgroup=11508758; ssculture=locale:::it-IT&market:::IT&currency:::EUR; __Secure-ska=dded7952-f4dc-40b0-b11c-cded5bf05af4; device_guid=dded7952-f4dc-40b0-b11c-cded5bf05af4; preferences=3f3b8373a17f4f23a0259c754ab9ea78; gdpr=information&adverts&version:::2; scanner=currency:::EUR&legs:::TSF|2022-02-09|PARI|PARI|2022-02-09|TSF&to&oym:::2202&oday:::07&wy:::0&iym:::2202&iday:::07&from:::VENI; _pxvid=83a18243-5b88-11ec-9dfd-6a4a75655554; ssaboverrides=; _csrf=Sj321ITth2uKg4_ihPBnQpF6",
                         "Referer", "https://www.skyscanner.it/trasporti/voli-da/veni/220207/220207/?adults=2&adultsv2=2&cabinclass=economy&children=0&childrenv2=&inboundaltsenabled=false&infants=0&originentityid=27547373&outboundaltsenabled=false&preferdirects=false&ref=home&rtn=1",
                         "Sec-Fetch-Dest", "empty",
                         "Sec-Fetch-Mode", "cors",
                         "Sec-Fetch-Site", "same-origin",
                         "Sec-GPC", "1",
                         "TE", "trailers",
                         "User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:96.0) Gecko/20100101 Firefox/96.0",
                         "X-Requested-With", "XMLHttpRequest")
                .GET()
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.body().contains("blocked")) throw new RequestBlockedException("The GET request has been blocked");
        else return response.body();
    }

    private static void changeIpAddress() throws IOException {
        String ipAddress= "192.168.1." + (int)Math.floor(Math.random() * 100);
        String subnetMask = "255.255.255.0";
        String defaultGateway = "192.168.1.1";
        String dns1 = "192.168.1.1";
        String[] setIp = { "netsh", "interface", "ip", "set", "address", "Wi-Fi" ,"static", ipAddress, subnetMask, defaultGateway};
        String[] setDns = { "netsh", "interface", "ip", "set", "dns", "Wi-Fi" ,"static", dns1, "primary"};;
        Process pp1 = java.lang.Runtime.getRuntime().exec(setIp);
        Process pp2 = java.lang.Runtime.getRuntime().exec(setDns);

        //System.out.println(new String(pp1.getInputStream().readAllBytes(), StandardCharsets.UTF_8));
    }
    private static void solveCaptcha() {
        try {
            Desktop desktop = Desktop.getDesktop();
            desktop.browse(new URI(captcha));
            Thread.sleep(4000);
            Robot robot = new Robot();
            robot.mouseMove(960, 650);
            robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
            robot.delay(10000);
            robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
