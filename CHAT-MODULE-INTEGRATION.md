# Chat Module Integration for Truck Driver WhatsApp Wrapper

## Overview

The Truck Driver WhatsApp Wrapper has been integrated with a Chat Module API to provide intelligent AI-powered responses to truck drivers instead of simple keyword-based responses.

## Architecture

### Previous Flow
1. Driver sends WhatsApp message
2. Simple keyword matching (positive/negative responses)
3. Predefined static responses

### New Flow
1. Driver sends WhatsApp message → `MessageProcessingService`
2. Extract message details (phone, content, message_id, timestamp)
3. Generate/retrieve conversation_id based on driver phone number
4. Call Chat Module API with structured payload
5. Parse API response and extract "chunk" (AI response)
6. Send AI response back to driver via WhatsApp

## Key Components

### 1. ChatModuleService
- **Location**: `src/main/java/com/driver/whatsapp/wrapper/service/ChatModuleService.java`
- **Purpose**: Handles all communication with the Chat Module API
- **Key Features**:
  - Conversation management per driver
  - Error handling and fallback responses
  - Health check capabilities

### 2. Modified MessageProcessingService
- **Location**: `src/main/java/com/driver/whatsapp/wrapper/service/MessageProcessingService.java`
- **Changes**: 
  - Replaced keyword-based logic with Chat Module integration
  - Added conversation reset functionality
  - Enhanced debugging capabilities

### 3. Enhanced DriverController
- **Location**: `src/main/java/com/driver/whatsapp/wrapper/controller/DriverController.java`
- **New Endpoints**:
  - `/wawrapper/test-chat` - Test chat module integration
  - `/wawrapper/reset-conversation` - Reset driver conversation
  - `/wawrapper/conversation-status` - Get conversation status

## Chat Module API Configuration

### Fixed Configuration Values
```java
private static final String CHAT_API_URL = "https://digicel-sandbox.bngrenew.com/chat_module/chat";
private static final String TENANT_ID = "US_DIG_f12cba33";
private static final String ASSISTANT_ID = "1115";
private static final String AUTH_TOKEN_SUFFIX = "_5";
private static final String PLATFORM = "Whatsapp";
private static final String LANGUAGE_ID = "en-US";
private static final String LANGUAGE_NAME = "English";
private static final String MESSAGE_TYPE = "text";
```

### Dynamic Values (from driver message)
- `user_id`: Driver phone number
- `phone_no`: Driver phone number
- `name`: Driver's actual name from webhook contact field (defaults to "Driver" if not available)
- `conversation_id`: Generated UUID per driver
- `auth_token`: Driver phone number (digits only) + "_5"
- `text`: Message content from driver
- `message_id`: WhatsApp message ID
- `timestamp`: Parsed from `receivedAt` field in webhook (ISO 8601 format)

### API Request Payload Example
```json
{
  "conversation_id": "b774e7a2-dfb4-4f71-8b1a-e69c41fbd177",
  "language_id": "en-US",
  "language_name": "English",
  "tenant_id": "US_DIG_f12cba33",
  "user_id": "385919998888",
  "name": "Frank",
  "phone_no": "385919998888",
  "auth_token": "385919998888_5",
  "assistant_id": "1115",
  "platform": "Whatsapp",
  "text": "I will be late due to traffic",
  "message_id": "USER_123",
  "timestamp": 1635517800,
  "message_type": "text"
}
```

### API Response Example
```json
{
  "user_id": "385919998888",
  "conversation_id": "b774e7a2-dfb4-4f71-8b1a-e69c41fbd177",
  "chunk": "Hello Frank! I understand you're a truck driver. How can I assist you today?",
  "message_id": "599ed8e",
  "timestamp": 1751381261391,
  "is_end": true,
  "language_id": "en-US",
  "language_name": "English",
  "message_type": "text"
}
```

## Testing Endpoints

### 1. Test Chat Module
```bash
POST /wawrapper/test-chat?driverPhone=+1234567890&message=Hello
```
**Response**:
```json
{
  "status": "success",
  "driverPhone": "+1234567890",
  "message": "Hello",
  "chatModuleHealthy": true,
  "conversationId": "b774e7a2-dfb4-4f71-8b1a-e69c41fbd177",
  "timestamp": "2025-01-18T10:30:00"
}
```

### 2. Reset Conversation
```bash
POST /wawrapper/reset-conversation?driverPhone=+1234567890
```
**Response**:
```json
{
  "status": "success",
  "message": "Conversation reset successfully",
  "driverPhone": "+1234567890",
  "timestamp": "2025-01-18T10:30:00"
}
```

### 3. Get Conversation Status
```bash
GET /wawrapper/conversation-status?driverPhone=+1234567890
```
**Response**:
```json
{
  "status": "success",
  "driverPhone": "+1234567890",
  "conversationId": "b774e7a2-dfb4-4f71-8b1a-e69c41fbd177",
  "hasActiveConversation": true,
  "chatModuleHealthy": true,
  "timestamp": "2025-01-18T10:30:00"
}
```

## Error Handling

The system includes comprehensive error handling:

1. **API Timeout**: Returns user-friendly message about slow response
2. **Client Errors (4xx)**: Returns generic error message
3. **Server Errors (5xx)**: Returns service unavailable message
4. **Network Issues**: Returns connection problem message
5. **Fallback Response**: Generic error message for unexpected issues

## Logging

All interactions are logged with appropriate levels:
- **INFO**: Normal message processing, API responses
- **DEBUG**: Detailed payload information, conversation management
- **ERROR**: API failures, exceptions, health check failures
- **WARN**: Empty messages, validation issues

## Conversation Management

### Conversation ID Generation
- Each driver gets a unique conversation ID (UUID)
- Conversation IDs are stored in memory (`ConcurrentHashMap`)
- Conversation IDs persist until explicitly reset or timeout.

### Conversation Timeout & Auto-Cleanup
- **Timeout Duration**: 30 minutes of inactivity (configurable)
- **Auto-Cleanup**: Expired conversations are automatically reset
- **Memory Management**: Prevents unlimited conversation accumulation
- **Fresh Context**: Drivers get fresh conversations after long breaks

#### How Timeout Works:
1. **Activity Tracking**: System tracks last message timestamp per driver
2. **Expiry Check**: Before processing new messages, checks if conversation expired
3. **Auto-Reset**: If timeout exceeded, conversation is automatically reset
4. **Fresh Start**: Next message starts a new conversation with clean context

#### Example Timeline:
```
10:00 AM - Driver sends "Hello" → Conversation starts
10:30 AM - Driver sends "I'm stuck in traffic" → Conversation continues
12:31 PM - Driver sends "I'm moving again" → Conversation expired (>30min), auto-reset, fresh start
```

### Auth Token Generation
- Format: `[phone_digits]_5`
- Example: Phone `+1234567890` → Auth token `1234567890_5`
- Removes all non-digit characters from phone number

### Timestamp Handling
- Uses actual `receivedAt` timestamp from WhatsApp webhook
- Format: ISO 8601 with timezone (e.g., `2025-01-01T10:10:00.000+0000`)
- Parsed to Unix timestamp (milliseconds) for chat module API
- Falls back to current time if parsing fails

### Contact Name Handling
- Uses actual driver name from webhook `contact.name` field
- Example: `"contact": {"name": "Frank"}` → `"name": "Frank"` in API payload
- Falls back to generic "Driver" if contact name is not available or empty
- Improves personalization in AI responses

## Health Monitoring

The system includes health check functionality:
- Tests chat module with a simple "Hello" message
- Returns `true` if chat module responds normally
- Returns `false` if any errors occur or response contains error indicators

## Deployment Notes

### Dependencies
- `RestTemplate` is configured in `AppConfig.java` with 30-second timeouts
- Jackson for JSON serialization/deserialization
- Lombok for logging (`@Slf4j`)

### Configuration
- All chat module settings are hardcoded constants
- No external configuration files required
- Database/Redis not required for basic functionality (uses in-memory storage)

## Conversation Timeout Management

### Available Methods

#### `getConversationStatus(driverPhone)`
Returns detailed conversation status including:
- Has active conversation
- Last message timestamp
- Minutes since last activity
- Whether conversation is still active
- Current conversation ID

#### `getConversationTimeoutMinutes()`
Returns current timeout configuration (30 minutes by default)

#### `cleanupAllExpiredConversations()`
Manually triggers cleanup of all expired conversations
- Returns count of cleaned conversations
- Useful for maintenance tasks

#### `resetConversationState(driverPhone)`
Manually resets specific driver's conversation and timestamp tracking

## Production Considerations

1. **Conversation Storage**: Consider using Redis or database for conversation persistence across restarts
2. **Load Balancing**: Conversation state is stored in memory, consider sticky sessions or external storage
3. **Monitoring**: Add metrics for chat module response times and success rates
4. **Rate Limiting**: Consider implementing rate limiting for chat module API calls
5. **Timeout Configuration**: The 30-minute timeout is configurable in `CONVERSATION_TIMEOUT_MS`
6. **Memory Management**: Auto-cleanup prevents memory leaks from abandoned conversations
7. **Maintenance**: Use `cleanupAllExpiredConversations()` for periodic maintenance

## API Documentation

The endpoints are documented with OpenAPI annotations and are available via Swagger UI at:
- `http://localhost:8080/swagger-ui.html` (when running locally)

## Migration from Old System

To migrate from the old keyword-based system:
1. The old methods are still present but not used
2. All message processing now goes through `ChatModuleService`
3. Old conversation state logic is preserved for compatibility
4. Reset functionality works with both old and new systems

## Troubleshooting

### Common Issues

1. **Chat Module Not Responding**
   - Check network connectivity to `https://digicel-sandbox.bngrenew.com/chat_module/chat`
   - Verify API endpoint is accessible
   - Check logs for specific error messages

2. **Conversation Not Starting**
   - Verify phone number format
   - Check if conversation ID is being generated
   - Use `/conversation-status` endpoint to debug

3. **Auth Token Issues**
   - Ensure phone number contains digits
   - Verify auth token format: `[digits]_5`
   - Check API logs for authentication errors

4. **Timestamp Parsing Issues**
   - Check webhook `receivedAt` format is valid ISO 8601
   - Look for "Failed to parse receivedAt timestamp" warnings in logs
   - System falls back to current time if parsing fails

5. **Conversation Timeout Issues**
   - Check for "Conversation expired for driver" messages in logs
   - Verify timeout setting (30 minutes default) is appropriate
   - Use `getConversationStatus()` to debug conversation state
   - Monitor memory usage to ensure cleanup is working

### Log Analysis
```bash
# Check chat module integration logs
grep "ChatModuleService" logs/application.log

# Check conversation management
grep "conversation" logs/application.log

# Check API responses
grep "Received AI response" logs/application.log

# Check timestamp parsing issues
grep "Failed to parse receivedAt timestamp" logs/application.log

# Check conversation timeout activity
grep "Conversation expired for driver" logs/application.log

# Check conversation cleanup
grep "Cleanup completed" logs/application.log
``` 