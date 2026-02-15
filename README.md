# java-currency-exchange-api-service

Minimal Java REST API for currency exchange (SEK, EUR, USD).  
Rates are fetched from Riksbank and stored in an H2 database.  
The exchange endpoint always uses the latest stored rate.

## Requirements
- Java 17
- Maven

## Build and Run

From the project root:

mvn clean package  
mvn exec:java -Dexec.mainClass="org.example.ExchangeAPI"

Server starts on:
http://localhost:8080

H2 console runs on:
http://localhost:8082

## Endpoints

POST /fetch  
Fetches latest rates from Riksbank and stores them in the database.

Example:
POST http://localhost:8080/fetch

Returns:
- 200 on success
- 500 on failure

GET /exchange  
Converts an amount using the latest stored rate.

Example:
GET http://localhost:8080/exchange?from=EUR&to=SEK&amount=100

Returns:
- 200 and a number with 2 decimals, for example 1056.70
- 404 if the currency pair is not found
- 400 for invalid query parameters

Supported currencies:
- SEK
- EUR
- USD

## Database

H2 database files are created locally in the project directory:

- ratesdb.mv.db
- ratesdb.trace.db

H2 console login:
JDBC URL: jdbc:h2:./ratesdb  
User: sa  
Password: (empty)

Table structure:
rates(date DATE, currency_pair VARCHAR(30), exchange_rate DOUBLE)

## Notes

- /fetch is the only endpoint that calls the external Riksbank API.
- Avoid repeated calls to prevent rate limiting.
- The service stores these currency pairs:
  SEK_TO_EUR  
  EUR_TO_SEK  
  SEK_TO_USD  
  USD_TO_SEK  
  EUR_TO_USD  
  USD_TO_EUR  

## .gitignore

Add the following to .gitignore:

ratesdb.mv.db  
ratesdb.trace.db
