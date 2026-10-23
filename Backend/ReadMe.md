# Cafeteria-Ordering-System

> ITMDA3-34 Group S19

This is a mobile pre-ordering food application made up of two PWA apps: one running the Staff and Admin Dashboard
for managing orders, items, and users, and another running the mobile user app. All of this is powered by a robust 
Spring Boot backend handling REST endpoints, business logic, security, and real-time WebSockets.

---

## About

This app is designed to reduce queue times in the campus cafeteria at the Eduvos Tygervalley campus. 
By allowing students to pre-order food while still in class, it cuts down the extra time spent waiting
and deciding at the counter.

---

## Tech Stack
* **Language & Framework:** Java 21, Spring Boot
* **Security:** Spring Security with JJWT (v0.12.6) for stateless JWT token creation and validation
* **Persistence:** Spring Data JPA, Hibernate, & MySQL (with H2 configured for automated testing)
* **API & Validation:** Spring Web (REST controllers) & Spring Boot Validation (for rigorous DTO payload checks)
* **Real-Time / Extras:** Spring WebSockets (STOMP messaging)
* **Media Management:** Cloudinary SDK (v1.38.0) for cloud image uploads/deletions
* **Developer Productivity:** Project Lombok
* **Testing:** Spring Boot Test, Spring Security Test, and WebMVC Test
* **Build Tool:** Maven
* **Containerization:** Docker & Docker Compose

---

## Prerequisites
Before you begin, ensure you have the following installed on your local machine:
* [Java JDK 21](https://adoptium.net/)
* [Maven](https://maven.apache.org/)
* [MySQL Server](https://dev.mysql.com/) (or run via Docker Compose)
* [Docker & Docker Compose](https://www.docker.com/) (Recommended)

---

## Installation & Setup

1. **Clone the repository:**
git clone <your-new-repository-url>
cd Cafeteria_Ordering_Backend

2. **Configure Environment Variables:**
This will create a new copy of .env.exampe named .env
cp .env.example .env 

You must then Complete this newly made .env file        

3. **Build and Run via Maven:**
mvn clean install
mvn spring-boot:run
4. **Front end:**
You have different options, I've Just been Running them via VSCode by:

Opening 2 terminals
Changing directory so that each points to the corresponding frontend app Client or Staff: cd <insert path here>
running: npm run dev -- --host 

Then opening the first local link if you want it open on your browser, otherwise the Network link will allow you to
open it on any other device in the same network, assuming network rules allow it.

