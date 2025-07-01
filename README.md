# Truck Driver WhatsApp Wrapper

A Spring Boot service that facilitates WhatsApp communication between TruKKer logistics platform and truck drivers using Infobip's WhatsApp Business API.

## Overview

This service acts as a middleware that:
1. Receives truck driver details from TruKKer
2. Sends WhatsApp messages to drivers via Infobip API
3. Processes driver responses through webhooks
4. Updates TruKKer system with driver status and ETA changes

## System Architecture

```
TruKKer System → Driver WhatsApp Wrapper → Infobip WhatsApp API → Driver's WhatsApp
     ↑                                                                      ↓
     └─────────── Updates (Status/ETA) ←────── Webhook ←─────────────────────┘
```

## Features

- **Initial ETA Check**: Sends templated messages asking if drivers are on time
- **Dynamic Responses**: Handles various driver responses (Yes/No/Delays/Breakdowns)
- **ETA Updates**: Processes new ETA information from drivers
- **Status Tracking**: Updates TruKKer system with real-time driver status
- **Conversation Management**: Maintains conversation state for each driver
- **Error Handling**: Comprehensive error handling and logging

## Prerequisites

- Java 17 or higher
- Maven 3.6+
- Infobip WhatsApp Business API account
- TruKKer API access credentials

## Setup

### 1. Configure Application Properties
Update `src/main/resources/application.properties`:

```properties
# Infobip API Configuration
infobip.api.key=your-infobip-api-key
infobip.whatsapp.from=your-whatsapp-business-number

# Webhook URL (your public domain)
app.webhook.url=https://yourdomain.com
```

### 2. Build and Run
```bash
mvn clean install
mvn spring-boot:run
```

## Complete System Flow

### 1. TruKKer Sends Driver Details
```bash
curl -X POST http://localhost:8080/api/trukker/driver-details \
  -H "Content-Type: application/json" \
  -d '{
    "driverName": "Ahmed",
    "pickup": "Dubai",
    "dropoff": "Abu Dhabi", 
    "moveDate": "26th June 2025",
    "commodity": "Electronics",
    "orderId": "ORD987654321",
    "customerName": "Al Futtaim Logistics",
    "etaTime": "9:30 AM",
    "driverPhone": "971526328601"
  }'
```

### 2. System Sends WhatsApp Message
The driver receives:
```
Hi Ahmed,
Your journey from Dubai to Abu Dhabi is starting at 9:30 AM. Will you be reaching on time?

[Yes I am on time] [I am late]
```

### 3. Driver Responds via WhatsApp
- **If "Yes"**: System updates TruKKer with "on_time" status
- **If "No"**: System asks for delay reason and new ETA

### 4. System Logs TruKKer Updates
Based on driver response, system logs what would be sent to TruKKer:
- Driver status (on_time/delayed/breakdown)  
- New ETA if provided
- Reason for delay if applicable

**Note:** TruKKer API integration is not implemented yet. The system currently logs the updates that would be sent.

## API Endpoints

### Receive Driver Details
**POST** `/api/trukker/driver-details`

### WhatsApp Webhook  
**POST** `/whatsapp/callback`

### Health Check
**GET** `/api/trukker/health`

## Message Flow Examples

### Scenario 1: Driver On Time
1. TruKKer → Wrapper: Driver details
2. Wrapper → Driver: "Will you be on time?"
3. Driver → Wrapper: "Yes I am on time"
4. Wrapper → TruKKer: Update status to "on_time"
5. Wrapper → Driver: "Thank you! Have a safe journey!"

### Scenario 2: Driver Delayed
1. TruKKer → Wrapper: Driver details
2. Wrapper → Driver: "Will you be on time?"
3. Driver → Wrapper: "I am late"
4. Wrapper → Driver: "What's the issue? 1.Traffic 2.Breakdown 3.Other"
5. Driver → Wrapper: "1" 
6. Wrapper → Driver: "Please provide new ETA"
7. Driver → Wrapper: "10:30 AM"
8. Wrapper → TruKKer: Update ETA to "10:30 AM"
9. Wrapper → Driver: "ETA updated to 10:30 AM"

## Configuration Files

The system includes these key files:
- `DriverDetails.java` - Model for TruKKer data
- `WhatsAppMessage.java` - Infobip API message format
- `WhatsAppService.java` - Handles Infobip API calls
- `TruKKerService.java` - Handles TruKKer API calls
- `MessageProcessingService.java` - Processes driver responses
- `DriverController.java` - Receives TruKKer requests
- `WhatsAppWebhookController.java` - Handles Infobip webhooks

## Deployment

Set environment variables:
```bash
export INFOBIP_API_KEY=your-key
export APP_WEBHOOK_URL=https://yourdomain.com
```

## Testing

Test with sample requests:
```bash
# Test driver details endpoint
curl -X POST http://localhost:8080/api/trukker/driver-details -H "Content-Type: application/json" -d '{"driverName":"Ahmed","pickup":"Dubai","dropoff":"Abu Dhabi","orderId":"ORD123","etaTime":"9:30 AM","driverPhone":"971526328601"}'

# Test health check
curl http://localhost:8080/api/trukker/health
```

---

**Note**: Replace placeholder API keys and URLs with actual values for your environment. 