package com.gestudio.crm.messaging;

public record ProviderResult(
    String result, String provider, String externalMessageId, String externalThreadId) {}
