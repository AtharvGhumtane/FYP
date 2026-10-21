# 🎓 AI-Powered Smart Examination & Learning Analytics Platform

An enterprise-grade, distributed smart examination and learning analytics platform powered by **Spring Boot microservices**, **Spring Cloud API Gateway**, and **Google Gemini AI**. Includes a unified **React + Vite** frontend with role-based portals and an interactive **API Explorer / E2E Suite Runner**.

---

## 📁 Repository Structure

```
.
├── backend/                  # All Spring Boot Microservices & Infrastructure
│   ├── api-gateway/          # Spring Cloud Gateway (:8080) - JWT filtering & routing
│   ├── auth-service/         # Authentication & Teacher Key Registry (:8081)
│   ├── classroom-service/    # Classroom lifecycle & Roster management (:8082)
│   ├── assessment-service/   # Exam authoring, autosave & auto-grading (:8083)
│   ├── notification-service/ # Real-time polling & user alerts (:8084)
│   ├── analytics-service/    # DB visual metrics & Google Gemini AI (:8085)
│   ├── common-dto/           # Shared DTO transfer models
│   ├── common-security/      # Shared JWT & security utilities
│   ├── http-tests/           # Complete HTTP verification test suites
│   ├── init-scripts/         # PostgreSQL schema initializers
│   ├── docker-compose.yml    # Multi-service container orchestration
│   ├── pom.xml               # Root Maven parent reactor
│   └── .env.example          # Sample environment variables
│
└── frontend/                 # React + Vite Modern Web UI & API Lab
    ├── src/
    │   ├── components/       # Platform portals, API Explorer & E2E runner
    │   ├── context/          # Session, persona & toast context
    │   └── services/         # API client & full endpoint registry
    ├── package.json
    └── vite.config.js
```

---

## 🛠️ Microservices Architecture

| Microservice | Port | Database | Primary Responsibilities |
| :--- | :---: | :---: | :--- |
| **API Gateway** | `8080` | — | Central entry point, CORS, JWT routing filter, Actuator |
| **Auth Service** | `8081` | `auth_db` | User registration (Student / Teacher), Login, Admin key registry, Google OAuth |
| **Classroom Service** | `8082` | `classroom_db` | Course creation, enrollment join requests, approvals, rosters |
| **Assessment Service** | `8083` | `assessment_db` | MCQ test builder, real-time answer autosave, auto-grading, proctoring telemetry |
| **Notification Service** | `8084` | `notification_db` | Event notifications for scheduled tests and graded evaluations |
| **Analytics & AI** | `8085` | `analytics_db` | Cohort distributions, topic mastery, and Google Gemini AI cognitive diagnosis |

---

## 🚀 Getting Started

### 1. Prerequisites
- **Java 17+**
- **Maven 3.8+**
- **Node.js 18+** & **npm**
- **Docker & Docker Compose** (or local PostgreSQL)

### 2. Environment Configuration
Navigate to `backend/` and set up your environment variables:
```bash
cd backend
cp .env.example .env
```

### 3. Launching the Backend

#### Option A: Run via Docker Compose (Recommended)
```bash
cd backend
docker compose up -d
```

#### Option B: Run locally via Maven
```bash
cd backend
./mvnw clean install
./mvnw spring-boot:run -pl api-gateway
# Start other services in parallel or via start-services.ps1
```

### 4. Launching the Frontend
```bash
cd frontend
npm install
npm run dev
```
Open **`http://localhost:5173/`** to access the web application.

---

## 💻 Frontend Features & Capabilities

- **Instant Persona Testing**: 1-click authentication as Seeded Admin, Teacher (`Dr. Smith`), or Student (`Grace Hopper`).
- **Interactive Exam Cockpit**: Real-time timer countdown, question palette, option selector, instant autosave (`PUT /api/tests/{testId}/answers`), and anti-cheat tab-switch detection.
- **Graded Scorecards**: Question-level grading breakdown showing candidate selection vs. correct answer, marks awarded, and instructor explanations.
- **Google Gemini AI Diagnostics**: Automated cognitive analysis of student learning pitfalls and class-wide instructional recommendations.
- **Interactive API Explorer**: Test all endpoints across all microservices directly from the browser with syntax highlighting, parameter inputs, preset JSON payloads, and response latency tracking.
- **Automated E2E Suite Runner**: Executes the complete 18-step multi-service lifecycle test in sequence with real-time logs.

---

## 🔒 Security
- Stateless JWT-based authentication.
- BCrypt hashed teacher authorization registry (Admin generated).
- Internal S2S token authentication between microservices (`X-Internal-Token`).
- Sensitive `.env` files are strictly excluded from source control.
