# Flash Sale Microservice

Hey! This is a Spring Boot backend I built to simulate a high-traffic flash sale (like when iPhones drop and everyone tries to buy them at once). 

The main goal here was to handle high concurrency without crashing the database or accidentally selling more items than we actually have in stock.

## What's inside?
* **Spring Boot (Java 21)**: The core framework.
* **H2 Database**: Used for local testing so you don't need to set up MySQL just to run it.
* **Caching**: I used Spring Cache to store product details in memory. This means we don't hit the database every time someone just wants to look at the product page.
* **Apache Kafka**: Used to handle orders. Instead of making the user wait for the order to process, the app drops a message into Kafka and immediately tells the user "Order placed!".

## How it handles the "Flash Sale" problem
If 1,000 people click "Buy" at the exact same second, a normal database might oversell. 
To fix this, I didn't do the usual "check stock in Java, then update". Instead, I wrote a custom SQL query in `ProductRepository` that does the check and the update in a single atomic step. If the stock is 0, the database rejects the update automatically. 

I also added `@CacheEvict` so that the second an item is bought, the old cached stock is deleted and the next user sees the real-time number.

## How to run it

### 1. Start Kafka (Docker)
You'll need Docker installed. Since Kafka is required for the order processing, I included a simple `docker-compose.yml` file in the project.
Just open your terminal in the project folder and run:
`docker-compose up -d`

### 2. Run the App
Open the project in Eclipse (or IntelliJ), wait for Maven to finish downloading, and just run `FlashSale1Application.java`. 
The app automatically creates the database and seeds it with 10 iPhones and 50 AirPods so you have something to test with.

## How to test it
Once it's running on `localhost:8080`, you can test it in Postman or your browser:

1. **Look at a product:**
   `GET http://localhost:8080/api/product/1`
   *(Try refreshing it a few times. The first time it hits the database, after that it's served from the cache).*

2. **Buy a product:**
   `POST http://localhost:8080/api/order?productId=1&qty=1`
   *(If you watch the Eclipse console, you'll see the Kafka listener pick up the message in the background).*

3. **Try to oversell:**
   Send that POST request 11 times. The 11th time will fail with "Out of stock" because we only seeded 10 items.
