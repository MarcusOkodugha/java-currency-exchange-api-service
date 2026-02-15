package org.example;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.io.IOException;
import org.h2.tools.Server;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class ExchangeAPI {


    private static final String SEK_TO_EUR = "https://api.riksbank.se/swea/v1/CrossRates/SEKETT/SEKEURPMI/%s";
    private static final String SEK_TO_USD = "https://api.riksbank.se/swea/v1/CrossRates/SEKETT/SEKUSDPMI/%s";


    HttpClient client = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();
    private record  RateResponse(String date, double value){}

    static double sekToEur;
    static double eurToSek;
    static double sekToUsd;
    static double usdToSek;
    static double eurToUsd;
    static double usdToEur;

    String date;
    public void genRates() throws IOException, InterruptedException {
//        String today = LocalDate.now()
//                .format(DateTimeFormatter.ISO_LOCAL_DATE);


        this.date = LocalDate.now()
                .minusDays(4)
                .format(DateTimeFormatter.ISO_LOCAL_DATE);

        sekToEur = getRate(String.format(SEK_TO_EUR, this.date));
        sekToUsd = getRate(String.format(SEK_TO_USD, this.date));

        eurToSek = 1.0 / sekToEur;
        usdToSek = 1.0 / sekToUsd;
        eurToUsd = sekToUsd / sekToEur;
        usdToEur = sekToEur / sekToUsd;

        System.out.println("SEK_TO_EUR " + sekToEur);
        System.out.println("EUR_TO_SEK " + eurToSek);
        System.out.println("SEK_TO_USD " + sekToUsd);
        System.out.println("USD_TO_SEK " + usdToSek);
        System.out.println("EUR_TO_USD " + eurToUsd);
        System.out.println("USD_TO_EUR " + usdToEur);
    }

    public double getRate(String URL) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(URL)).GET().build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 204 || response.statusCode() == 404) {
            String yesterday = LocalDate.now()
                    .minusDays(1)
                    .format(DateTimeFormatter.ISO_LOCAL_DATE);

            String fallbackUrl = URL.substring(0, URL.lastIndexOf("/") + 1) + yesterday;
            return getRate(fallbackUrl);
        }

        if (response.statusCode() != 200) {
            System.out.println("Status: " + response.statusCode());
            System.out.println("Body: " + response.body());
            throw new RuntimeException("API error unknown");
        }

        java.util.List<RateResponse> rates =
                mapper.readValue(response.body(), new TypeReference<>() {});

        if (rates.isEmpty()) {
            throw new RuntimeException("No rate data returned");
        }

        return rates.get(0).value();
    }

    public void startServer(RateRepository repo) throws IOException {

        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);

        // POST /fetch
        server.createContext("/fetch", exchange -> {
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "http://localhost:3000");
            if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
                exchange.sendResponseHeaders(405, -1);
                return;
            }

            try {
                genRates();
                repo.deleteAll();
                LocalDate parsedDate = LocalDate.parse(this.date, DateTimeFormatter.ISO_LOCAL_DATE);
                repo.save(new Rate(parsedDate, "SEK_TO_EUR", sekToEur));
                repo.save(new Rate(parsedDate, "EUR_TO_SEK", eurToSek));
                repo.save(new Rate(parsedDate, "SEK_TO_USD", sekToUsd));
                repo.save(new Rate(parsedDate, "USD_TO_SEK", usdToSek));
                repo.save(new Rate(parsedDate, "EUR_TO_USD", eurToUsd));
                repo.save(new Rate(parsedDate, "USD_TO_EUR", usdToEur));

                exchange.sendResponseHeaders(200, -1);
                exchange.getResponseBody().close();

            } catch (Exception e) {
                exchange.sendResponseHeaders(500, -1);
                exchange.getResponseBody().close();
            }
        });

        // GET /exchange?from=SEK&to=EUR&amount=100
        server.createContext("/exchange", exchange -> {
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "http://localhost:3000");
            if (!exchange.getRequestMethod().equalsIgnoreCase("GET")) {
                exchange.sendResponseHeaders(405, -1);
                return;
            }

            try {
                String query = exchange.getRequestURI().getQuery();

                String from = null;
                String to = null;
                double amount = 0;

                for (String param : query.split("&")) {
                    String[] kv = param.split("=");
                    if (kv[0].equals("from")) from = kv[1];
                    if (kv[0].equals("to")) to = kv[1];
                    if (kv[0].equals("amount")) amount = Double.parseDouble(kv[1]);
                }

                String pair = from + "_TO_" + to;
                Double rate = repo.findLatestRate(pair);

                if (rate == null) {
                    exchange.sendResponseHeaders(404, -1);
                    return;
                }

                double result = amount * rate;
                String response = String.format(Locale.US, "%.2f", result);
                System.out.println("response = " + response);

                exchange.sendResponseHeaders(200, response.length());
                exchange.getResponseBody().write(response.getBytes());
                exchange.getResponseBody().close();

            } catch (Exception e) {
                exchange.sendResponseHeaders(400, -1);
            }
        });
        server.start();
        System.out.println("Running on http://localhost:8080");
    }

    static void main(String[] args) throws IOException, InterruptedException, SQLException {

        Server.createWebServer("-webPort", "8082").start(); //start db
        ExchangeAPI api = new ExchangeAPI();

//        api.genRates();

        RateRepository repo = new RateRepository();
        api.startServer(repo); // star server
    }


}
