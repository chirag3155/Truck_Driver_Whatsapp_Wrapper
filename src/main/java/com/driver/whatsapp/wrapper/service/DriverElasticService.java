package com.driver.whatsapp.wrapper.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.query_dsl.QueryBuilders;
import co.elastic.clients.elasticsearch.core.IndexRequest;
import co.elastic.clients.elasticsearch.core.IndexResponse;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.driver.whatsapp.wrapper.dto.ConversationMessageDto;
import com.driver.whatsapp.wrapper.dto.ConversationWithMetadataDTO;
import com.driver.whatsapp.wrapper.entity.TransactionDetail;
import com.driver.whatsapp.wrapper.repository.TransactionDetailRepository;
import com.driver.whatsapp.wrapper.repository.TransactionHistoryRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Objects;
import com.driver.whatsapp.wrapper.entity.TransactionHistory;

@Service
@Slf4j
public class DriverElasticService {

    private final Optional<ElasticsearchClient> esClient;
    private final TransactionDetailRepository transactionDetailRepository;
    private final TransactionHistoryRepository transactionHistoryRepository;
    private final ObjectMapper objectMapper;

    @Value("${elasticsearch.index.name}")
    private String indexName;

    @Autowired
    public DriverElasticService(@Lazy Optional<ElasticsearchClient> esClient,
                              TransactionDetailRepository transactionDetailRepository,
                              TransactionHistoryRepository transactionHistoryRepository,
                              ObjectMapper objectMapper) {
        this.esClient = esClient;
        this.transactionDetailRepository = transactionDetailRepository;
        this.transactionHistoryRepository = transactionHistoryRepository;
        this.objectMapper = objectMapper;
    }

    public List<ConversationWithMetadataDTO> getConversationsWithMetadataByTransactionId(String transactionId) throws IOException {
        if (esClient.isEmpty()) {
            log.warn("Elasticsearch client is not available");
            return List.of();
        }

        // Fetch distinct conversation IDs from transaction_history table
        List<String> conversationIds = transactionHistoryRepository
                .findDistinctConversationIdsByTransactionId(transactionId);

        if (conversationIds.isEmpty()) {
            log.warn("No conversationIds found for transaction ID: {}", transactionId);
            return List.of();
        }

        List<ConversationWithMetadataDTO> result = new ArrayList<>();

        for (String conversationId : conversationIds) {
            log.info("Processing conversation ID: {}", conversationId);

            if (conversationId == null) {
                continue;
            }

            List<ConversationMessageDto> messages = getConversationMessages(conversationId);
            Map<String, String> meta = getSummaryAndIntent(conversationId);
            String summary = meta.getOrDefault("summary", null);
            String intent  = meta.getOrDefault("intent", null);

            // Attempt to get additional metadata from transaction_detail table (optional)
            String phoneNumber = null;
            String orderNumber = null;
            String truckNumber = null;
            String communicationMode = null;

            
        // 1. Look up TransactionHistory by conversationId
        Optional<TransactionHistory> historyOpt = transactionHistoryRepository
                .findFirstByConversationIdOrderByTransactionTimestampDesc(conversationId);

        if (historyOpt.isPresent()) {
            TransactionHistory history = historyOpt.get();
            transactionId = history.getTransactionId();
            phoneNumber   = history.getPhoneNumber();
            truckNumber   = history.getTruckNumber();
            communicationMode = history.getCommunicationMode();
        }

        // 2. If we found a transactionId, try to fetch TransactionDetail to get
        //    orderNumber (and override any missing/null values)
        if (transactionId != null) {
            Optional<TransactionDetail> tdOpt = transactionDetailRepository.findByTransactionId(transactionId);
            if (tdOpt.isPresent()) {
                TransactionDetail td = tdOpt.get();
                if (phoneNumber == null) {
                    phoneNumber = td.getPhoneNumber();
                }
                if (truckNumber == null) {
                    truckNumber = td.getTruckNumber();
                }
                orderNumber = td.getOrderNumber();
            }
        }

            if (!messages.isEmpty()) {
                result.add(ConversationWithMetadataDTO.builder()
                        .conversationId(conversationId)
                        .transactionId(transactionId)
                        .phoneNumber(phoneNumber)
                        .orderNumber(orderNumber)
                        .truckNumber(truckNumber)
                        .communicationMode(communicationMode)
                        .summary(summary)
                        .intent(intent)
                        .messages(messages)
                        .build());
            }
        }

        return result;
    }

    public ConversationWithMetadataDTO getConversationWithMetadataByConversationId(String conversationId) throws IOException {
        if (conversationId == null || conversationId.trim().isEmpty()) {
            log.warn("conversationId is null or empty");
            return null;
        }

        // Fetch messages for the conversation
        List<ConversationMessageDto> messages = getConversationMessages(conversationId);

        // Fetch summary & intent if available
        Map<String, String> meta = getSummaryAndIntent(conversationId);
        String summary = meta.getOrDefault("summary", null);
        String intent  = meta.getOrDefault("intent", null);

        // ---------------------------------------------------------------------
        //  Fetch additional metadata using TransactionHistory first, then
        //  enrich using TransactionDetail (based on the transactionId we find)
        // ---------------------------------------------------------------------
        String phoneNumber = null;
        String orderNumber = null;
        String truckNumber = null;
        String transactionId = null;
        String communicationMode = null;

        // 1. Look up TransactionHistory by conversationId
        Optional<TransactionHistory> historyOpt = transactionHistoryRepository
                .findFirstByConversationIdOrderByTransactionTimestampDesc(conversationId);

        if (historyOpt.isPresent()) {
            TransactionHistory history = historyOpt.get();
            transactionId = history.getTransactionId();
            phoneNumber   = history.getPhoneNumber();
            truckNumber   = history.getTruckNumber();
            communicationMode = history.getCommunicationMode();
        }

        // 2. If we found a transactionId, try to fetch TransactionDetail to get
        //    orderNumber (and override any missing/null values)
        if (transactionId != null) {
            Optional<TransactionDetail> tdOpt = transactionDetailRepository.findByTransactionId(transactionId);
            if (tdOpt.isPresent()) {
                TransactionDetail td = tdOpt.get();
                if (phoneNumber == null) {
                    phoneNumber = td.getPhoneNumber();
                }
                if (truckNumber == null) {
                    truckNumber = td.getTruckNumber();
                }
                orderNumber = td.getOrderNumber();
            }
        }

        return ConversationWithMetadataDTO.builder()
                .conversationId(conversationId)
                .transactionId(transactionId)
                .phoneNumber(phoneNumber)
                .orderNumber(orderNumber)
                .truckNumber(truckNumber)
                .communicationMode(communicationMode)
                .summary(summary)
                .intent(intent)
                .messages(messages)
                .build();
    }

    private List<ConversationMessageDto> getConversationMessages(String conversationId) throws IOException {
        log.info("Starting search for conversationId: {}", conversationId);
        log.info("Using index pattern: {}", indexName + "*");
        
        // Build search request with simplified query structure
        SearchRequest searchRequest = new SearchRequest.Builder()
            .index(indexName + "*") // Use wildcard pattern to search multiple indices
            .ignoreUnavailable(true) // Skip indices that don't exist
            .allowNoIndices(true) // Allow the query even if no indices match the pattern
            .size(1000) // Set maximum size to get all results
            .query(QueryBuilders.bool(b -> b
                .must(QueryBuilders.term(t -> t
                    .field("conversationId.keyword")
                    .value(conversationId)
                ))
            ))
            .build();

        log.info("Executing Elasticsearch search query for conversationId: {}", conversationId);
        
        // Execute search
        SearchResponse<JsonNode> searchResponse = esClient.get().search(searchRequest, JsonNode.class);

        log.info("Search completed. Total hits: {}, Took: {}ms", 
                searchResponse.hits().total().value(), 
                searchResponse.took());

        if (searchResponse.hits().total().value() == 0) {
            log.warn("No documents found for conversationId: {}", conversationId);
            return List.of();
        }

        log.info("Processing {} search hits", searchResponse.hits().hits().size());

        // Extract hits and transform to DTOs
        return searchResponse.hits().hits().stream()
            .map(hit -> {
                try {
                    log.info("Processing hit with ID: {} from index: {}", hit.id(), hit.index());
                    JsonNode source = hit.source();
                    
                    if (source == null) {
                        log.info("Hit {} has null source", hit.id());
                        return null;
                    }
                    
                    log.info("Source data for hit {}: {}", hit.id(), source.toString());
                    
                    List<ConversationMessageDto> messages = processConversationNode(source);
                    log.info("Extracted {} messages from hit {}", messages.size(), hit.id());
                    
                    return messages;
                } catch (Exception e) {
                    log.error("Error processing hit {}: {}", hit.id(), e.getMessage(), e);
                    return null;
                }
            })
            .filter(Objects::nonNull)
            .flatMap(List::stream)
            .collect(Collectors.toList());
    }

  private List<ConversationMessageDto> processConversationNode(JsonNode messageNode) {
        List<ConversationMessageDto> messages = new ArrayList<>();
        
        log.info("Processing conversation node. Has 'conversation' field: {}", messageNode.has("conversation"));
        
        JsonNode conversationArray = messageNode.path("conversation");
        
        if (!conversationArray.isArray()) {
            log.warn("Conversation node is not an array or is missing. Node type: {}", conversationArray.getNodeType());
            return messages;
        }
        
        log.info("Found conversation array with {} entries", conversationArray.size());

        // Process messages in chronological order
        for (int i = 0; i < conversationArray.size(); i++) {
            JsonNode conversationEntry = conversationArray.get(i);
            
            try {
                log.info("Processing conversation entry {} of {}", i + 1, conversationArray.size());
                
                String correlationId = conversationEntry.path("correlationId").asText();
                log.info("Entry {}: correlationId = {}", i + 1, correlationId);
                
                // Add user message if exists
                JsonNode userMessage = conversationEntry.path("userMessage");
                if (!userMessage.isMissingNode() && !userMessage.isNull()) {
                    String content = userMessage.path("content").asText();
                    String timestamp = conversationEntry.path("requestTime").asText();
                    
                    log.info("Entry {}: Found user message - content: '{}', timestamp: '{}'", 
                            i + 1, content, timestamp);
                    
                    if (!content.isEmpty()) {
                        ConversationMessageDto userMsg = ConversationMessageDto.builder()
                            .type("Driver")
                            .content(content)
                            .timestamp(normalizeTimestamp(timestamp))
                            .build();
                        messages.add(userMsg);
                        log.info("Added user message: {}, correlationId: {}", content, correlationId);
                    } else {
                        log.info("Entry {}: User message content is empty, skipping", i + 1);
                    }
                } else {
                    log.info("Entry {}: No user message found or userMessage is null", i + 1);
                }
                
                // Add system responses if exist
                JsonNode systemResponses = conversationEntry.path("systemResponse");
                if (systemResponses.isArray()) {
                    log.info("Entry {}: Found system responses array with {} items", 
                            i + 1, systemResponses.size());
                    
                    for (int j = 0; j < systemResponses.size(); j++) {
                        JsonNode response = systemResponses.get(j);
                        if (!response.isNull()) {
                            String content = response.path("sys_response").asText();
                            String timestamp = response.path("sys_response_time").asText();
                            
                            log.info("Entry {}, Response {}: content: '{}', timestamp: '{}'", 
                                    i + 1, j + 1, content.substring(0, Math.min(content.length(), 100)) + "...", timestamp);
                            
                            if (!content.isEmpty()) {
                                ConversationMessageDto systemMsg = ConversationMessageDto.builder()
                                    .type("Agent")
                                    .content(content)
                                    .timestamp(normalizeTimestamp(timestamp))
                                    .build();
                                messages.add(systemMsg);
                                log.info("Added system response: correlationId: {}", correlationId);
                            } else {
                                log.info("Entry {}, Response {}: System response content is empty, skipping", i + 1, j + 1);
                            }
                        } else {
                            log.info("Entry {}, Response {}: System response is null, skipping", i + 1, j + 1);
                        }
                    }
                } else if (!systemResponses.isMissingNode() && systemResponses.isNull()) {
                    log.info("Entry {}: systemResponse is explicitly null", i + 1);
                } else {
                    log.info("Entry {}: No system responses found or not an array. Node type: {}", 
                            i + 1, systemResponses.getNodeType());
                }
            } catch (Exception e) {
                log.error("Error processing conversation entry {}: {}", i + 1, e.getMessage(), e);
            }
        }
        
        log.info("Before sorting: {} messages", messages.size());
        
        // Sort messages by timestamp using the new parsing method
        messages.sort((m1, m2) -> {
            try {
                Instant t1 = parseTimestamp(m1.getTimestamp());
                Instant t2 = parseTimestamp(m2.getTimestamp());
                return t1.compareTo(t2);
            } catch (Exception e) {
                log.error("Failed to parse timestamps for sorting: {} vs {} - Error: {}", 
                    m1.getTimestamp(), m2.getTimestamp(), e.getMessage());
                // Fallback to string comparison if parsing fails
                return m1.getTimestamp().compareTo(m2.getTimestamp());
            }
        });
        
        log.info("Processed and sorted {} messages from conversation", messages.size());
        
        // Log final message order for debugging
        for (int i = 0; i < messages.size(); i++) {
            ConversationMessageDto msg = messages.get(i);
            log.info("Final order {}: {} - {} - {}", 
                    i + 1, msg.getType(), msg.getTimestamp(), 
                    msg.getContent().substring(0, Math.min(msg.getContent().length(), 50)) + "...");
        }
        
        return messages;
    }

    /**
     * Parses a timestamp string into an Instant object, handling various formats
     * @param timestamp The timestamp string to parse
     * @return Instant object representing the timestamp
     */
    private Instant parseTimestamp(String timestamp) {
        if (timestamp == null || timestamp.trim().isEmpty()) {
            return Instant.now(); // Default to current time if timestamp is missing
        }

        try {
            String normalized = ensureUtc(timestamp);
            return Instant.parse(normalized);
        } catch (Exception e) {
            log.warn("Failed to parse timestamp: {} - Error: {}", timestamp, e.getMessage());
            // Return current time as fallback
            return Instant.now();
        }
    }

    /**
     * Ensures the incoming timestamp string ends with an explicit UTC offset so that
     * Instant.parse(..) can consume it. Handles:
     *  - yyyy-MM-ddTHH:mm:ss        -> + ".000Z"
     *  - yyyy-MM-ddTHH:mm:ss.SSS    -> + "Z"
     *  - yyyy-MM-ddTHH:mm:ss+05:00  -> unchanged
     *  - yyyy-MM-ddTHH:mm:ssZ       -> unchanged
     */
    private String ensureUtc(String ts) {
        if (ts == null || ts.isBlank()) {
            return Instant.now().toString();
        }

        // Already has explicit zone (Z or +hh:mm / -hh:mm or +hhmm)
        if (ts.endsWith("Z") || ts.matches(".*[+-]\\d{2}:?\\d{2}$")) {
            // Add colon in offset if missing (e.g., +0500 -> +05:00) because Instant.parse needs colon
            if (ts.matches(".*[+-]\\d{4}$")) {
                return ts.substring(0, ts.length() - 5) + ts.substring(ts.length() - 5, ts.length() - 3) + ":" + ts.substring(ts.length() - 3);
            }
            return ts;
        }

        // Missing timezone but has milliseconds
        if (ts.matches(".*\\.\\d{3}$")) {
            return ts + "Z";
        }

        // Missing both milliseconds and zone
        if (!ts.contains(".")) {
            return ts + ".000Z";
        }

        // Fallback just append Z
        return ts + "Z";
    }
    /**
     * Normalises a timestamp string so that it always ends with an explicit UTC
     * designator and (if necessary) milliseconds. No parsing, just string ops.
     */
    private String normalizeTimestamp(String timestamp) {
        return ensureUtc(timestamp);
    }

    private ConversationMessageDto buildUserMessage(JsonNode conversationEntry, JsonNode rootNode) {
    JsonNode userMessageNode = null;

    if (conversationEntry.has("userMessage") && !conversationEntry.get("userMessage").isNull()) {
        userMessageNode = conversationEntry.get("userMessage");
    } else if (conversationEntry.has("message") && conversationEntry.get("message").has("text")) {
        userMessageNode = conversationEntry.get("message").get("text");
    }

    if (userMessageNode != null) {
        // Only check if "content" is present
        JsonNode contentNode = userMessageNode.get("content");
        if (contentNode != null && !contentNode.isNull() && !contentNode.asText().trim().isEmpty()) {
            return ConversationMessageDto.builder()
                .type("driver")
                .content(contentNode.asText())
                .timestamp(conversationEntry.path("requestTime").asText())
                .build();
        }
    }

    return null;
}


    private ConversationMessageDto buildSystemResponse(JsonNode response, JsonNode conversationEntry, JsonNode rootNode) {
        return ConversationMessageDto.builder()
            .type(response.path("type").asText("System"))
            .content(response.path("sys_response").asText())
            .timestamp(extractTimestamp(conversationEntry, rootNode)) // Fixed: Remove String.valueOf()
            .build();
    }

    private String extractTimestamp(JsonNode conversationEntry, JsonNode rootNode) {
        // Try to get timestamp in order of preference
        if (conversationEntry.has("responseTime")) {
            return conversationEntry.get("responseTime").asText();
        } else if (rootNode.has("@timestamp")) {
            return rootNode.get("@timestamp").asText();
        }
        return Instant.now().toString(); // Return current time as ISO string
    }


    private Double getDoubleValue(JsonNode node, String field) {
        return node.has(field) && !node.get(field).isNull() ? node.get(field).asDouble() : null;
    }

    private Integer getIntegerValue(JsonNode node, String field) {
        return node.has(field) && !node.get(field).isNull() ? node.get(field).asInt() : null;
    }

    private String getStringValue(JsonNode node, String field) {
        return node.has(field) && !node.get(field).isNull() ? node.get(field).asText() : null;
    }

    private Map<String, Object> extractCustomAttributes(JsonNode workflowNode) {
        Map<String, Object> attributes = new HashMap<>();
        if (workflowNode.has("customAttributes") && !workflowNode.get("customAttributes").isNull()) {
            JsonNode customAttrs = workflowNode.get("customAttributes");
            customAttrs.fields().forEachRemaining(entry -> 
                attributes.put(entry.getKey(), parseJsonValue(entry.getValue())));
        }
        return attributes;
    }

    private Object parseJsonValue(JsonNode node) {
        if (node.isTextual()) return node.asText();
        if (node.isNumber()) return node.numberValue();
        if (node.isBoolean()) return node.asBoolean();
        if (node.isNull()) return null;
        return node.toString();
    }

      public JsonNode createConversation(JsonNode conversation) {
        if (esClient.isEmpty()) {
            log.warn("Elasticsearch client is not available");
            throw new RuntimeException("Elasticsearch is not available");
        }

        try {
            if (conversation == null) {
                throw new IllegalArgumentException("Conversation cannot be null");
            }
            
            // Extract details for logging - handle null values gracefully
            String conversationId = "unknown";
            String tenantId = "unknown";
            String assistantId = "unknown";
            String sessionId = "unknown";
            
            if (conversation.has("conversationId") && !conversation.get("conversationId").isNull()) {
                conversationId = conversation.get("conversationId").asText();
            }
            
            if (conversation.has("tenantId") && !conversation.get("tenantId").isNull()) {
                tenantId = conversation.get("tenantId").asText();
            }
            
            if (conversation.has("assistantId") && !conversation.get("assistantId").isNull()) {
                assistantId = conversation.get("assistantId").asText();
            }
            
            if (conversation.has("sessionId") && !conversation.get("sessionId").isNull()) {
                sessionId = conversation.get("sessionId").asText();
            }
            
            // Handle userInfo which can be null
            String userInfoLog = "null";
            JsonNode userInfo = conversation.get("userInfo");
            if (userInfo != null && !userInfo.isNull()) {
                String userName = userInfo.has("name") ? userInfo.get("name").asText() : "N/A";
                String userMobile = userInfo.has("mobile") ? userInfo.get("mobile").asText() : "N/A";
                String userEmail = userInfo.has("email") ? userInfo.get("email").asText() : "N/A";
                userInfoLog = String.format("name=%s, mobile=%s, email=%s", userName, userMobile, userEmail);
            }
            
            log.info("Creating conversation - ConversationId: {}, TenantId: {}, AssistantId: {}, SessionId: {}, UserInfo: [{}]", 
                    conversationId, tenantId, assistantId, sessionId, userInfoLog);

            // Create dynamic index name with today's date (e.g., "complete-conversation-2025.07.16")
            String todayDate = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy.MM.dd"));
            String dynamicIndexName = "complete-conversation-" + todayDate;
            
            log.info("Using dynamic index name: {}", dynamicIndexName);

            // Add metadata to the conversation document
            ObjectMapper objectMapper = new ObjectMapper();
            ObjectNode enrichedConversation = objectMapper.createObjectNode();
            
            enrichedConversation.set("requestInfo", conversation.get("requestInfo"));
            enrichedConversation.put("kafka_topic", "complete_conversation");
            enrichedConversation.put("tenantId", tenantId);
            enrichedConversation.put("conversationId", conversationId);
            enrichedConversation.set("connectionEstablishTime", conversation.get("connectionEstablishTime"));
            enrichedConversation.put("sessionId", sessionId);
            enrichedConversation.set("conversationStartTime", conversation.get("conversationStartTime"));
            enrichedConversation.put("connectionEstablish", conversation.has("connectionEstablish") ? conversation.get("connectionEstablish").asBoolean() : false);
            enrichedConversation.set("conversation", conversation.get("conversation"));
            enrichedConversation.set("conversationAnalysis", conversation.get("conversationAnalysis"));
            enrichedConversation.put("@version", "1");
            enrichedConversation.put("@timestamp", Instant.now().toString());
            enrichedConversation.set("userInfo", conversation.get("userInfo"));
            enrichedConversation.set("conversationEndTime", conversation.get("conversationEndTime"));
            enrichedConversation.put("message", conversation.toString());
            enrichedConversation.put("assistantId", assistantId);
            enrichedConversation.set("channelInfo", conversation.get("channelInfo"));
            enrichedConversation.set("conversationEndInfo", conversation.get("conversationEndInfo"));

            // Create the index request to add the conversation to Elasticsearch
            IndexRequest<JsonNode> indexRequest = new IndexRequest.Builder<JsonNode>()
                    .index(dynamicIndexName)
                    .document(enrichedConversation)
                    .build();

            // Execute the indexing request
            IndexResponse response = esClient.get().index(indexRequest);

            // Log response and return the conversation if successful
            log.info("Conversation created successfully with ID: {} in index: {}", response.id(), response.index());
            
            // Return the enriched conversation with Elasticsearch metadata
            ObjectNode responseNode = objectMapper.createObjectNode();
            responseNode.put("_index", response.index());
            responseNode.put("_id", response.id());
            responseNode.put("_version", response.version());
            responseNode.put("result", response.result().toString());
            responseNode.set("_source", enrichedConversation);
            
            return responseNode;

        } catch (IOException e) {
            log.error("Error creating conversation: {}", e.getMessage());
            throw new RuntimeException("Failed to create conversation: " + e.getMessage());
        }
    }

    /**
     * Fetches summary and intent fields from conversationAnalysis for a given conversationId.
     * Lightweight: size=1 query.
     */
    private Map<String, String> getSummaryAndIntent(String conversationId) throws IOException {
        Map<String, String> meta = new HashMap<>();
        if (esClient.isEmpty()) return meta;

        SearchRequest req = new SearchRequest.Builder()
            .index(indexName + "*")
            .ignoreUnavailable(true)
            .allowNoIndices(true)
            .size(1)
            .query(QueryBuilders.term(t -> t.field("conversationId.keyword").value(conversationId)))
            .build();

        SearchResponse<JsonNode> resp = esClient.get().search(req, JsonNode.class);
        if (resp.hits().total() == null || resp.hits().total().value() == 0) return meta;

        JsonNode source = resp.hits().hits().get(0).source();
        if (source == null) return meta;
        JsonNode analysis = source.path("conversationAnalysis");
        if (!analysis.isMissingNode()) {
            if (analysis.has("summary")) meta.put("summary", analysis.get("summary").asText(null));
            if (analysis.has("intent")) meta.put("intent", analysis.get("intent").asText(null));
        }
        return meta;
    }
} 