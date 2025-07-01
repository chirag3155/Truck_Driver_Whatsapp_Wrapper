#!/bin/bash

# Curl Commands for Driver Controller (TruKKer Integration)
# Base URL: localhost:8080

BASE_URL="http://localhost:8080"

echo "=== Driver Controller API Tests ==="
echo "Base URL: $BASE_URL"
echo ""

# 1. Health Check
echo "1. Health Check"
echo "GET $BASE_URL/api/trukker/health"
echo ""
curl -X GET "$BASE_URL/api/trukker/health" \
  -H "Accept: application/json" | jq '.'
echo -e "\n"

# 2. Send Driver Details (Main endpoint)
echo "2. Send Driver Details"
echo "POST $BASE_URL/api/trukker/driver-details"
echo ""
curl -X POST "$BASE_URL/api/trukker/driver-details" \
  -H "Accept: application/json" \
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
echo -e "\n"

# 3. Send Driver Details - Another Example
echo "3. Send Driver Details - Different Driver"
echo "POST $BASE_URL/api/trukker/driver-details"
echo ""
curl -X POST "$BASE_URL/api/trukker/driver-details" \
  -H "Accept: application/json" \
  -H "Content-Type: application/json" \
  -d '{
    "driverName": "Mohammed Ali",
    "pickup": "Sharjah Industrial Area",
    "dropoff": "Dubai International Airport",
    "moveDate": "27th June 2025",
    "commodity": "Pharmaceutical Products",
    "orderId": "ORD987654322",
    "customerName": "Emirates Cargo",
    "etaTime": "2:15 PM",
    "driverPhone": "971555123456"
  }' | jq '.'
echo -e "\n"

# 4. Test with Invalid Data (Missing required fields)
echo "4. Test Validation - Missing Required Fields"
echo "POST $BASE_URL/api/trukker/driver-details"
echo ""
curl -X POST "$BASE_URL/api/trukker/driver-details" \
  -H "Accept: application/json" \
  -H "Content-Type: application/json" \
  -d '{
    "driverName": "",
    "pickup": "Dubai",
    "orderId": ""
  }' | jq '.'
echo -e "\n"

# 5. Test with Minimal Valid Data
echo "5. Test with Minimal Valid Data"
echo "POST $BASE_URL/api/trukker/driver-details"
echo ""
curl -X POST "$BASE_URL/api/trukker/driver-details" \
  -H "Accept: application/json" \
  -H "Content-Type: application/json" \
  -d '{
    "driverName": "Saeed Ahmed",
    "pickup": "Abu Dhabi",
    "dropoff": "Al Ain",
    "moveDate": "28th June 2025",
    "orderId": "ORD987654323",
    "etaTime": "11:00 AM",
    "driverPhone": "971501234567"
  }' | jq '.'
echo -e "\n"

echo "=== Test Summary ==="
echo "✅ Test 1: Health check should return status 'UP'"
echo "✅ Test 2-3: Valid driver details should return success"
echo "❌ Test 4: Invalid data should return 400 Bad Request"
echo "✅ Test 5: Minimal valid data should return success"
echo ""
echo "Expected Response Format:"
echo '{'
echo '  "status": "success",'
echo '  "message": "Driver details received and WhatsApp message sent successfully",'
echo '  "orderId": "ORD987654321",'
echo '  "driverPhone": "971526328601",'
echo '  "timestamp": "2025-06-30T15:30:00"'
echo '}'
echo ""
echo "Note: Make sure your application is running on localhost:8080" 