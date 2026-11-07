# College ERP System

A Java 21 / Spring Boot application using Thymeleaf, Spring Data JPA, and MySQL.

## Local run

1. Create a MySQL database named `collegeerp`.
2. Copy `src/main/resources/application.properties.example` to `src/main/resources/application.properties`.
3. Set local database credentials and Razorpay **test-mode** credentials in the local properties file.
4. Run `./mvnw spring-boot:run` (Windows PowerShell: `./mvnw.cmd spring-boot:run`).
5. Open `http://localhost:8081`.

## Railway deployment configuration

Set these Railway service variables:
- `SPRING_DATASOURCE_URL` — JDBC URL for the Railway MySQL service
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`
- `RAZORPAY_KEY_ID` — Razorpay test key ID
- `RAZORPAY_KEY_SECRET` — Razorpay test secret

Railway provides `PORT`; the application config uses it automatically.

## Database safety

The uploaded local ZIP contained database backups with student/user records. Those backups and all SQL exports are intentionally excluded from this deployment bundle. The Railway database must be initialized with a reviewed schema before the application can serve real data. No local student records or passwords are copied by this bundle.

## Security

Do not commit `application.properties`, `.env`, SQL dumps, database passwords, Razorpay secrets, or real student records.
