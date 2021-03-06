package uk.gov.pay.connector.model.domain;

import static uk.gov.pay.connector.model.domain.ChargeEntityFixture.aValidChargeEntity;

public class RefundEntityFixture {

    private Long amount = 500L;
    private RefundStatus status = RefundStatus.CREATED;
    private GatewayAccountEntity gatewayAccountEntity = ChargeEntityFixture.defaultGatewayAccountEntity();
    private ChargeEntity charge;
    private String reference = "reference";
    public static RefundEntityFixture aValidRefundEntity() {
        return new RefundEntityFixture();
    }

    public RefundEntity build() {
        ChargeEntity chargeEntity = charge == null ? buildChargeEntity() : charge;
        RefundEntity refundEntity = new RefundEntity(chargeEntity, amount);
        refundEntity.setStatus(status);
        refundEntity.setReference(reference);
        return refundEntity;
    }

    public RefundEntityFixture withStatus(RefundStatus status) {
        this.status = status;
        return this;
    }

    public RefundEntityFixture withAmount(Long amount) {
        this.amount = amount;
        return this;
    }

    public RefundEntityFixture withReference(String reference) {
        this.reference = reference;
        return this;
    }

    public RefundEntityFixture withGatewayAccountEntity(GatewayAccountEntity gatewayAccountEntity) {
        this.gatewayAccountEntity = gatewayAccountEntity;
        return this;
    }

    public Long getAmount() {
        return amount;
    }

    private ChargeEntity buildChargeEntity() {
        return aValidChargeEntity().withGatewayAccountEntity(gatewayAccountEntity).build();
    }

    public RefundEntityFixture withCharge(ChargeEntity charge) {
        this.charge = charge;
        return this;
    }
}
