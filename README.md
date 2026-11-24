# AI API Testing & Contract Intelligence Agent

[![CI Pipeline](https://github.com/adarsh-chaurasia/ai-api-testing-agent/actions/workflows/ci.yml/badge.svg)](https://github.com/adarsh-chaurasia/ai-api-testing-agent/actions/workflows/ci.yml)
[![Java 21](https://img.shields.io/badge/Java-21-orange.svg)](https://www.oracle.com/java/technologies/javase/jdk21-archive-downloads.html)
[![Spring Boot 3.3.4](https://img.shields.io/badge/Spring%20Boot-3.3.4-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Spring AI](https://img.shields.io/badge/Spring%20AI-1.0.0-green.svg)](https://spring.io/projects/spring-ai)
[![React 18](https://img.shields.io/badge/React-18-blue.svg)](https://react.dev/)
[![TypeScript](https://img.shields.io/badge/TypeScript-5.5-blue.svg)](https://www.typescriptlang.org/)
[![Vite](https://img.shields.io/badge/Vite-5.4-purple.svg)](https://vitejs.dev/)
[![Docker](https://img.shields.io/badge/Docker-Enabled-2496ED.svg)](https://www.docker.com/)

An autonomous AI API testing platform that parses OpenAPI specifications, uses GenAI (Spring AI + Ollama / LLM) to synthesize structured test suites across positive, negative, and boundary scenarios, executes tests against live endpoints, performs deterministic contract validation, and provides automated failure root-cause analysis.

---

## Architecture Overview

```
                                      +-----------------------------+
                                      |    React SPA (Vite + TS)    |
                                      +--------------+--------------+
                                                     |
                                                     | REST APIs
                                                     v
                                      +-----------------------------+
                                      |   Spring Boot Backend       |
                                      +--------------+--------------+
                                                     |
         +-------------------------------------------+-------------------------------------------+
         |                                           |                                           |
         v                                           v                                           v
+-----------------------+                 +---------------------+                     +---------------------+
|   OpenAPI Parser      |                 |   Spring AI Engine  |                     |  Execution Engine   |
| (Swagger Parser v3)   |                 | (Ollama / LLMs)     |                     | (Async HTTP Pools)  |
+-----------+-----------+                 +----------+----------+                     +----------+----------+
            |                                        |                                           |
            v                                        v                                           v
 Normalized Contract DTO                 Structured Test Cases (JSON)                   Live HTTP Requests
            |                                        |                                           |
            +----------------------------------------+-------------------------------------------+
                                                     |
                                                     v
                                      +-----------------------------+
                                      | Contract Validator Engine   |
                                      | (Deterministic Validation)  |
                                      +--------------+--------------+
                                                     |
                                                     v
                                      +-----------------------------+
                                      | AI Failure Diagnostics      |
                                      | (Root-Cause Explanations)   |
                                      +--------------+--------------+
                                                     |
                                                     v
                                      +-----------------------------+
                                      | PostgreSQL / H2 Database    |
                                      +-----------------------------+
```

---

## Key Capabilities

- **Normalized OpenAPI Ingestion**: Ingests OpenAPI 3.0 / 3.1 specifications into memory-efficient normalized contract models (`ApiContractDto`) to minimize token overhead.
- **Autonomous Scenario Synthesis**: Synthesizes comprehensive positive, negative, edge, and security test cases using Ollama (e.g. `llama3.2`) or cloud LLMs via Spring AI.
- **Deterministic Contract Validation**: Enforces exact HTTP status codes, schema conformity, required property assertions, and Content-Type matching without relying on probabilistic LLM checks.
- **Concurrent Test Runner**: Non-blocking asynchronous HTTP test execution with per-endpoint timeouts, concurrency throttling, and response safeguards.
- **SSRF & Network Shield**: Enforces strict destination validation, private subnet isolation, and customizable host allowlists.
- **Automated Root-Cause Diagnosis**: Translates contract mismatches, schema drift, and unexpected status codes into clear natural language explanations with remediation guidance.
- **Interactive Demo Suite**: Built-in `/demo/users` endpoints pre-configured with realistic API defects for instant evaluation.

---

## Project Structure

```
├── .github/
│   ├── workflows/ci.yml              # GitHub Actions CI build and test pipeline
│   ├── ISSUE_TEMPLATE/               # Bug report and feature request templates
│   └── pull_request_template.md      # Standard pull request template
├── backend/                          # Spring Boot 3.3.4 (Java 21) REST Service
│   ├── src/main/java/com/apitest/agent/
│   │   ├── ai/                       # Spring AI & Ollama abstractions
│   │   ├── config/                   # Async, CORS, SSRF & Jackson configurations
│   │   ├── controller/               # Project, TestRun, and Demo REST endpoints
│   │   ├── dto/                      # Data Transfer Objects & API models
│   │   ├── exception/                # Global exception handling & custom errors
│   │   ├── executor/                 # Concurrent HTTP test execution engine
│   │   ├── model/                    # JPA persistence entities
│   │   ├── parser/                   # OpenAPI specification normalization service
│   │   ├── repository/               # Spring Data JPA repositories
│   │   ├── service/                  # Business orchestration services
│   │   ├── util/                     # JSON & HTTP helper utilities
│   │   └── validator/                # Deterministic schema & contract validator
│   ├── src/main/resources/           # Application profiles (application.yml, local, test)
│   ├── src/test/                     # Mock AI & integration test suites
│   ├── Dockerfile                    # Multi-stage production container build
│   └── pom.xml                       # Maven dependencies & build definitions
├── frontend/                         # React 18 + TypeScript + Vite SPA
│   ├── src/
│   │   ├── api/                      # Axios HTTP client & API endpoints
│   │   ├── components/               # Status badges, AI badges, viewers, layout
│   │   ├── pages/                    # Dashboard, project detail, test run detail
│   │   └── types/                    # Shared TypeScript interfaces
│   ├── Dockerfile                    # Production Nginx container build
│   └── package.json                  # Frontend dependencies & scripts
├── sample-data/                      # Example OpenAPI specifications for testing
├── docker-compose.yml                # Orchestration for PostgreSQL, Backend & Frontend
├── .env.example                      # Reference environment configuration
├── .gitignore                        # Comprehensive Git ignore rules
├── .gitattributes                    # Line ending and binary file definitions
├── .editorconfig                     # Code formatting standards across IDEs
└── README.md                         # Project documentation
```

---

## Quick Start Guide

### Prerequisites
- **JDK 21** or later
- **Node.js 20+** & **npm**
- **Ollama** (local AI engine) or API key for cloud LLM
- **Docker & Docker Compose** (optional)

---

### Option A: Running with Docker Compose (Recommended)

1. Ensure Ollama is running on your host machine:
   ```bash
   ollama serve
   ollama pull llama3.2
   ```

2. Copy environment settings:
   ```bash
   cp .env.example .env
   ```

3. Launch all containers:
   ```bash
   docker compose up --build
   ```

4. Access services:
   - **Frontend UI**: [http://localhost:3000](http://localhost:3000)
   - **Backend API**: [http://localhost:8080](http://localhost:8080)
   - **Swagger UI**: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)

---

### Option B: Running Manually for Local Development

#### 1. Start Ollama
```bash
ollama serve
ollama pull llama3.2
```

#### 2. Configure Environment
```bash
cp .env.example .env
```

#### 3. Start PostgreSQL
Run PostgreSQL on port 5432, or use the containerized database:
```bash
docker compose up postgres -d
```

#### 4. Run Backend
```bash
cd backend
mvn spring-boot:run
```
*Backend runs at `http://localhost:8080`.*

#### 5. Run Frontend
```bash
cd frontend
npm install
npm run dev
```
*Frontend runs at `http://localhost:5173`.*

---

## Running the Demo Suite

1. Open the web interface at `http://localhost:5173` (or `http://localhost:3000` under Docker).
2. Click **New Project**:
   - **Project Name**: `Demo User Service`
   - **Base URL**: `http://localhost:8080`
3. Upload the sample specification:
   - File location: `sample-data/sample-openapi.yaml`
4. Click **Generate AI Tests**:
   - The agent normalizes the specification and queries the LLM to generate positive, negative, and edge test scenarios.
5. Click **Execute Tests**:
   - The test engine dispatches HTTP requests to the target endpoints.
   - The deterministic validator validates response status codes and schemas.
   - Any defects trigger automated AI root-cause analysis with actionable fixes.

---

## Automated Testing

### Backend Unit & Parser Tests
```bash
cd backend
mvn clean test
```
*Note: Unit tests leverage `MockAiEngine` and in-memory H2 mode to execute deterministically in CI environments without live LLM or database dependencies.*

### Frontend Production Build Check
```bash
cd frontend
npm run build
```

---

## Configuration Reference

| Environment Variable | Default Value | Description |
|----------------------|---------------|-------------|
| `DATABASE_URL` | `jdbc:postgresql://localhost:5432/api_testing` | JDBC database connection URL |
| `DATABASE_USERNAME` | `apitest` | Database username |
| `DATABASE_PASSWORD` | `apitest` | Database password |
| `OLLAMA_BASE_URL` | `http://localhost:11434` | Ollama service endpoint |
| `OLLAMA_MODEL` | `llama3.2` | Target LLM model |
| `APP_TESTING_DEMO_MODE` | `false` | Enable strict host execution restrictions |
| `APP_TESTING_ALLOWED_HOSTS` | ` ` | Comma-separated allowlist of target hostnames |
| `APP_DEMO_DEFECTS_ENABLED` | `true` | Toggle intentional defect simulation in demo API |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:5173,http://localhost:3000` | Allowed origins for web dashboard |

---

## CI/CD Pipeline

The project includes automated GitHub Actions workflows in `.github/workflows/ci.yml`:
- **Backend Job**: Checks out code, sets up Java 21, caches Maven dependencies, runs `mvn clean test` under the test profile.
- **Frontend Job**: Sets up Node.js 20, caches npm packages, runs type checks (`tsc --noEmit`), and builds production assets (`vite build`).

---

## License

This project is licensed under the terms described in the [LICENSE](LICENSE) file.
