# Pangreksa HRIS – pangreksa-web

**Human Resource Information System (HRIS)**  
**Developed and maintained by PT Fusi Solusi Transformasi**

---

## 1. Overview

Pangreksa HRIS is a web-based Human Resource Information System built to help organizations manage their workforce more effectively. This system is designed as a centralized platform for handling various HR processes such as employee data management, attendance tracking, payroll, and leave management.

Our goal is to support companies in simplifying administrative tasks, improving employee experience, and enabling data-driven decision making in human capital management.

### 1.1 Business Needs Addressed

- **Manual HR processes** are time-consuming and prone to error. Pangreksa digitalizes these workflows to ensure speed, accuracy, and accountability.
- **Employee records** are often scattered across multiple files or systems. Pangreksa provides a centralized and secure location for all employee information.
- **Attendance management** helps companies track productivity and compliance across multiple locations or branches.
- **Payroll processing** is simplified with automated calculations, deduction rules, and salary slip generation.
- **Leave requests and approvals** are streamlined with a transparent, trackable workflow.
- **Multi-branch support** enables the system to manage and separate data from multiple company branches within a single, centralized platform.

### 1.2 Who Is It For?

- Small to medium enterprises looking to digitize and centralize their HR operations
- Organizations with multiple branches or locations
- HR teams aiming to reduce administrative workload and improve service quality
- Decision makers seeking real-time data for strategic HR planning

### 1.3 Key Modules

- **Employee Information Management**  
  Store, edit, and search employee records with ease.

- **Attendance Monitoring**  
  Capture employee attendance in real-time.

- **Leave Management**  
  Submit, approve, and track leave applications with automated validation and balance calculation.

- **Payroll Module**  
  Generate monthly salaries, handle tax and insurance deductions, and distribute payslips.

- **Organization & Role Management**  
  Manage branches, departments, positions, and role-based access control.

---

## 2. Tech Stack

- **Backend**
  - Java 21
  - Spring Boot 3 (REST, security, validation, actuator)
  - Spring Data JPA
  - Vaadin 24 (Flow + Vaadin React components)
  - PostgreSQL driver
  - Lombok

- **Frontend**
  - Vaadin React Components
  - React 18
  - React Router 7
  - TypeScript
  - Vite (build & dev server)

- **Database**
  - PostgreSQL (primary database)
  - Testcontainers PostgreSQL for integration tests

- **Build & Tooling**
  - Maven (primary build tool)
  - Spotless + Eclipse formatter (Java formatting)
  - Spotless + Prettier (TypeScript/React formatting)
  - JUnit 5, Spring Boot Test, Spring Security Test
  - Testcontainers, ArchUnit

---

## 3. Architecture & Project Structure

The project follows a typical Spring Boot multi-module style with Vaadin and a React-based frontend.

```text
pangreksa-web/
├─ pom.xml                 # Maven configuration (Spring Boot, Vaadin, plugins)
├─ package.json            # Node/Vite/Vaadin React dependencies
├─ vite.config.ts          # Vite configuration (overridden by Vaadin)
├─ tsconfig.json           # TypeScript configuration
├─ src/
│  ├─ main/
│  │  ├─ java/             # Spring Boot application, domain, services, security, etc.
│  │  ├─ resources/        # application properties, messages, static resources
│  │  └─ frontend/         # Vaadin + React UI code
│  └─ test/
│     └─ java/             # Unit & integration tests (JUnit, Testcontainers, ArchUnit)
└─ Dockerfile              # Container image definition
```

Typical backend layering (may evolve as the project grows):

- **API/UI layer**: Vaadin views and routes, controllers/endpoints
- **Application/Service layer**: orchestration of use cases, business logic
- **Domain layer**: entity models, value objects, domain rules
- **Persistence layer**: Spring Data JPA repositories, database mappings

---

## 4. Getting Started

### 4.1 Prerequisites

- Java **21**
- Node.js **LTS** (for Vaadin/Vite frontend tooling)
- Maven **3.9+**
- PostgreSQL database (local or container)

Optional but recommended:

- Docker (for running PostgreSQL and/or the application in a container)

### 4.2 Clone the Repository

```bash
git clone https://github.com/fusi24/pangreksa-web.git
cd pangreksa-web
```

---

## 5. Configuration

Application configuration is located under `src/main/resources` (for example `application.yml` or `application.properties`).

At a minimum, configure:

- **Database connection** (PostgreSQL)
  - URL, username, password
  - Schema/database name

- **Spring profiles**
  - `dev` – local development
  - `prod` – production deployment (tuned for performance and security)

Use environment variables or external configuration in production to avoid committing secrets.

---

## 6. Running the Application

### 6.1 Run in Development Mode

This project uses Vaadin with Spring Boot. Vaadin handles the frontend build with Vite internally.

Start the backend (and integrated frontend) using Maven:

```bash
mvn spring-boot:run
```

By default, the application will start on `http://localhost:8080` (unless configured otherwise).

### 6.2 Running with Docker (optional)

A `Dockerfile` is provided so you can build and run the application in a container:

```bash
# Build jar first
mvn clean package -DskipTests

# Build Docker image
docker build -t pangreksa-web .

# Run container (configure DB connection via environment variables)
docker run --rm -p 8080:8080 \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://host.docker.internal:5432/pangreksa \
  -e SPRING_DATASOURCE_USERNAME=your_user \
  -e SPRING_DATASOURCE_PASSWORD=your_password \
  pangreksa-web
```

Adjust environment variables and ports according to your environment.

### 6.3 Production Build

Use the `production` Maven profile to build optimized frontend assets and a production-ready JAR:

```bash
mvn -Pproduction clean package
```

This will:

- Run the Vaadin production build
- Produce an executable JAR under `target/`

---

## 7. Testing

The project includes dependencies for unit, integration, and architecture tests.

### 7.1 Run All Tests

```bash
mvn test
```

### 7.2 Integration Tests with Testcontainers

Integration tests are configured to use **Testcontainers** with PostgreSQL. When you run the integration test phase, Docker must be available:

```bash
mvn -Pintegration-test verify
```

This will:

- Start a PostgreSQL container
- Run integration tests against it

---

## 8. Contribution Guidelines

Pangreksa HRIS is provided **free of charge** for IT professionals, developers, and technology enthusiasts who are passionate about exploring and building better HR solutions.

We welcome issues, suggestions, and pull requests. When contributing:

- **Follow the existing coding style**
  - Java formatting is enforced via Spotless + `eclipse-formatter.xml`.
  - TypeScript/React formatting is enforced via Prettier.
- **Add or update tests** where applicable for new features or bug fixes.
- **Document user-facing changes** in the README or UI.

Basic contribution workflow:

1. Fork the repository.
2. Create a feature branch from `main`.
3. Implement your changes.
4. Run tests and ensure formatting passes.
5. Submit a pull request with a clear description of the change.

---

## 9. About PT Fusi Solusi Transformasi

PT Fusi Solusi Transformasi was founded in 2013 by a group of seasoned IT professionals with a shared vision to bring greater value to the information technology industry—particularly in the field of software development.

With a strong passion for innovation and excellence, we are committed to delivering the best solutions to our clients, helping them achieve operational efficiency and digital transformation through reliable, tailor-made software products.

Visit us at: [https://fusi24.com](https://fusi24.com)

---

## 10. Contact & Support

The Pangreksa HRIS product is proudly supported and maintained by **PT Fusi Solusi Transformasi** as part of our commitment to the IT community and open innovation.

We believe in collaboration, learning, and sharing—feel free to use, modify, or contribute to this project.

For partnership opportunities, product demo, or inquiries, please reach out to:

- 📧 Email: info@fusi24.com  
- 🌐 Website: [https://fusi24.com](https://fusi24.com)  
- 📍 Jakarta, Indonesia

---

_© 2025 PT Fusi Solusi Transformasi — Empowering organizations through smart HR solutions._
