#!/bin/bash

# Webhook POST Curl Commands for localhost
# Endpoint: POST http://localhost:8080/whatsapp/callback

BASE_URL="http://localhost:8080"

echo "=== WhatsApp Webhook Test Commands ==="
echo "Endpoint: $BASE_URL/whatsapp/callback"
echo ""

# 1. Driver says "Yes I am on time"
echo "1. Driver responds: 'Yes I am on time'"
curl -X POST "$BASE_URL/whatsapp/callback" \
  -H "Content-Type: application/json" \
  -d '{
    "results": [
      {
        "from": "971526328601",
        "to": "919601112550",
        "messageId": "ORD987654321",
        "receivedAt": "2025-06-30T09:30:00",
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

# 2. Driver says "I am late"
echo "2. Driver responds: 'I am late'"
curl -X POST "$BASE_URL/whatsapp/callback" \
  -H "Content-Type: application/json" \
  -d '{
    "results": [
      {
        "from": "971526328601",
        "to": "919601112550",
        "messageId": "ORD987654321",
        "receivedAt": "2025-06-30T09:35:00",
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

# 3. Driver selects traffic delay (option 1)
echo "3. Driver selects: '1' (Traffic delay)"
curl -X POST "$BASE_URL/whatsapp/callback" \
  -H "Content-Type: application/json" \
  -d '{
    "results": [
      {
        "from": "971526328601",
        "to": "919601112550",
        "messageId": "ORD987654321",
        "receivedAt": "2025-06-30T09:40:00",
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

# 4. Driver provides new ETA
echo "4. Driver provides new ETA: '10:30 AM'"
curl -X POST "$BASE_URL/whatsapp/callback" \
  -H "Content-Type: application/json" \
  -d '{
    "results": [
      {
        "from": "971526328601",
        "to": "919601112550",
        "messageId": "ORD987654321",
        "receivedAt": "2025-06-30T09:45:00",
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

# 5. Driver selects breakdown (option 2)
echo "5. Driver selects: '2' (Vehicle breakdown)"
curl -X POST "$BASE_URL/whatsapp/callback" \
  -H "Content-Type: application/json" \
  -d '{
    "results": [
      {
        "from": "971526328601",
        "to": "919601112550",
        "messageId": "ORD987654321",
        "receivedAt": "2025-06-30T09:50:00",
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

# 6. Driver provides breakdown details
echo "6. Driver provides breakdown details"
curl -X POST "$BASE_URL/whatsapp/callback" \
  -H "Content-Type: application/json" \
  -d '{
    "results": [
      {
        "from": "971526328601",
        "to": "919601112550",
        "messageId": "ORD987654321",
        "receivedAt": "2025-06-30T09:55:00",
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

echo "=== One-liner Commands ==="
echo ""
echo "Quick test - Driver says Yes:"
echo "curl -X POST '$BASE_URL/whatsapp/callback' -H 'Content-Type: application/json' -d '{\"results\":[{\"from\":\"971526328601\",\"to\":\"919601112550\",\"messageId\":\"ORD987654321\",\"receivedAt\":\"2025-06-30T09:30:00\",\"message\":{\"type\":\"text\",\"text\":\"Yes I am on time\"},\"context\":{\"from\":\"971526328601\",\"id\":\"msg123456\",\"groupId\":\"group123\"}}]}'"
echo ""
echo "Quick test - Driver says No:"
echo "curl -X POST '$BASE_URL/whatsapp/callback' -H 'Content-Type: application/json' -d '{\"results\":[{\"from\":\"971526328601\",\"to\":\"919601112550\",\"messageId\":\"ORD987654321\",\"receivedAt\":\"2025-06-30T09:35:00\",\"message\":{\"type\":\"text\",\"text\":\"I am late\"},\"context\":{\"from\":\"971526328601\",\"id\":\"msg123457\",\"groupId\":\"group123\"}}]}'"
echo ""
echo "Expected Response:"
echo '{"status": "success", "message": "Message processed successfully"}' 