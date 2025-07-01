# Swagger API Documentation Access

## Accessing Swagger UI

Once your application is running, you can access the Swagger UI documentation at:

**Swagger UI:** http://localhost:8080/swagger-ui.html
**OpenAPI JSON:** http://localhost:8080/api-docs

## Available API Groups

The API documentation is organized into the following groups:

### 1. TruKKer Integration API
- **Group:** `trukker-integration`
- **Endpoints:** `/api/trukker/**`
- **Description:** APIs for receiving driver details from TruKKer system

**Key Endpoints:**
- `POST /api/trukker/driver-details` - Receive driver details and initiate WhatsApp communication
- `GET /api/trukker/health` - Health check endpoint

### 2. WhatsApp Webhook API
- **Group:** `whatsapp-webhooks`  
- **Endpoints:** `/whatsapp/**`
- **Description:** Webhook endpoints for receiving messages from Infobip WhatsApp API

**Key Endpoints:**
- `POST /whatsapp/callback` - Receive driver responses from Infobip
- `POST /whatsapp/delivery-report` - Receive message delivery reports
- `GET /whatsapp/test` - Test webhook configuration

### 3. Complete API Overview
- **Group:** `truck-driver-whatsapp`
- **Description:** All APIs in one view

## Sample API Requests

### Send Driver Details
```json
POST /api/trukker/driver-details
{
  "driverName": "Ahmed Hassan",
  "pickup": "Dubai Marina",
  "dropoff": "Abu Dhabi Downtown",
  "moveDate": "26th June 2025",
  "commodity": "Electronics & Consumer Goods",
  "orderId": "ORD987654321",
  "customerName": "Al Futtaim Logistics",
  "etaTime": "9:30 AM",
  "driverPhone": "971526328601"
}
```

### Webhook Response (from Infobip)
```json
POST /whatsapp/callback
{
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
}
```

## Features

- **Interactive Testing:** Test all endpoints directly from the Swagger UI
- **Request/Response Examples:** See sample requests and responses
- **Schema Documentation:** Detailed model documentation with validation rules
- **Authentication:** JWT Bearer token support (when implemented)
- **Multiple Environments:** Switch between development and production servers

## Development Tips

1. **Testing Flow:** Use the Swagger UI to test the complete flow:
   - Send driver details via `/api/trukker/driver-details`
   - Simulate driver responses via `/whatsapp/callback`
   - Check health status via `/api/trukker/health`

2. **Authentication:** When security is implemented, use the "Authorize" button to set your JWT token

3. **Response Validation:** All responses include examples to help understand the expected format

4. **Error Handling:** Each endpoint documents possible error responses with examples

## URL Summary

| Purpose | URL |
|---------|-----|
| Swagger UI | http://localhost:8080/swagger-ui.html |
| OpenAPI Spec | http://localhost:8080/api-docs |
| Health Check | http://localhost:8080/api/trukker/health |
| Test Webhook | http://localhost:8080/whatsapp/test |

---

**Note:** Make sure the application is running on port 8080 before accessing these URLs. 