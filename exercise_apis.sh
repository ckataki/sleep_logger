#!/usr/bin/env bash
set -euo pipefail

pause() {
  echo
  read -p "Press Enter to continue..."
  echo
}

BASE_URL="http://localhost:8080"

USERNAME="test_user_$(date +%s)"
echo "=== 1. Create Test User ($USERNAME) ==="
USER_ID=$(podman exec -i postgres_db psql -q -U user -d postgres -tAc \
  "INSERT INTO users (username) VALUES ('$USERNAME') RETURNING id;")
echo "User ID: $USER_ID"
pause

YESTERDAY=$(date -d "yesterday" +%Y-%m-%d)
DAY2=$(date -d "yesterday - 1 day" +%Y-%m-%d)
DAY3=$(date -d "yesterday - 2 days" +%Y-%m-%d)
DAY4=$(date -d "yesterday - 3 days" +%Y-%m-%d)
DAY5=$(date -d "yesterday - 4 days" +%Y-%m-%d)

echo "=== 2. Create Sleep Log: $DAY5 (BAD, 4h) ==="
curl -X POST "$BASE_URL/user/$USER_ID/sleep/add" \
  -H "Content-Type: application/json" \
  -d '{
    "user_id": "'"$USER_ID"'",
    "sleep_date": "'"$DAY5"'",
    "start_time": "02:00",
    "end_time": "06:00",
    "duration": "PT4H",
    "quality": "BAD"
  }'
pause

echo "=== 3. Create Sleep Log: $DAY4 (OK, 6h) ==="
curl -sS -X POST "$BASE_URL/user/$USER_ID/sleep/add" \
  -H "Content-Type: application/json" \
  -d '{
    "user_id": "'"$USER_ID"'",
    "sleep_date": "'"$DAY4"'",
    "start_time": "01:00",
    "end_time": "07:00",
    "duration": "PT6H",
    "quality": "OK"
  }'
pause

echo "=== 4. Create Sleep Log: $DAY3 (GOOD, 8h) ==="
curl -sS -X POST "$BASE_URL/user/$USER_ID/sleep/add" \
  -H "Content-Type: application/json" \
  -d '{
    "user_id": "'"$USER_ID"'",
    "sleep_date": "'"$DAY3"'",
    "start_time": "22:30",
    "end_time": "06:30",
    "duration": "PT8H",
    "quality": "GOOD"
  }'
pause

echo "=== 5. Create Sleep Log: $DAY2 (OK, 5h30m) ==="
curl -sS -X POST "$BASE_URL/user/$USER_ID/sleep/add" \
  -H "Content-Type: application/json" \
  -d '{
    "user_id": "'"$USER_ID"'",
    "sleep_date": "'"$DAY2"'",
    "start_time": "23:30",
    "end_time": "05:00",
    "duration": "PT5H30M",
    "quality": "OK"
  }'
pause

echo "=== 6. Create Sleep Log: $YESTERDAY (GOOD, 7h45m) ==="
curl -sS -X POST "$BASE_URL/user/$USER_ID/sleep/add" \
  -H "Content-Type: application/json" \
  -d '{
    "user_id": "'"$USER_ID"'",
    "sleep_date": "'"$YESTERDAY"'",
    "start_time": "22:45",
    "end_time": "06:30",
    "duration": "PT7H45M",
    "quality": "GOOD"
  }'
pause

echo "=== 7. Get Last Night's Sleep ==="
curl -sS "$BASE_URL/user/$USER_ID/sleep/last"
pause

echo "=== 8. Get Sleep Averages (7-day lookback) ==="
curl -sS "$BASE_URL/user/$USER_ID/sleep/average?lookback_days=7"
pause

echo "=== 9. Get Sleep Averages (default 30-day lookback) ==="
curl -sS "$BASE_URL/user/$USER_ID/sleep/average"
pause

echo "=== Done ==="
