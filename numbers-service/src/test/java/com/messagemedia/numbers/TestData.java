/*
 * Copyright (c) Message4U Pty Ltd 2014-2018
 *
 * Except as otherwise permitted by the Copyright Act 1967 (Cth) (as amended from time to time) and/or any other
 * applicable copyright legislation, the material may not be reproduced in any format and in any way whatsoever
 * without the prior written consent of the copyright owner.
 */

package com.messagemedia.numbers;

import com.google.common.collect.ImmutableList;
import com.messagemedia.domainmodels.accounts.VendorAccountId;
import com.messagemedia.framework.jackson.core.valuewithnull.ValueWithNull;
import com.messagemedia.numbers.model.dto.CallableNumberDto;
import com.messagemedia.numbers.model.dto.CallableNumberRoutingProfile;
import com.messagemedia.numbers.model.dto.CallableNumberRoutingProfileStep;
import com.messagemedia.numbers.model.dto.CallableNumberRoutingProfileStepDetail;
import com.messagemedia.numbers.repository.entities.AssignmentEntity;
import com.messagemedia.numbers.repository.entities.BillingRatePlanEntity;
import com.messagemedia.numbers.repository.entities.NumberEntity;
import com.messagemedia.numbers.repository.entities.builders.AssignmentEntityBuilder;
import com.messagemedia.numbers.service.client.models.*;

import java.io.InputStream;
import java.time.OffsetDateTime;
import java.util.*;
import org.apache.commons.io.IOUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.messagemedia.numbers.repository.entities.builders.AssignmentEntityBuilder.anAssignmentEntity;
import static com.messagemedia.numbers.repository.entities.builders.NumberEntityBuilder.aNumberEntity;
import static com.messagemedia.numbers.service.client.models.AssignmentDto.AssignmentDtoBuilder.anAssignmentDto;
import static com.messagemedia.numbers.service.client.models.NumberDto.NumberDtoBuilder.aNumberDto;
import static com.messagemedia.numbers.service.client.models.validator.MetadataValidator.*;
import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.apache.commons.lang3.RandomStringUtils.randomNumeric;
import static org.apache.commons.lang3.RandomUtils.nextInt;
import static org.springframework.http.MediaType.APPLICATION_JSON_UTF8_VALUE;

public class TestData {

    public static final String WEB_CONTAINER_PORT = "web.container.port";
    public static final String ASSIGNMENT_URL_FORMAT = "/v1/numbers/%s/assignment";
    public static final String GET_NUMBER_URL_FORMAT = "/v1/numbers/%s";
    public static final String FORWARD_URL_FORMAT = "/v1/numbers/%s/forward";

    private static final List<String> VALID_ISO_COUNTRY_CODES = ImmutableList.copyOf(Locale.getISOCountries());
    private static Random random = new Random();

    public static NumberEntity randomUnassignedNumberEntity() {
        return aNumberEntity()
                .withClassification(randomEnum(Classification.class))
                .withCountry(randomCountryCode())
                .withPhoneNumber(randomPhoneNumber())
                .withProviderId(UUID.randomUUID())
                .withType(randomNumberType())
                .withCapabilities(randomCapabilities())
                .withDedicatedReceiver(randomDedicatedReceiver())
                .build();
    }

    public static NumberEntity randomUnassignedNumberEntityWithCapabilities(Set<ServiceType> capabilities) {
        return aNumberEntity()
                .withClassification(randomEnum(Classification.class))
                .withCountry(randomCountryCode())
                .withPhoneNumber(randomPhoneNumber())
                .withProviderId(UUID.randomUUID())
                .withType(randomNumberType())
                .withCapabilities(capabilities)
                .withDedicatedReceiver(randomDedicatedReceiver())
                .build();
    }

    public static NumberEntity randomAssignedNumberEntity() {
        NumberEntity numberEntity = randomUnassignedNumberEntity();
        AssignmentEntity assignmentEntity = randomAssignmentEntityWithoutNumberEntity();
        numberEntity.setAssignedTo(assignmentEntity);
        assignmentEntity.setNumberEntity(numberEntity);
        return numberEntity;
    }

    public static NumberEntity randomAssignedNumberEntity(VendorAccountId vendorAccountId) {
        NumberEntity numberEntity = randomUnassignedNumberEntity();
        AssignmentEntity assignmentEntity = randomAssignmentEntityWithoutNumberEntity();
        assignmentEntity.setVendorId(vendorAccountId.getVendorId().getVendorId());
        assignmentEntity.setAccountId(vendorAccountId.getAccountId().getAccountId());
        numberEntity.setAssignedTo(assignmentEntity);
        assignmentEntity.setNumberEntity(numberEntity);
        return numberEntity;
    }

    public static AssignmentEntity randomAssignmentEntity() {
        AssignmentEntity assignmentEntity = randomAssignmentEntityWithoutNumberEntity();
        assignmentEntity.setNumberEntity(randomUnassignedNumberEntity());
        return assignmentEntity;
    }

    public static AssignmentEntity randomAssignmentEntityWithoutNumberEntity() {
        return anAssignmentEntity()
                .withAccountId(randomAlphanumeric(25))
                .withVendorId(randomAlphanumeric(25))
                .withCallbackUrl(randomCallbackUrl())
                .withDeleted(null)
                .withExternalMetadata(randomHashMap())
                .withLabel(randomValidLabel())
                .build();
    }

    public static BillingRatePlanEntity randomBillingRatePlanEntity() {
        return randomBillingRatePlanEntity(randomAlphanumeric(25), randomCountryCode());
    }

    public static BillingRatePlanEntity randomBillingRatePlanEntity(String ratePlanId, String country) {
        BillingRatePlanEntity billingRatePlanEntity = new BillingRatePlanEntity();
        billingRatePlanEntity.setId(UUID.randomUUID());
        billingRatePlanEntity.setCapabilities(randomCapabilities());
        billingRatePlanEntity.setClassification(randomClassification());
        billingRatePlanEntity.setCountry(country);
        billingRatePlanEntity.setType(randomNumberType());
        billingRatePlanEntity.setRatePlanId(ratePlanId);
        return billingRatePlanEntity;
    }

    public static AssignNumberRequest randomAssignNumberRequest() {
        return new AssignNumberRequest(randomVendor(), randomAccount(), randomCallbackUrl(), randomHashMap(),
                randomValidLabel());
    }

    public static RegisterNumberRequest randomRegisterNumberRequest(Classification classification) {
        return new RegisterNumberRequest(randomPhoneNumber(), UUID.randomUUID(), "AU",
                NumberType.MOBILE, classification, randomCapabilities(), randomDedicatedReceiver());
    }

    public static RegisterNumberRequest randomRegisterNumberRequest(Classification classification,
                                                                    Set<ServiceType> capabilities) {
        return new RegisterNumberRequest(randomPhoneNumber(), UUID.randomUUID(), "AU",
                NumberType.MOBILE, classification, capabilities, randomDedicatedReceiver());
    }

    public static RegisterNumberRequest randomRegisterNumberRequest() {
        return randomRegisterNumberRequest(randomClassification());
    }

    public static RegisterNumberRequest randomRegisterUsTollFreeNumberRequest() {
        return new RegisterNumberRequest(randomTollFreeNumber(), UUID.randomUUID(), "US",
                NumberType.TOLL_FREE, randomEnum(Classification.class), randomCapabilities(), randomDedicatedReceiver());
    }

    public static UpdateNumberRequest randomUpdateNumberRequestWithAvailableAfter() {
        return updateNumberRequest(randomClassification(), randomCapabilities(),
                ValueWithNull.of(OffsetDateTime.now()), randomDedicatedReceiver(), null, null);
    }

    public static UpdateNumberRequest randomUpdateNumberRequestWithoutAvailableAfter() {
        return updateNumberRequest(randomClassification(), randomCapabilities(), null, randomDedicatedReceiver(), null, null);
    }

    public static UpdateNumberRequest randomUpdateNumberRequestWithOnlyStatus() {
        return updateNumberRequest(null, null, null, null, randomStatus(), null);
    }

    public static UpdateNumberRequest updateNumberRequest(Classification classification, Set<ServiceType> capabilities,
                                                          ValueWithNull<OffsetDateTime> availableAfter, Boolean dedicatedReceiver,
                                                          Status status, UUID providerId) {
        return new UpdateNumberRequest(classification, capabilities, availableAfter, dedicatedReceiver, status, providerId);
    }

    public static UpdateAssignmentRequest randomUpdateAssignmentRequest() {
        return new UpdateAssignmentRequest(
                ValueWithNull.of(randomCallbackUrl()),
                ValueWithNull.of(randomHashMap()),
                ValueWithNull.of(randomValidLabel()));
    }

    public static NumberDto.NumberDtoBuilder randomUnassignedNumberDtoBuilder() {
        return aNumberDto()
                .withClassification(randomEnum(Classification.class))
                .withCountry(randomCountryCode())
                .withPhoneNumber(randomPhoneNumber())
                .withProviderId(UUID.randomUUID())
                .withType(randomEnum(NumberType.class))
                .withCreated(OffsetDateTime.now())
                .withUpdated(OffsetDateTime.now())
                .withAvailableAfter(OffsetDateTime.now())
                .withCapabilities(randomCapabilities());
    }

    public static NumberDto randomUnassignedNumberDto() {
        return randomUnassignedNumberDtoBuilder()
                .withId(UUID.randomUUID())
                .build();
    }

    public static AssignmentDto.AssignmentDtoBuilder randomAssignmentDtoBuilder() {
        return randomAssignmentDtoBuilderWithMinimumFields()
                .withCallbackUrl(randomAlphabetic(200))
                .withMetadata(randomHashMap());
    }

    public static AssignmentDto.AssignmentDtoBuilder randomAssignmentDtoBuilderWithMinimumFields() {
        return anAssignmentDto()
                .withAccountId(randomAlphabetic(20))
                .withVendorId(randomAlphabetic(20))
                .withCreated(OffsetDateTime.now())
                .withNumberId(UUID.randomUUID());
    }

    public static AssignmentDto randomAssignNumberDto() {
        return randomAssignmentDtoBuilder().withId(UUID.randomUUID()).build();
    }

    public static EventNotification randomEventNotification() {
        return new EventNotification(randomEnum(Event.class), randomUnassignedNumberDto(), randomAssignNumberDto());
    }

    public static Set<ServiceType> randomCapabilities() {
        Set<ServiceType> serviceTypeSet = new HashSet<>();
        for (int i = 0; i < nextInt(1, ServiceType.values().length); i++) {
            serviceTypeSet.add(randomEnum(ServiceType.class));
        }
        return serviceTypeSet;
    }

    public static String randomPhoneNumber() {
        return "+61" + randomNumeric(11);
    }

    public static String randomTollFreeNumber() {
        return "+1800" + randomNumeric(7);
    }

    public static String randomCountryCode() {
        return VALID_ISO_COUNTRY_CODES.get(random.nextInt(VALID_ISO_COUNTRY_CODES.size()));
    }

    public static NumberType randomNumberType() {
        return randomEnum(NumberType.class);
    }

    public static Classification randomClassification() {
        return randomEnum(Classification.class);
    }

    public static <T extends Enum<?>> T randomEnum(Class<T> clazz) {
        return clazz.getEnumConstants()[random.nextInt(clazz.getEnumConstants().length)];
    }

    public static Map<String, String> randomHashMap() {
        return randomHashMap(random.nextInt(MAX_SIZE));
    }

    public static Map<String, String> randomHashMap(int size) {
        Map<String, String> map = new HashMap<>();
        for (int i = 0; i < size; i++) {
            map.put(randomAlphanumeric(MAX_KEY_LENGTH), randomAlphanumeric(MAX_VALUE_LENGTH));
        }
        return map;
    }

    public static Map<String, String> randomInvalidKeyMap() {
        Map<String, String> invalidKeyMap = randomHashMap(MAX_SIZE - 1);
        invalidKeyMap.put(randomAlphanumeric(MAX_KEY_LENGTH + 1), randomAlphanumeric(MAX_VALUE_LENGTH));
        return invalidKeyMap;
    }

    public static Map<String, String> randomInvalidValueMap() {
        Map<String, String> invalidValueMap = randomHashMap(MAX_SIZE - 1);
        invalidValueMap.put(randomAlphanumeric(MAX_KEY_LENGTH), randomAlphanumeric(MAX_VALUE_LENGTH + 1));
        return invalidValueMap;
    }

    public static Map<String, String> randomInvalidSizeMap() {
        return randomHashMap(MAX_SIZE + 1);
    }

    public static Map<String, String> randomEmptyKeyMap() {
        Map<String, String> emptyKeyMap = randomHashMap(MAX_SIZE - 1);
        emptyKeyMap.put("   ", randomAlphanumeric(MAX_VALUE_LENGTH));
        return emptyKeyMap;
    }

    public static String randomVendor() {
        return randomAlphanumeric(25);
    }

    public static String randomAccount() {
        return randomAlphanumeric(50);
    }

    public static VendorAccountId randomVendorAccountId() {
        return new VendorAccountId(randomVendor(), randomAccount());
    }

    public static String randomCallbackUrl() {
        String method = Arrays.asList("http", "https").get(new Random().nextInt(2));
        return String.format("%s://%s.%s ", method, randomAlphanumeric(100), randomAlphanumeric(10));
    }

    public static String randomValidLabel() {
        return randomAlphanumeric(new Random().nextInt(101));
    }

    public static String randomInvalidLabel() {
        return randomAlphanumeric(new Random().nextInt(1000) + 101);
    }

    public static Boolean randomDedicatedReceiver() {
        int randomInt = random.nextInt(3);
        switch (randomInt) {
            case 0:
                return TRUE;
            case 1:
                return FALSE;
            case 2:
            default:
                return null;
        }
    }

    public static Status randomStatus() {
        return randomEnum(Status.class);
    }

    public static UUID randomProviderId() {
        return UUID.randomUUID();
    }

    public static String loadJson(String path) throws Exception {
        try (InputStream resourceAsStream = TestData.class.getClassLoader().getResourceAsStream(path)) {
            return IOUtils.toString(resourceAsStream, "UTF-8");
        }
    }

    public static void mockAmsGetAccount(String filePath, String accountName) throws Exception {
        stubFor(get(urlPathEqualTo(String.format("/v1/api/accounts/%s", accountName)))
                .withQueryParam("effectiveFeatures", equalTo(String.valueOf(false)))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_UTF8_VALUE)
                        .withBody(loadJson(filePath))));
    }

    public static AssignmentEntity createAssignmentEntityfromAccount(VendorAccountId vendorAccountId) {
        return AssignmentEntityBuilder.anAssignmentEntity()
                .withAccountId(vendorAccountId.getAccountId().getAccountId())
                .withVendorId(vendorAccountId.getVendorId().getVendorId())
                .withCallbackUrl(randomCallbackUrl())
                .withExternalMetadata(randomHashMap()).build();
    }

    public static AssignNumberRequest createAssignmentRequestFromAccount(VendorAccountId vendorAccountId) {
        return createAssignmentRequestFromAccount(vendorAccountId, randomValidLabel());
    }

    public static AssignNumberRequest createAssignmentRequestFromAccount(VendorAccountId vendorAccountId, String label) {
        return new AssignNumberRequest(vendorAccountId.getVendorId().getVendorId(),
                vendorAccountId.getAccountId().getAccountId(),
                randomCallbackUrl(),
                randomHashMap(),
                label);
    }

    public static void mockUpAmsEndpoint() throws Exception {
        mockAmsGetAccount("ams/account_root.json", "MessageMedia");
        mockAmsGetAccount("ams/account_m1.json", "AccountM1");
        mockAmsGetAccount("ams/account_m2.json", "AccountM2");
        mockAmsGetAccount("ams/account_m1_1.json", "AccountM11");
        mockAmsGetAccount("ams/account_m1_2.json", "AccountM12");
        mockAmsGetAccount("ams/account_m1_2_1.json", "AccountM121");
        mockAmsGetAccount("ams/account_m1_2_2.json", "AccountM122");
        mockAmsGetAccount("ams/account_m2.json", "AccountM2");
        mockAmsGetAccount("ams/account_c1.json", "AccountC1");
        mockAmsGetAccount("ams/account_m3.json", "AccountM3");
        mockAmsGetAccount("ams/account_m3_1.json", "AccountM31");
        mockAmsGetAccount("ams/account_m3_1_1.json", "AccountM311");
        mockAmsGetAccount("ams/account_m3_1_2.json", "AccountM312");
        mockAmsGetAccount("ams/account_m4.json", "AccountM4");
    }

    public static AssignmentAuditSearchRequest randomAssignmentAuditSearchRequest() {
        return AssignmentAuditSearchRequest.AssignmentAuditSearchRequestBuilder.anAssignmentAuditSearchRequest()
                .withId(UUID.randomUUID())
                .withCreatedAfter(OffsetDateTime.now())
                .withCreatedBefore(OffsetDateTime.now())
                .withDeletedAfter(OffsetDateTime.now())
                .withDeletedBefore(OffsetDateTime.now())
                .withNumberId(UUID.randomUUID())
                .withVendorAccountId(randomVendorAccountId())
                .withToken(randomAuditToken())
                .build();
    }

    public static AuditToken randomAuditToken() {
        return new AuditToken(UUID.randomUUID(), random.nextInt(1000));
    }

    public static AuditPageMetadata randomAuditPageMetadata() {
        return new AuditPageMetadata(random.nextInt(100), randomAuditToken());
    }

    public static CallableNumberDto randomCallableNumberDto() {
        return new CallableNumberDto(UUID.randomUUID().toString(),
                randomAlphabetic(10),
                randomNumeric(10),
                randomCallableNumberRoutingProfile());
    }

    public static CallableNumberRoutingProfile randomCallableNumberRoutingProfile() {
        return new CallableNumberRoutingProfile(UUID.randomUUID().toString(),
                randomAlphabetic(10),
                ImmutableList.of(randomCallableNumberRoutingProfileStep()));
    }

    public static CallableNumberRoutingProfileStep randomCallableNumberRoutingProfileStep() {
        return new CallableNumberRoutingProfileStep(
                randomAlphabetic(10),
                randomCallableNumberRoutingProfileStepDetail()
        );
    }

    public static CallableNumberRoutingProfileStepDetail randomCallableNumberRoutingProfileStepDetail() {
        return new CallableNumberRoutingProfileStepDetail(randomNumeric(10));
    }

    public static NumberForwardDto randomForwardNumberResponseDto() {
        return new NumberForwardDto(randomNumeric(10));
    }

    public static NumberEntity randomUnassignedNumberEntityWithCallCapability() {
        NumberEntity numberEntity = randomUnassignedNumberEntity();
        numberEntity.setCapabilities(new HashSet<>(ImmutableList.of(ServiceType.CALL)));
        return numberEntity;
    }

    public static NumberEntity unassignedUsTollFreeNumberEntity() {
        return aNumberEntity()
                .withClassification(randomEnum(Classification.class))
                .withCountry("US")
                .withPhoneNumber(randomTollFreeNumber())
                .withProviderId(UUID.randomUUID())
                .withType(NumberType.TOLL_FREE)
                .withCapabilities(randomCapabilities())
                .withDedicatedReceiver(randomDedicatedReceiver())
                .build();
    }

    public static NumberEntity assignedUsTollFreeNumberEntity() {
        NumberEntity numberEntity = unassignedUsTollFreeNumberEntity();
        AssignmentEntity assignmentEntity = randomAssignmentEntityWithoutNumberEntity();
        numberEntity.setAssignedTo(assignmentEntity);
        assignmentEntity.setNumberEntity(numberEntity);
        return numberEntity;
    }
}
