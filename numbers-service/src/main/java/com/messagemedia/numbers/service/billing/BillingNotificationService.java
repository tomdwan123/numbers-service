/*
 * Copyright (c) Message4U Pty Ltd 2014-2020
 *
 * Except as otherwise permitted by the Copyright Act 1967 (Cth) (as amended from time to time) and/or any other
 * applicable copyright legislation, the material may not be reproduced in any format and in any way whatsoever
 * without the prior written consent of the copyright owner.
 */
package com.messagemedia.numbers.service.billing;

import com.messagemedia.domainmodels.accounts.VendorAccountId;
import com.messagemedia.domainmodels.accounts.VendorId;
import com.messagemedia.framework.logging.Logger;
import com.messagemedia.framework.logging.LoggerFactory;
import com.messagemedia.numbers.repository.BillingRatePlanRepository;
import com.messagemedia.numbers.repository.entities.BillingRatePlanEntity;
import com.messagemedia.numbers.repository.entities.NumberEntity;
import com.messagemedia.numbers.service.client.models.ServiceType;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class BillingNotificationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(BillingNotificationService.class);

    // Can't have blank SSM property values so we default to 'NONE' when no URL supplied
    private static final String NO_SLACK_URL_VALUE = "NONE";

    private static final String ADD_ON_ASSIGNMENT_URL_PART = "/v1/addOn/assignment";
    private static final VendorId SMS_BROADCAST = new VendorId("SMSBroadcast");
    private static final String COUNTRY_CODE_AU = "AU";

    private final BillingRatePlanRepository billingRatePlanRepository;
    private final RestTemplate restTemplate;
    private final String smsBroadcastRatePlanId;
    private final String callableNumberRatePlanId;
    private final Set<VendorAccountId> unbilledAccounts;
    private final String addOnAssignmentUrl;
    private final String slackNotificationUrl;

    @Autowired
    public BillingNotificationService(BillingRatePlanRepository billingRatePlanRepository,
                                      RestTemplate restTemplate,
                                      @Value("${service.add-on-billing-manager.endpoint}") String billingManagerUrl,
                                      @Value("${numbers.service.billingManagement.smsBroadcastRatePlanId}") String smsBroadcastRatePlanId,
                                      @Value("${numbers.service.billingManagement.callableNumberRatePlanId}") String callableNumberRatePlanId,
                                      @Value("#{'${numbers.service.billingManagement.unbilledAccounts}'.split(',')}") Set<String> unbilledAccounts,
                                      @Value("${service.numbers-service.slack-notification.url:NONE}") String slackNotificationUrl) {
        this.billingRatePlanRepository = billingRatePlanRepository;
        this.restTemplate = restTemplate;
        this.smsBroadcastRatePlanId = smsBroadcastRatePlanId;
        this.callableNumberRatePlanId = callableNumberRatePlanId;
        this.unbilledAccounts = unbilledAccounts.stream().map(VendorAccountId::fromColonString).collect(Collectors.toSet());
        this.addOnAssignmentUrl = billingManagerUrl + ADD_ON_ASSIGNMENT_URL_PART;
        this.slackNotificationUrl = slackNotificationUrl;
    }

    @Async
    public void sendAddNotification(VendorAccountId vendorAccountId, NumberEntity number) {
        if (unbilledAccounts.contains(vendorAccountId)) {
            LOGGER.infoWithReason("Not sending billing notification to add rate plan", "Unbilled account");
            return;
        }
        String ratePlanId = getRatePlanId(vendorAccountId, number, "add");
        if (ratePlanId != null) {
            exchange(HttpMethod.POST, ratePlanId, vendorAccountId, "add");
        }
    }

    @Async
    public void sendRemoveNotification(VendorAccountId vendorAccountId, NumberEntity number) {
        if (unbilledAccounts.contains(vendorAccountId)) {
            LOGGER.infoWithReason("Not sending billing notification to remove rate plan", "Unbilled account");
            return;
        }
        String ratePlanId = getRatePlanId(vendorAccountId, number, "remove");
        if (ratePlanId != null) {
            exchange(HttpMethod.DELETE, ratePlanId, vendorAccountId, "remove");
        }
    }

    private String getRatePlanId(VendorAccountId vendorAccountId, NumberEntity number, String action) {
        if (vendorAccountId.getVendorId().equals(SMS_BROADCAST)) {
            return smsBroadcastRatePlanId;
        } else {
            // return hardcoded rate plan for number with CALL capabilities & AU country
            if (Objects.equals(number.getCountry(), COUNTRY_CODE_AU) && number.getCapabilities().contains(ServiceType.CALL)) {
                return callableNumberRatePlanId;
            }

            Optional<BillingRatePlanEntity> ratePlan;

            // return rate plan by classification for number with MMS capabilities & AU country
            if (Objects.equals(number.getCountry(), COUNTRY_CODE_AU) && number.getCapabilities().contains(ServiceType.MMS)) {
                ratePlan =
                        billingRatePlanRepository.findByNumber(number).stream().findFirst();
            } else {
                ratePlan =
                        billingRatePlanRepository.findByNumber(number).stream()
                                .filter(e -> e.getCapabilities().equals(number.getCapabilities())).findFirst();

            }

            if (!ratePlan.isPresent()) {
                LOGGER.warnWithReason("Not sending billing notification", "No rate plan found for number {}", number.getPhoneNumber());
                sendSlackNotification(String.format("No rate plan found for number %s", number.getPhoneNumber()), vendorAccountId, "-", action);
                return null;
            } else {
                return ratePlan.get().getRatePlanId();
            }
        }
    }

    private void exchange(HttpMethod httpMethod, String ratePlanId, VendorAccountId vendorAccountId, String action) {
        try {
            restTemplate.exchange(addOnAssignmentUrl, httpMethod,
                    new HttpEntity<>(new BillingNotificationRequest(ratePlanId), getRequestHeaders(vendorAccountId)), Void.class);
            LOGGER.info("{} successful for rate plan {}", action, ratePlanId);
        } catch (HttpStatusCodeException e) {
            handleStatusCodeException(e, action, vendorAccountId, ratePlanId);
        } catch (RestClientException e) {
            logAndConvertException(e, action);
        }
    }

    private HttpHeaders getRequestHeaders(VendorAccountId vendorAccountId) {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_UTF8_VALUE);
        headers.add("Vendor-Id", vendorAccountId.getVendorId().getVendorId());
        headers.add("Effective-Account-Id", vendorAccountId.getAccountId().getAccountId());
        return headers;
    }

    private void handleStatusCodeException(HttpStatusCodeException exception, String action, VendorAccountId vendorAccountId, String ratePlanId) {
        String errorMessage = String.format("Failed to %s rate plan", action);
        if (exception.getStatusCode().equals(HttpStatus.NOT_FOUND)) {
            LOGGER.warnWithReason(errorMessage, "Billing account not found");
            sendSlackNotification("Billing account not found in Zuora", vendorAccountId, ratePlanId, action);
        } else if (exception.getStatusCode().equals(HttpStatus.BAD_REQUEST)) {
            LOGGER.warnWithReason(errorMessage, "Currency mismatch");
            sendSlackNotification("Currency mismatch in Zuora", vendorAccountId, ratePlanId, action);
        } else if (exception.getStatusCode().equals(HttpStatus.UNPROCESSABLE_ENTITY)) {
            LOGGER.warnWithReason(errorMessage, "Rate plan id does not exist");
            sendSlackNotification("Rate plan id does not exist in Zuora", vendorAccountId, ratePlanId, action);
        } else if (exception.getStatusCode().equals(HttpStatus.INTERNAL_SERVER_ERROR)) {
            String reason = "Unknown billing management internal server error";
            LOGGER.errorWithReason(errorMessage, reason);
            sendSlackNotification(reason, vendorAccountId, ratePlanId, action);
        } else {
            String reason = "Unexpected status code=" + exception.getStatusCode();
            LOGGER.errorWithReason(errorMessage, reason);
            sendSlackNotification(reason, vendorAccountId, ratePlanId, action);
        }
    }

    private void logAndConvertException(Exception exception, String action) {
        LOGGER.errorWithReason(String.format("Failed to %s rate plan", action), exception.getMessage());
    }

    private void sendSlackNotification(String message, VendorAccountId vendorAccountId, String ratePlanId, String action) {
        if (StringUtils.isNotBlank(slackNotificationUrl) && !NO_SLACK_URL_VALUE.equals(slackNotificationUrl)) {
            try {
                restTemplate.postForObject(slackNotificationUrl,
                        new SlackMessage(String.format("Failed to %s rate plan for account.\n"
                                + "Account : %s\n"
                                + "Rate Plan ID : %s\n"
                                + "Reason : %s", action, vendorAccountId.toColonString(), ratePlanId, message)), String.class);
            } catch (Exception e) {
                LOGGER.warnWithReason("Failed to post notification to slack", e.getMessage(), e);
            }
        }
    }
}
