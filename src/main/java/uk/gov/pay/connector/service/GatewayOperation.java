package uk.gov.pay.connector.service;

public enum GatewayOperation {
    AUTHORISE("auth"),
    CAPTURE("capture"),
    REFUND("refund"),
    CANCEL("cancel");


    private final String description;

    GatewayOperation(String auth) {
        this.description = auth;
    }

    public String getConfigKey() {
        return description;
    }
}
