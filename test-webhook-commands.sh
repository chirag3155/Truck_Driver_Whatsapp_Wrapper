#!/bin/bash

# Test Commands for WhatsApp Webhook
# This script contains proper curl commands to test the webhook with correct JSON format

BASE_URL="http://localhost:8080"

echo "=== Testing WhatsApp Webhook Endpoints ==="
echo ""

# Test 1: Driver says "Yes I am on time"
echo "1. Testing Driver Response: 'Yes I am on time'"
curl -X 'POST' \
  "$BASE_URL/whatsapp/callback" \
  -H 'accept: application/json' \
  -H 'Content-Type: application/json' \
  -d '{
    "results": [
      {
        "from": "971526328601",
        "to": "919601112550",
        "messageId": "ORD987654321",
        "receivedAt": "2025-06-25T09:30:00",
        "message": {
          "type": "text",
          "text": "Yes I am on time"
        },
        "context": {
          "from": "971526328601",
          "id": "msg123456",
          "groupId": "group123"
        }
      }
    ]
  }' | jq '.'
echo -e "\n"

# Test 2: Driver says "I am late"
echo "2. Testing Driver Response: 'I am late'"
curl -X 'POST' \
  "$BASE_URL/whatsapp/callback" \
  -H 'accept: application/json' \
  -H 'Content-Type: application/json' \
  -d '{
    "results": [
      {
        "from": "971526328601",
        "to": "919601112550",
        "messageId": "ORD987654321",
        "receivedAt": "2025-06-25T09:35:00",
        "message": {
          "type": "text",
          "text": "I am late"
        },
        "context": {
          "from": "971526328601",
          "id": "msg123457",
          "groupId": "group123"
        }
      }
    ]
  }' | jq '.'
echo -e "\n"

# Test 3: Driver selects traffic delay (Option 1)
echo "3. Testing Driver Response: Traffic Delay (Option 1)"
curl -X 'POST' \
  "$BASE_URL/whatsapp/callback" \
  -H 'accept: application/json' \
  -H 'Content-Type: application/json' \
  -d '{
    "results": [
      {
        "from": "971526328601",
        "to": "919601112550",
        "messageId": "ORD987654321",
        "receivedAt": "2025-06-25T09:40:00",
        "message": {
          "type": "text",
          "text": "1"
        },
        "context": {
          "from": "971526328601",
          "id": "msg123458",
          "groupId": "group123"
        }
      }
    ]
  }' | jq '.'
echo -e "\n"

# Test 4: Driver provides new ETA
echo "4. Testing Driver Response: New ETA"
curl -X 'POST' \
  "$BASE_URL/whatsapp/callback" \
  -H 'accept: application/json' \
  -H 'Content-Type: application/json' \
  -d '{
    "results": [
      {
        "from": "971526328601",
        "to": "919601112550",
        "messageId": "ORD987654321",
        "receivedAt": "2025-06-25T09:45:00",
        "message": {
          "type": "text",
          "text": "10:30 AM"
        },
        "context": {
          "from": "971526328601",
          "id": "msg123459",
          "groupId": "group123"
        }
      }
    ]
  }' | jq '.'
echo -e "\n"

# Test 5: Driver selects breakdown (Option 2)
echo "5. Testing Driver Response: Breakdown (Option 2)"
curl -X 'POST' \
  "$BASE_URL/whatsapp/callback" \
  -H 'accept: application/json' \
  -H 'Content-Type: application/json' \
  -d '{
    "results": [
      {
        "from": "971526328601",
        "to": "919601112550",
        "messageId": "ORD987654321",
        "receivedAt": "2025-06-25T09:50:00",
        "message": {
          "type": "text",
          "text": "2"
        },
        "context": {
          "from": "971526328601",
          "id": "msg123460",
          "groupId": "group123"
        }
      }
    ]
  }' | jq '.'
echo -e "\n"

# Test 6: Driver provides breakdown details
echo "6. Testing Driver Response: Breakdown Details"
curl -X 'POST' \
  "$BASE_URL/whatsapp/callback" \
  -H 'accept: application/json' \
  -H 'Content-Type: application/json' \
  -d '{
    "results": [
      {
        "from": "971526328601",
        "to": "919601112550",
        "messageId": "ORD987654321",
        "receivedAt": "2025-06-25T09:55:00",
        "message": {
          "type": "text",
          "text": "Engine overheating, waiting for roadside assistance"
        },
        "context": {
          "from": "971526328601",
          "id": "msg123461",
          "groupId": "group123"
        }
      }
    ]
  }' | jq '.'
echo -e "\n"

# Test 7: Test with invalid JSON (should return 400 error with helpful message)
echo "7. Testing Invalid JSON (should return 400 error)"
curl -X 'POST' \
  "$BASE_URL/whatsapp/callback" \
  -H 'accept: application/json' \
  -H 'Content-Type: application/json' \
  -d '"hey"' | jq '.'
echo -e "\n"

echo "=== Testing Complete ==="
echo ""
echo "Expected Results:"
echo "✅ Tests 1-6 should return 200 with 'success' status"
echo "✅ Test 7 should return 400 with helpful error message"
echo ""
echo "Note: Make sure your application is running on localhost:8080" 