# GEMINI Project Context

This document provides a comprehensive overview of the "Azure DevOps Git Uploader" project to be used as instructional context for future interactions with the Gemini AI agent.

## Project Overview

This is a full-stack application designed to simplify Git operations for Azure DevOps repositories. It consists of a Java/Spring Boot backend that provides a REST API, and an Angular frontend that provides a web-based user interface.

The primary purpose of the application is to automate the process of cloning, committing, and pushing changes to specific folders within Azure DevOps repositories.

### Main Technologies

*   **Backend:**
    *   Java 21
    *   Spring Boot 3
    *   Maven
*   **Frontend:**
    *   Angular 20
    *   Angular Material
    *   TypeScript
    *   Node.js (for development)
*   **Other:**
    *   Git

### Architecture

The project is divided into two main parts:

*   **`backend/`:** A Spring Boot application that exposes a REST API for managing PATs, repositories, and Git operations.
*   **`frontend/`:** An Angular single-page application (SPA) that consumes the backend API and provides the user interface.

The two parts are developed independently but are packaged together for production deployment. The `build.sh` script handles the entire build process, including building the frontend and packaging it into the backend's jar file.

## Building and Running

### Prerequisites

*   Java 21+
*   Node.js 20+
*   Git 2.40+

### Unified Build (Production)

The project provides a unified build script that packages the frontend and backend together into a single executable jar file.

```bash
./build.sh
```

This command will:
1.  Install the frontend dependencies (`npm install`).
2.  Build the frontend for production (`npm run build`).
3.  Package the backend and the built frontend into a single jar file using Maven.

The final artifacts will be located in the `build/` directory.

To run the application:

```bash
cd build
java -jar git-uploader.jar
```

The application will be available at `http://localhost:8080`.

### Development Mode

**Backend:**

To run the backend in development mode:

```bash
cd backend
./mvnw spring-boot:run
```

The backend API will be available at `http://localhost:8080`.

**Frontend:**

To run the frontend in development mode:

```bash
cd frontend
npm install
npm start
```

The frontend will be available at `http://localhost:4200` and will be proxied to the backend API at `http://localhost:8080`.

### Testing

**Backend:**

```bash
cd backend
./mvnw test
```

**Frontend:**

```bash
cd frontend
npm run test
```

## Development Conventions

*   **Backend:**
    *   The backend follows the standard Spring Boot project structure.
    *   The code is written in Java.
    *   Error handling is done through custom exception classes (`InvalidRequestException`, `RepoNotFoundException`).
*   **Frontend:**
    *   The frontend is built with Angular and uses TypeScript.
    *   The UI is built with Angular Material components.
    *   The application is a single-page application (SPA) with a single component (`App`).
    *   State is managed with Angular signals.
*   **Git:**
    *   The project uses Git for version control.
    *   The `main` branch is the primary development branch.
    *   Commits should follow the conventional commit format (e.g., `feat:`, `fix:`, `docs:`).
