package com.driver.whatsapp.wrapper.service;

import com.driver.whatsapp.wrapper.entity.TransactionDetail;
import com.driver.whatsapp.wrapper.model.FlowType;
import com.driver.whatsapp.wrapper.repository.TransactionDetailRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.annotation.PostConstruct;

@Service
@Slf4j
public class TransactionPollingScheduler {

    @Autowired
    private TruKKerService truKKerService;

    @Value("${scheduler.poll.minutes:5}")
    private int pollMinutes;

    @Value("${scheduler.target-api-url:http://localhost:8080/target-api}")
    private String targetApiUrl;

    @Value("${scheduler.disallowed.modes:}")
    private String disallowedModesProp;

    @Value("${sip.url}")
    private String sipUrl;

    @Value("${sip.a-party}")
    private String sipAParty;

    @Value("${trukker.api.otr.url}")
    private String trukkerOtrUrl;

    @Value("${trukker.api.loading-confirmation.url}")
    private String trukkerLoadingConfirmationUrl;

    @Value("${trukker.api.generic-connect.url}")
    private String trukkerGenericConnectUrl;

    @Value("${trukker.api.doc-reminder.url}")
    private String trukkerDocReminderUrl;

    @Autowired
    private TransactionDetailRepository transactionDetailRepository;

    @Autowired
    private RestTemplate restTemplate;

    private List<String> disallowedModes;

    @PostConstruct
    public void initAllowedModes() {
        if (disallowedModesProp != null && !disallowedModesProp.isBlank()) {
            disallowedModes = Arrays.asList(disallowedModesProp.split(","));
        } else {
            disallowedModes = List.of();
        }
    }

    // Use static methods from ConfigurationCacheService
    @Scheduled(fixedDelayString = "${scheduler.poll.fixed-delay-ms:60000}", initialDelay = 10000)
    public void pollOldTransactions() {
        log.info("[Scheduler] Polling for transactions older than {} minutes...", pollMinutes);
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(pollMinutes);
        List<TransactionDetail> oldTransactions = transactionDetailRepository.findTransactionsOlderThan(cutoff);
        log.info("[Scheduler] Found {} transactions to process.", oldTransactions.size());

        for (TransactionDetail tx : oldTransactions) {
            try {
                String apiName = tx.getFlowName(); // Use flow_name column as apiName
                String currentMode = tx.getCommunicationMode();
                
                if(tx.getStatusCode().equalsIgnoreCase("Call_Intiaited")){
                    tx.setStatusCode("no_answer");
                    tx.setState("Close");
                    tx.setCommunicationMode("human");
    
                    // Flow-based routing to TruKKerService
                    String flowName = tx.getFlowName();
    
                    if (flowName != null) {
                        try {
                            FlowType flowType = FlowType.fromValue(flowName);
                            switch (flowType) {
                                case OTR:
                                    /** CaptureBotResponseForOTR  */
                                    truKKerService.updateTrukkerForHumanEscalation(trukkerOtrUrl, tx);
                                    
                                    break;
    
                                case LOADING_CONFIRMATION:
                                    /** CaptureBotResponseForLoadingConfirmation */
                                    truKKerService.updateTrukkerForHumanEscalation(trukkerLoadingConfirmationUrl, tx);
    
                                    break;
                                case ORDER_COMPLETION:
                                    /** CaptureBotResponseForDocReminder */
                                    truKKerService.updateTrukkerForHumanEscalation(trukkerDocReminderUrl, tx);
    
                                    break;
    
                                case STATUS_FOLLOW_UP:
                                    /** CaptureBotResponseForGenericConnect */
                                    truKKerService.updateTrukkerForHumanEscalation(trukkerGenericConnectUrl, tx);
                                    break;
    
                                default:
                                    log.info("Not a valid flow type as if now.");
                                    break;
                            }
                        } catch (IllegalArgumentException e) {
                            log.warn("Unknown flow type: {}. Skipping TruKKerService call.", flowName);
                        }
                    }
                }else{

                    String nextMode = ConfigurationCacheService.getNextCommunicationMode(apiName, currentMode);
                    String assistantId = ConfigurationCacheService.getAssistantId(apiName, nextMode);
                    String tenantId = ConfigurationCacheService.getTenantId(apiName, nextMode);
                    
                    log.info("[Scheduler] SIP URL: {}", sipUrl);
    
                    if (nextMode == null || assistantId == null || tenantId == null) {
                        log.warn("[Scheduler] Missing mapping for next communication mode or assistant/tenant for transaction {}", tx.getTransactionId());
                        continue;
                    }
    
                    // Skip if nextMode is in disallowed modes (case-insensitive)
                    boolean isDisallowed = disallowedModes.stream().anyMatch(mode -> mode.equalsIgnoreCase(currentMode));
                    if (isDisallowed) {
                        log.info("[Scheduler] Skipping transaction {}: next communication mode '{}' is in disallowed modes {}", tx.getTransactionId(), nextMode, disallowedModes);
                        continue;
                    }
    
                    Map<String, Object> request = new HashMap<>();
                    request.put("aParty", sipAParty);
                    request.put("sipUri", sipUrl.replace("phoneNumber", tx.getPhoneNumber()));
                    request.put("assistantId", Integer.parseInt(assistantId));
                    request.put("tenantId", tenantId);
                    request.put("transactionId", tx.getTransactionId());
                    
                    log.info("[Scheduler] Sending transaction {} to next communication mode {} with assistant {} and tenant {}", tx.getTransactionId(), nextMode, assistantId, tenantId);
                    ResponseEntity<Void> response = restTemplate.postForEntity(targetApiUrl, request, Void.class);
    
                    if (response.getStatusCode().is2xxSuccessful()) {
                        // Update communication mode and status in the database
                        tx.setCommunicationMode(nextMode);
                        tx.setStatusCode("Call_Intiaited");
                        transactionDetailRepository.save(tx);
                        log.info("[Scheduler] Updated transaction {}: communicationMode={}, statusCode=Call_Intiaited", tx.getTransactionId(), nextMode);
                    } else {
                        log.warn("[Scheduler] Received non-2xx response for transaction {}: {}", tx.getTransactionId(), response.getStatusCode());
                    }

                }
            } catch (Exception e) {
                log.error("[Scheduler] Error processing transaction {}: {}", tx.getTransactionId(), e.getMessage(), e);
            }
        }
        log.info("[Scheduler] Polling cycle complete.");
    }
} 