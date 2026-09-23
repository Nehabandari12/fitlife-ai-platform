#fitlife — AI-Driven Fitness Tracking Platform

An event-driven, microservices-based fitness platform that delivers **personalized AI workout recommendations** in real time using Google Gemini, Apache Kafka, and Spring Cloud.

---

## 🏗️ Architecture Overview

FitLife Connect is built as a distributed system with 6 independently deployable microservices, secured by OAuth2 PKCE and orchestrated via an API Gateway.

```
React Frontend (Vite + Redux)
        │
        ▼
  API Gateway (Spring Cloud)
        │
   ┌────┴─────┬──────────────┐
   ▼          ▼              ▼
User       Activity       AI Service
Service    Service     (Gemini Engine)
               │              ▲
               └──── Kafka ───┘
```

| Service | Responsibility |
|---|---|
| `gateway` | Routing, CORS, JWT validation, Keycloak integration |
| `userservice` | User registration, profile management, Keycloak sync |
| `activityservice` | Activity CRUD, Kafka producer |
| `aiservice` | Kafka consumer, Gemini API, recommendation generation |
| `eureka` | Service discovery |
| `configserver` | Centralized configuration |

---

## ✨ Key Features

- **AI Recommendation Engine** — Integrates Google Gemini to generate structured, personalized fitness recommendations (analysis, improvements, suggestions, safety guidance) triggered automatically on every activity event
- **Event-Driven Pipeline** — Activity Service publishes create/update/delete events to Kafka; AI Service consumes them asynchronously, keeping user-facing latency under 200ms
- **OAuth2 PKCE Security** — Keycloak-backed authentication with Spring Cloud Gateway enforcing token validation and automatic user sync across services
- **Real-Time Recommendations** — Per-activity (`/recommendations/activity/{id}`) and aggregated user-level insights (`/recommendations/user/{userId}`)
- **Full React Frontend** — Vite + Redux Toolkit SPA with protected routes, activity CRUD, recommendation display, and analytics dashboard
- **Prompt Engineering** — Structured Gemini prompts producing consistent JSON output across four sections: `analysis`, `improvements[]`, `suggestions[]`, `safety[]`

---

## 🛠️ Tech Stack

| Layer | Technology |
|---|---|
| Backend | Java, Spring Boot 3, Spring Cloud (Gateway, Eureka, Config Server) |
| Security | Keycloak, OAuth2 PKCE, JWT, Spring Security |
| Messaging | Apache Kafka (event-driven async pipeline) |
| AI | Google Gemini API |
| Database | PostgreSQL, MongoDB |
| Frontend | React (Vite), Redux Toolkit, Tailwind CSS |
| Testing | JUnit, Postman, Kafka CLI, Spring Boot Test |
| DevOps | Docker, Docker Compose |

---

## 🤖 AI Service — How It Works

```
User logs activity
      │
      ▼
Activity Service → Kafka Topic (activity.created / updated / deleted)
      │
      ▼
AI Service (Kafka Consumer)
      │
      ▼
Gemini API (structured prompt with activity metadata + trend context)
      │
      ▼
Recommendation stored → available via REST API
```

The AI service was tuned through multiple prompt iterations to eliminate inconsistent JSON, add safety guidance, and incorporate long-term trend context — resulting in reliable, structured output every time.

---

## 🔐 Security Architecture

- **OAuth2 PKCE Flow** — Prevents authorization code interception in the browser-based SPA
- **Spring Cloud Gateway** — Central enforcement point for JWT validation before any request reaches a downstream service
- **KeycloakUserSyncFilter** — Automatically syncs authenticated Keycloak users into the User Service on first login
- **Protected Routes** — React frontend enforces authentication on all activity and recommendation screens

---

## 🚀 Running Locally

### Prerequisites
- Java 17+
- Node.js 18+
- Docker & Docker Compose
- Keycloak instance

### Start Infrastructure
```bash
docker-compose up -d   # starts Kafka, PostgreSQL, MongoDB, Keycloak
```

### Start Services (in order)
```bash
# 1. Config Server
cd configserver && mvn spring-boot:run

# 2. Eureka
cd eureka && mvn spring-boot:run

# 3. Core services (any order)
cd userservice && mvn spring-boot:run
cd activityservice && mvn spring-boot:run
cd aiservice && mvn spring-boot:run

# 4. Gateway
cd gateway && mvn spring-boot:run

# 5. Frontend
cd fitness-frontend && npm install && npm run dev
```

---

## 📡 Key API Endpoints

| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/activities` | Log a new activity (triggers AI recommendation) |
| GET | `/api/activities/{id}` | Get activity details |
| GET | `/recommendations/activity/{id}` | Get AI recommendation for a specific activity |
| GET | `/recommendations/user/{userId}` | Get aggregated AI insights across all user activities |



