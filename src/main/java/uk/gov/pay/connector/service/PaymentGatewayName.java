package uk.gov.pay.connector.service;

public enum PaymentGatewayName {
    SANDBOX("sandbox"), SMARTPAY("smartpay"), WORLDPAY("worldpay"), EPDQ("epdq");

    private final String gatewayName;

    PaymentGatewayName(String gatewayName) {
        this.gatewayName = gatewayName;
    }

    public String getName() {
        return gatewayName;
    }

    public static class Unsupported extends RuntimeException {
        public Unsupported() { super(); }
        public Unsupported(String msg) { super(msg); }
    }

    public static boolean isValidPaymentGateway(String name) {
        try {
            valueFrom(name);
            return true;
        } catch (RuntimeException e){
            return false;
        }
    }

    public static PaymentGatewayName valueFrom(String gatewayName) {
        for (PaymentGatewayName paymentGatewayName : values()) {
            if (paymentGatewayName.getName().equals(gatewayName)) {
                return paymentGatewayName;
            }
        }
        throw new Unsupported("Unsupported Payment Gateway " + gatewayName);
    }
}
