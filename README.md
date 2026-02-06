# RiskDeV - Software Supply Chain Risk Assessment System

**RiskDeV** is a platform designed to analyze the security of the Python package ecosystem. It collects package metadata and maps dependencies to highlight both direct and indirect security risks.

The system focuses on "Software Supply Chain" security, identifying vulnerabilities (CVEs) that might be hidden deep within a project's dependency tree. To manage the complex data relationships and ensure high performance, the architecture implements **Polyglot Persistence**, utilizing both **MongoDB** (document store) and **Neo4j** (graph database).

## Repository Structure

The repository is organized into two main directories:

* **`dataset/`**: Contains the Python scripts required for dataset generation, data fetching from PyPI/NVD, and data normalization.
* **`riskDeV/`**: Contains the Java Spring Boot backend application source code.

## Tech Stack & Frameworks

The project is built using the following technologies:

### Backend
* **Java 21**
* **Spring Boot**: Main application framework.
* **Spring Security**: Handling authentication and authorization (JWT).
* **Spring Data**: For data access layers (MongoDB and Neo4j).
* **Maven**: Build and dependency management.
* **Lombok**: Utility library to reduce boilerplate code.

### Databases
* **MongoDB**: Stores documents for Packages, Vulnerabilities, Users, and Projects.
* **Neo4j**: Manages the graph structure of package dependencies.

### Tools
* **Python**: Used for data collection and dataset generation.
* **Swagger/OpenAPI**: For API documentation and testing.

## Installation and Deployment

This repository maintains two distinct branches to support different deployment environments. Please select the appropriate branch for your setup.

### Local Environment (Branch: `main`)
*Target: Single machine development and testing.*

This branch is configured to run all services on `localhost`.

**Prerequisites:**
* Java 21
* Maven
* MongoDB (running on port `27017`)
* Neo4j (running on port `7687`)

**How to run:**
1. Clone the repo and checkout the `local` branch.
2. Navigate to the `riskDeV/` folder.
3. Run the application:
   ```bash
   mvn spring-boot:run
