# SmartExpenseTracker

SmartExpenseTracker is a comprehensive Spring Boot application designed to help users manage their personal finances. It provides secure user authentication, distinct tracking for expenses and budgets, and powerful analytics to visualize spending habits.

## 🏗 Architecture

The application follows a classic Layered Architecture pattern using Spring Boot:

```mermaid
graph TD
    Client[Client (Postman/Mobile/Web)] -->|HTTP Requests| Security[Security Layer (JWT Filter)]
    Security -->|Authorized| Controller[API Layer (Controllers)]
    
    subgraph S[Application Core]
        Controller -->|DTOs| Service[Service Layer (Business Logic)]
        Service -->|Entities| Repository[Data Layer (JPA Repositories)]
    end
    
    Repository -->|SQL| DB[(H2 Database)]
    
    classDef layer bitfill:#f9f,stroke:#333,stroke-width:2px;
    class Client,DB layer
```

### Components
*   **API Layer (`com.smartexpensetracker.api`)**: REST Controllers handling HTTP requests and responses.
*   **Service Layer (`com.smartexpensetracker.service`)**: Contains business logic (calculations, validations, alerts).
*   **Data Layer (`com.smartexpensetracker.dao`)**: Interfaces extending `JpaRepository` for database interaction.
*   **Model (`com.smartexpensetracker.model`)**: JPA Entities representing database tables (`User`, `Expense`, `Budget`, `Category`).
*   **Security (`com.smartexpensetracker.config`)**: configurations for JWT authentication and BCrypt password encoding.

## 🚀 Tech Stack

*   **Java**: 17 (LTS)
*   **Framework**: Spring Boot 3.2.3
*   **Database**: H2 In-Memory Database (SQL)
*   **Security**: Spring Security + JSON Web Tokens (JWT)
*   **Build Tool**: Maven
*   **Utilities**: Lombok (boilerplate reduction), JJWT (Token handling)

## 🛠 Setup & Running

1.  **Prerequisites**:
    *   Java 17 installed (`java -version`).
    *   Maven installed (or use included `mvnw`).

2.  **Build**:
    ```bash
    mvn clean install
    ```

3.  **Run**:
    ```bash
    mvn spring-boot:run
    ```
    The application will start on `http://localhost:8080`.

4.  **Database Console**:
    *   URL: `http://localhost:8080/h2-console`
    *   JDBC URL: `jdbc:h2:mem:expensedb`
    *   User: `sa`
    *   Password: `password`

## 🔌 API Endpoints

### Authentication
*   `POST /api/v1/auth/register` - Create a new user account.
*   `POST /api/v1/auth/login` - Authenticate and receive JWT.

### Expenses
*   `GET /api/v1/expenses` - List all expenses (Login Required).
*   `POST /api/v1/expenses?categoryId={id}` - Create a new expense.

### Budgets & Categories
*   `POST /api/v1/categories` - Create a custom category.
*   `POST /api/v1/budgets` - Set a monthly budget.

### Analytics
*   `GET /api/v1/analytics/summary` - Monthly spending summary.
*   `GET /api/v1/analytics/trends` - Daily spending trends.
*   `GET /api/v1/analytics/forecast/next-month` - Predicted spending.
*   `GET /api/v1/alerts` - **[NEW]** Check if over budget.

## 🧪 Testing
Use **Postman** to import the API collection or use `curl` commands to interact with the protected endpoints. Remember to include the `Authorization: Bearer <token>` header for all non-auth endpoints.
