#!/bin/bash

# Test script for creating TruKKer conversation in Elasticsearch
echo "Testing TruKKer Conversation Creation API..."

# Set the API endpoint
API_URL="http://localhost:8080/wawrapper/elastic"

# Create conversation with TruKKer structure
echo "Creating conversation..."
curl -X POST "${API_URL}" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer your-token-here" \
  -d @sample-trukker-conversation.json

echo -e "\n\nConversation creation test completed!"

# You can also test with inline JSON:
echo -e "\n\nTesting with inline JSON..."
curl -X POST "${API_URL}" \
  -H "Content-Type: application/json" \
  -d '{
    "sessionId": "test-session-123",
    "tenantId": "IN_TRUKKER_EVA",
    "assistantId": "1116",
    "userInfo": null,
    "conversationId": "CON-TEST-12345",
    "conversationStartTime": null,
    "connectionEstablishTime": null,
    "connectionEstablish": false,
    "channelInfo": null,
    "requestInfo": null,
    "conversation": [
      {
        "correlationId": null,
        "userMessage": null,
        "requestTime": null,
        "responseTime": "2025-07-16T12:04:42.448",
        "systemResponse": [
          {
            "sys_query_time": "2025-07-16T12:04:40",
            "sys_response_time": "2025-07-16T12:04:42",
            "sys_response": "Test system response message",
            "type": "System"
          }
        ]
      }
    ],
    "conversationAnalysis": {
      "languages": ["en"],
      "summary": "Test conversation",
      "intent": "[test]"
    }
  }'

echo -e "\n\nAll tests completed!" 