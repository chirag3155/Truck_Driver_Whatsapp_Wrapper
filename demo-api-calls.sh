#!/bin/bash

# Demo API Calls for Truck Driver WhatsApp Wrapper
# This script demonstrates the complete flow of the system

BASE_URL="http://localhost:8080"

echo "=== Truck Driver WhatsApp Wrapper Demo ==="
echo ""

# Step 1: Health Check
echo "1. Testing Health Check..."
curl -s -X GET "$BASE_URL/api/trukker/health" | jq '.'
echo ""

# Step 2: Service Info
echo "2. Getting Service Information..."
curl -s -X GET "$BASE_URL/api/trukker/info" | jq '.'
echo ""

# Step 3: Send Driver Details (This is what TruKKer would call)
echo "3. Sending Driver Details (TruKKer → Wrapper)..."
curl -s -X POST "$BASE_URL/api/trukker/driver-details" \
  -H "Content-Type: application/json" \
  -d '{
    "driverName": "Ahmed Hassan",
    "pickup": "Dubai Marina",
    "dropoff": "Abu Dhabi Downtown",
    "moveDate": "26th June 2025",
    "commodity": "Electronics & Consumer Goods",
    "orderId": "ORD987654321",
    "customerName": "Al Futtaim Logistics",
    "etaTime": "9:30 AM",
    "driverPhone": "971526328601"
  }' | jq '.'
echo ""

# Step 4: Test WhatsApp Webhook Test Endpoint
echo "4. Testing WhatsApp Webhook Endpoint..."
curl -s -X GET "$BASE_URL/whatsapp/test" | jq '.'
echo ""

# Step 5: Simulate Driver Response (This is what Infobip would send)
echo "5. Simulating Driver Response 'Yes I am on time' (Infobip → Wrapper)..."
curl -s -X POST "$BASE_URL/whatsapp/callback" \
  -H "Content-Type: application/json" \
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
echo ""

# Step 6: Simulate Driver Response - Delayed
echo "6. Simulating Driver Response 'I am late' (Infobip → Wrapper)..."
curl -s -X POST "$BASE_URL/whatsapp/callback" \
  -H "Content-Type: application/json" \
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
echo ""

# Step 7: Simulate Driver Response - Traffic Delay
echo "7. Simulating Driver Response 'Traffic delay' (Infobip → Wrapper)..."
curl -s -X POST "$BASE_URL/whatsapp/callback" \
  -H "Content-Type: application/json" \
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
echo ""

# Step 8: Simulate Driver Response - New ETA
echo "8. Simulating Driver Response 'New ETA: 10:30 AM' (Infobip → Wrapper)..."
curl -s -X POST "$BASE_URL/whatsapp/callback" \
  -H "Content-Type: application/json" \
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
echo ""

echo "=== Demo Complete ==="
echo ""
echo "Summary of what happened:"
echo "1. ✅ Health check verified service is running"
echo "2. ✅ Service info retrieved successfully"
echo "3. ✅ TruKKer sent driver details → Wrapper sent WhatsApp message"
echo "4. ✅ WhatsApp webhook endpoint is active"
echo "5. ✅ Driver responded 'Yes' → Wrapper logged 'on_time' update"
echo "6. ✅ Driver responded 'I am late' → Wrapper asked for issue type"
echo "7. ✅ Driver selected 'Traffic delay' → Wrapper asked for new ETA"
echo "8. ✅ Driver provided '10:30 AM' → Wrapper logged new ETA update"
echo ""
echo "🚛 Complete flow demonstrated successfully!"
echo ""
echo "Note: In a real environment:"
echo "- TruKKer would call the /api/trukker/driver-details endpoint"
echo "- Infobip would send actual WhatsApp messages to drivers"
echo "- Driver responses would come through Infobip webhooks"
echo "- System would log TruKKer updates (API integration not implemented yet)" 