package uk.gov.pay.connector.model.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableMap;

import java.util.Map;

public class ServiceAccount {

    private Long id;
    private String gatewayName;
    private Map<String, String> credentials;

    public ServiceAccount(Long id, String gatewayName, Map<String, String> credentials) {
        this.id = id;
        this.gatewayName = gatewayName;
        this.credentials = credentials;
    }

    @JsonProperty("gateway_account_id")
    public Long getId() {
        return id;
    }

    @JsonProperty("payment_provider")
    public String getGatewayName() {
        return gatewayName;
    }

    public Map<String, String> getCredentials() {
        return credentials;
    }

    public Map<String, String> withoutCredentials() {
        return ImmutableMap.of(
                "gateway_account_id", String.valueOf(id),
                "payment_provider", gatewayName);
    }
}
