# Conversation Timeout Demo

## How It Works

### Scenario 1: Normal Active Conversation (No Timeout)
```
10:00 AM - Driver Frank: "Hello, I'm starting my delivery"
         → Response: "Hello Frank! I understand you're a truck driver..."
         → Conversation ID: abc-123-def

10:15 AM - Driver Frank: "I'm at the pickup location"  
         → Same conversation continues (ID: abc-123-def)
         → 15 minutes since last message < 30-minute timeout ✅

10:45 AM - Driver Frank: "Heading to delivery now"
         → Same conversation continues (ID: abc-123-def)
         → 30 minutes since first message, but only 15 minutes since 10:15 AM ✅
```

### Scenario 2: Conversation Timeout & Auto-Reset
```
10:00 AM - Driver Frank: "Hello, I'm starting my delivery"
         → Response: "Hello Frank! I understand you're a truck driver..."
         → Conversation ID: abc-123-def

11:05 AM - Driver Frank: "I'm back, delivery complete!"
         → 65 minutes since last message > 30-minute timeout ⏰
         → System logs: "🧹 Conversation expired for driver +1234567890 (inactive for 65 minutes)"
         → Old conversation (abc-123-def) is reset
         → NEW conversation starts with fresh context
         → New Conversation ID: xyz-789-ghi
```

### Scenario 3: Multiple Drivers - Independent Timeouts
```
Driver A (Frank):
10:00 AM - "Hello" → Conversation starts
10:15 AM - "Update" → Conversation continues
11:00 AM - [No timeout yet - only 45 min since start, 45 min since last message]

Driver B (John):  
10:30 AM - "Hi there" → Different conversation starts
12:15 PM - "Status update" → 105 minutes since start > timeout
           → Driver B's conversation resets, Driver A's still active
```

## Configuration

### Current Settings
- **Timeout**: 30 minutes of inactivity
- **Location**: `CONVERSATION_TIMEOUT_MS` in `MessageProcessingService.java`
- **Value**: `30 * 60 * 1000L` (30 minutes in milliseconds)

### To Change Timeout (Example: 1 hour)
```java
private static final long CONVERSATION_TIMEOUT_MS = 60 * 60 * 1000L; // 60 minutes
```

## Monitoring & Debug

### Check Conversation Status
```java
ConversationStatus status = messageProcessingService.getConversationStatus("+1234567890");
System.out.println("Has conversation: " + status.isHasConversation());
System.out.println("Minutes since last message: " + status.getMinutesSinceLastMessage());
System.out.println("Is active: " + status.isActive());
System.out.println("Conversation ID: " + status.getConversationId());
```

### Force Cleanup All Expired
```java
int cleaned = messageProcessingService.cleanupAllExpiredConversations();
System.out.println("Cleaned up " + cleaned + " expired conversations");
```

### Manual Reset
```java
messageProcessingService.resetConversationState("+1234567890");
```

## Benefits for Truck Drivers

### Before (Without Timeout)
```
Driver: "Hello" (Monday 9 AM)
AI: "Hello! How can I help?"

Driver: "I'm back!" (Friday 2 PM - 5 days later!)
AI: "Great! Are you still talking about Monday's delivery?" ❌ CONFUSING
```

### After (With Timeout)
```
Driver: "Hello" (Monday 9 AM)  
AI: "Hello! How can I help?"

[Conversation expires after 30 minutes of inactivity]

Driver: "I'm back!" (Friday 2 PM)
AI: "Hello! Welcome back. How can I assist you today?" ✅ FRESH CONTEXT
```

## Log Examples

### Normal Processing
```
2025-01-18 10:00:15 INFO Processing message from driver: +1234567890 (Frank), Content: 'Hello', Timestamp: 1705564815000
2025-01-18 10:00:15 DEBUG 🆕 New conversation starting for driver: +1234567890
2025-01-18 10:00:15 INFO Generated new conversation ID abc-123-def for driver +1234567890
```

### Timeout & Cleanup
```
2025-01-18 11:05:20 INFO Processing message from driver: +1234567890 (Frank), Content: 'I'm back!', Timestamp: 1705568720000
2025-01-18 11:05:20 INFO 🧹 Conversation expired for driver +1234567890 (inactive for 65 minutes). Resetting conversation.
2025-01-18 11:05:20 INFO Reset conversation for driver +1234567890
2025-01-18 11:05:20 DEBUG ✅ Expired conversation cleaned up for driver: +1234567890
```

### Batch Cleanup
```
2025-01-18 12:00:00 INFO 🧹 Starting cleanup of all expired conversations...
2025-01-18 12:00:00 DEBUG Cleaning up expired conversation for driver: +1234567890
2025-01-18 12:00:00 DEBUG Cleaning up expired conversation for driver: +0987654321
2025-01-18 12:00:00 INFO ✅ Cleanup completed. 2 expired conversations removed.
``` 