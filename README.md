# College ERP System

A College ERP application built with Java 21, Spring Boot, Thymeleaf, Spring Data JPA, and MySQL.

## Technology Stack
- Java 21
- Spring Boot
- Spring MVC and Thymeleaf
- Spring Data JPA / Hibernate
- MySQL 8
- Maven

## Requirements
- JDK 21
- MySQL 8.x

## Local Setup (Windows PowerShell)
1. Create the database:
   ```sql
   CREATE DATABASE collegeerp;
   ```
2. Copy `src/main/resources/application.properties.example` to
   `src/main/resources/application.properties`.
3. Set your local database and Razorpay test credentials in the local
   `application.properties` file. Do not commit that file.
4. Build and run:
   ```powershell
   .\mvnw.cmd clean package
   .\mvnw.cmd spring-boot:run
   ```
5. Open http://localhost:8081

## Security
- Real `application.properties` is excluded from Git.
- Do not commit passwords, Razorpay secrets, API tokens, SQL dumps,
  real student records, or local backup files.
- Use Razorpay Test Mode credentials only while testing.

## Database
This repository does not include a database dump. Create/configure your
local MySQL database and required tables before running the application.
