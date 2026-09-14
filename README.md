# Banking Microservices Application

This project is a Java/Spring Boot microservices-based banking demo that simulates account management, money transfers, fraud checks, payment ordering, and notification flows across multiple independent services.

## Overview

The application is composed of the following services:

- Account Service: creates and manages customer accounts and balances
- Transaction Service: handles money transfers and OTP-based verification flow
- Fraud Detection Service: checks suspicious transactions using a Kafka event-driven workflow
- Notification Service: logs and surfaces alerts for OTP, fraud, refund, and payment lifecycle events
- Payment Service: creates Razorpay orders and handles payment webhooks
- API Gateway: exposes a single entry point and applies rate limiting

The system uses PostgreSQL for persistence, Redis for rate limiting and OTP storage, Kafka for async messaging, and Docker Compose to run the shared infrastructure.

## Architecture

```text
                               +-------------------+
                               |  Frontend / API  |
                               +---------+---------+
                                         |
                                         v
                               +-------------------+
                               | API Gateway       |
                               | port: 9090        |
                               +----+--------+----+
                                    |        |
        +---------------------------+   +---------------------------+
        |                           |   |                           |
        v                           v   v                           v
+------------------+         +---------------------+        +-------------------+
| Account Service  |         | Transaction Service |        | Payment Service   |
| port: 8081       |         | port: 8082         |        | port: 8084        |
+------------------+         +---------------------+        +-------------------+
        |                             |                               |
        |                             v                               |
        |                     +-------------------+                      |
        |                     | Fraud Detection  |                      |
        |                     | Service          |                      |
        |                     | port: 8083       |                      |
        |                     +-------------------+                      |
        |                             |
        |                             v
        |                     +---------------------+
        |                     | Notification Service |
        |                     | port: 8085          |
        |                     +---------------------+
        |
        +--------------------------------------+
                       |
                       v
                 +---------------+
                 | Kafka / Redis |
                 | Postgres      |
                 +---------------+
```

## Tech Stack

- Java 21
- Spring Boot 4
- Spring Cloud Gateway
- Spring Data JPA
- PostgreSQL
- Redis
- Apache Kafka
- Docker & Docker Compose
- Razorpay SDK for payment integration

## Service Details

### 1. Account Service

Location: `account-service/account-service`

Responsibilities:
- Create a bank account
- Fetch account details and balance
- Block an account
- Credit or deduct funds
- Publish/consume account-related bank events

Important endpoints (through gateway):
- `GET /api/v1/accounts` - list all accounts
- `POST /api/v1/accounts` - create account
- `GET /api/v1/accounts/{accountNumber}` - fetch account by number
- `GET /api/v1/accounts/{accountNumber}/balance` - fetch balance
- `PUT /api/v1/accounts/{accountNumber}/block` - block account
- `PUT /api/v1/accounts/{accountNumber}/deduct?amount={amount}` - deduct balance
- `PUT /api/v1/accounts/{accountNumber}/credit?amount={amount}` - credit balance

### 2. Transaction Service

Location: `transaction-service/transaction-service`

Responsibilities:
- Initiate money transfers between sender and receiver accounts
- Save transaction records in PostgreSQL
- Publish `transaction.initiated` events to Kafka
- Process fraud verification and OTP workflows
- Complete or fail transactions and publish refund events

Important endpoints (through gateway):
- `POST /api/v1/transactions/transfer` - create transfer request
- `GET /api/v1/transactions/{transactionId}` - fetch transaction
- `GET /api/v1/transactions/history/{accountNumber}` - transaction history by account
- `POST /api/v1/transactions/{transactionId}/verify?otp={otp}` - verify OTP for transaction

### 3. Fraud Detection Service

Location: `fraud-detection-service/fraud-detection-service`

Responsibilities:
- Consume transaction initiation events
- Check sender balance and run a simple risk evaluation
- Publish `verification.required` when suspicious activity is detected
- Publish `fraud.check.clean` when the transaction appears legitimate

The fraud check uses a random risk evaluation (`Math.random()`) to simulate a real fraud engine. In production, this would be replaced with a deterministic rules engine or ML-based model.

### 4. Notification Service

Location: `notification-service/notification-service`

Responsibilities:
- Consume Kafka events and emit alerts for:
  - OTP generation
  - payment success/failure
  - transaction refund
  - fraud detection
  - transaction completion

Currently this service logs alerts using the application logger rather than sending real emails/SMS.

### 5. Payment Service

Location: `payment-service/payment-service`

Responsibilities:
- Create Razorpay order records
- Generate Razorpay payment orders
- Handle webhook callbacks for successful or failed payments
- Publish payment lifecycle events to Kafka

Important endpoints (through gateway):
- `POST /api/v1/payments/create-order` - create a Razorpay order
- `POST /api/v1/payments/webhook` - receive Razorpay webhook event

### 6. API Gateway

Location: `api-gateway/api-gateway`

Responsibilities:
- Expose a consistent public API
- Route requests to service instances
- Apply rate limiting via Redis
- Support CORS for frontend clients

Gateway routes:
- `/api/v1/accounts/**` -> `account-service`
- `/api/v1/transactions/**` -> `transaction-service`
- `/api/v1/payments/**` -> `payment-service`

Gateway port: `9090`

## Event Flow

The transaction flow is asynchronous and event-driven:

1. Client submits a transfer request to the API gateway.
2. Transaction Service calls Account Service to deduct the sender balance.
3. Transaction metadata is stored in PostgreSQL with `PROCESSING` status.
4. Transaction Service publishes a `transaction.initiated` Kafka event.
5. Fraud Detection Service consumes the event and evaluates risk.
6. If fraud is detected:
   - a `verification.required` event is published
   - Transaction Service generates a one-time password and stores it in Redis
   - an OTP notification is sent via Kafka to Notification Service
7. User verifies the OTP using the `verifyOTP` endpoint.
8. If verification succeeds:
   - transaction status changes to `COMPLETED`
   - `transaction.completed` event is published
   - Account Service credits the receiver account
   - Notification Service sends debit/credit alerts
9. If verification fails or OTP expires:
   - transaction is compensated and refunded
   - a `fraud.detected` event blocks the original account
   - notification events are published

## Infrastructure

The repository includes a Docker Compose configuration for the core platform:

- Redis
- PostgreSQL
- Zookeeper
- Kafka
- Account Service
- Transaction Service
- Fraud Detection Service
- Notification Service
- API Gateway

File: `docker-compose.yml`

Service ports:
- Redis: `6379`
- PostgreSQL: `5432`
- Kafka: `9092`
- Account Service: `8081`
- Transaction Service: `8082`
- Fraud Detection Service: `8083`
- Notification Service: `8085`
- API Gateway: `9090`

Note: The included `docker-compose.yml` file references several application images (`account-service:fix`, `transaction-service:latest`, etc.). Those images should be built or published before running the stack. The payment service is present in source code but is not included as a compose service in this configuration.

## Running the Application

### Prerequisites

- Java 21+
- Maven
- Docker Desktop or Docker Engine
- Docker Compose

### Start infrastructure

From the project root:

```bash
docker compose up -d
```

This starts PostgreSQL, Redis, Zookeeper, and Kafka.

### Build each Spring Boot service

Each service has its own Maven project.

Example:

```bash
cd account-service/account-service
./mvnw clean package

cd ../../transaction-service/transaction-service
./mvnw clean package

cd ../../fraud-detection-service/fraud-detection-service
./mvnw clean package

cd ../../notification-service/notification-service
./mvnw clean package

cd ../../api-gateway/api-gateway
./mvnw clean package
```

### Run services

You can run each service individually with Maven:

```bash
cd account-service/account-service
./mvnw spring-boot:run
```

Or run the Docker Compose services after building the required images.

## Configuration Notes

### PostgreSQL

Database names used by service configuration:
- `account_db`
- `transaction_db`
- `payment_db` (defined in payment service config)

### Redis

Redis is used for:
- API gateway rate limiting
- OTP storage for transaction verification

### Kafka Topics

The system uses topics such as:
- `transaction.initiated`
- `verification.required`
- `transaction.otp.generated`
- `fraud.check.clean`
- `transaction.completed`
- `fraud.detected`
- `transaction.refunded`
- `payment.completed`
- `payment.failed`

## Example API Calls

### Create an account

```bash
curl -X POST http://localhost:9090/api/v1/accounts \
  -H "Content-Type: application/json" \
  -d '{
    "accountHolderName": "John Doe",
    "email": "john@example.com",
    "phone": "+1234567890",
    "initialDeposit": 5000,
    "accountType": "SAVING"
  }'
```

### Get balance

```bash
curl http://localhost:9090/api/v1/accounts/123456789012/balance
```

### Transfer funds

```bash
curl -X POST http://localhost:9090/api/v1/transactions/transfer \
  -H "Content-Type: application/json" \
  -d '{
    "senderAccountNumber": "123456789012",
    "receiverAccountNumber": "987654321098",
    "amount": 1500,
    "description": "Transfer to savings"
  }'
```

### Verify OTP

```bash
curl -X POST "http://localhost:9090/api/v1/transactions/{transactionId}/verify?otp=123456"
```

## Security and Production Considerations

This is a demo application. Before using it in a production environment, consider the following improvements:

- Replace random fraud scoring with a real fraud rules engine or anomaly detection model
- Add JWT/OAuth authentication and authorization
- Use secret management for Razorpay keys and database credentials
- Add retry, dead-letter queues, and idempotency for Kafka consumers
- Add central logging, tracing, and monitoring
- Harden validation and error handling across all services
- Add unit and integration tests for each service

## Project Structure

```text
microservice/
├── account-service/
├── api-gateway/
├── docker-compose.yml
├── fraud-detection-service/
├── notification-service/
├── payment-service/
├── transaction-service/
└── README.md
```

## Summary

This repository demonstrates a practical microservices architecture for a banking platform with event-driven processing, service isolation, and centralized API routing. It is useful as a reference for learning how Spring Boot, Kafka, Redis, and PostgreSQL can work together in a distributed system.
