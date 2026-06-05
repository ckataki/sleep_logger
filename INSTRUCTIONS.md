# How to Run

## Prerequisites

- Docker and Docker Compose
- Ports **5432** (Postgres) and **8080** (API) must be available

## Start the Services

```sh
docker compose up
```

To stop:

```sh
docker compose down
```

## Test the APIs

Once the services are running, execute the test script:

```sh
./exercise_apis.sh
```

This script:
1. Creates a test user directly in Postgres
2. Inserts several sample sleep logs via the API
3. Fetches the last night's sleep
4. Retrieves average stats (7-day and default 30-day lookback)
