# Logging Configuration

The Truck-Driver-WhatsApp-Wrapper application uses a simplified logging setup with a single logs folder containing all application logs.

## Log Files Structure

The logs are stored in the `logs/` directory with this simple structure:

```
logs/
├── application.log                    # All application logs (main log file)
├── errors.log                        # Error-only logs for quick troubleshooting
├── application.2024-01-15.1.log     # Rotated log files (auto-generated)
├── application.2024-01-16.1.log
├── errors.2024-01-15.1.log
└── errors.2024-01-16.1.log
```

## Log Configuration Details

### Main Log File (`application.log`)
- **Contains**: All logs from all components (WhatsApp service, TruKKer service, controllers, message processing, etc.)
- **Size limit**: 50MB per file
- **Rotation**: Daily rotation with size-based backup
- **Retention**: 30 days
- **Total size cap**: 1GB

### Error Log File (`errors.log`)
- **Contains**: Only ERROR level logs for quick issue identification
- **Size limit**: 10MB per file
- **Retention**: 60 days (longer retention for error tracking)
- **Total size cap**: 200MB

### Log Format
All logs use this format for easy parsing:
```
yyyy-MM-dd HH:mm:ss.SSS [thread] LEVEL [logger.class.name] - message
```

Example:
```
2024-01-15 14:30:25.123 [http-nio-8025-exec-1] INFO [c.d.w.w.service.WhatsAppService] - Sending WhatsApp message to 971526328601
2024-01-15 14:30:25.456 [http-nio-8025-exec-1] ERROR [c.d.w.w.controller.DriverController] - Failed to process driver update request
```

## Monitoring and Troubleshooting

### Real-time Log Monitoring

```bash
# Monitor all application activity
tail -f logs/application.log

# Monitor only errors
tail -f logs/errors.log

# Monitor with filtering
tail -f logs/application.log | grep -i "whatsapp"
tail -f logs/application.log | grep -i "trukker"
tail -f logs/application.log | grep -i "error"
```

### Searching Logs

```bash
# Search for specific patterns in current log
grep "WhatsApp" logs/application.log
grep "TruKKer" logs/application.log
grep "ERROR" logs/application.log

# Search across all log files (including rotated ones)
grep "search_pattern" logs/*.log

# Search for specific time periods
grep "2024-01-15 14:" logs/application.log

# Search for specific components
grep "WhatsAppService" logs/application.log
grep "DriverController" logs/application.log
grep "MessageProcessingService" logs/application.log
```

### Log Analysis Examples

```bash
# Count errors by type
grep "ERROR" logs/application.log | cut -d']' -f3 | sort | uniq -c

# Find all WhatsApp API calls
grep "WhatsAppService" logs/application.log | grep -i "api"

# Monitor API response times
grep "response time" logs/application.log

# Track specific phone numbers
grep "971526328601" logs/application.log
```

## Component Identification

Since all logs are in one file, you can identify different components by their logger names:

| Logger Pattern | Component |
|----------------|-----------|
| `WhatsAppService` | WhatsApp API interactions |
| `TruKKerService` | TruKKer API interactions |
| `MessageProcessingService` | Message handling and processing |
| `DriverController` | Driver-related API endpoints |
| `WhatsAppWebhookController` | WhatsApp webhook handling |
| `springframework.web` | Spring framework web operations |

## Configuration Files

- **logback-spring.xml**: Main logging configuration
- **application.properties**: Basic logging documentation

## Customizing Log Levels

You can adjust log levels by modifying the loggers in `logback-spring.xml`:

```xml
<!-- Change to DEBUG for more detailed WhatsApp service logs -->
<logger name="com.driver.whatsapp.wrapper.service.WhatsAppService" level="DEBUG"/>

<!-- Change to WARN to reduce Spring framework verbosity -->
<logger name="org.springframework.web" level="WARN"/>
```

Or add these properties to `application.properties`:
```properties
logging.level.com.driver.whatsapp.wrapper.service.WhatsAppService=DEBUG
logging.level.com.driver.whatsapp.wrapper.controller=DEBUG
logging.level.org.springframework.web=WARN
```

## Benefits of Single Log File Setup

1. **Simplicity**: All logs in one place, easy to monitor and manage
2. **Chronological Order**: See the complete flow of operations across all components
3. **Easy Correlation**: Track requests across multiple services in sequence
4. **Reduced Complexity**: No need to check multiple files for troubleshooting
5. **Better for Small Applications**: Perfect for applications with moderate log volume
6. **Container Friendly**: Easier to collect logs in containerized environments

## Production Considerations

- Monitor the `logs/` directory size regularly
- Consider log aggregation tools (ELK stack, Fluentd) for production environments
- Set up log rotation monitoring to ensure old logs are properly cleaned up
- Use the error log for alerting and monitoring systems

## Troubleshooting Tips

1. **High Log Volume**: Increase log level to WARN or ERROR in production
2. **Missing Logs**: Check directory permissions and available disk space
3. **Large Files**: Log rotation will handle this automatically, but monitor disk usage
4. **Performance Impact**: Logging is asynchronous, but excessive DEBUG logging can impact performance 