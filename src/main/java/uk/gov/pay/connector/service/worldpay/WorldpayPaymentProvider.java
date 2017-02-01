package uk.gov.pay.connector.service.worldpay;

import fj.data.Either;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import uk.gov.pay.connector.model.CancelGatewayRequest;
import uk.gov.pay.connector.model.CaptureGatewayRequest;
import uk.gov.pay.connector.model.Notifications;
import uk.gov.pay.connector.model.RefundGatewayRequest;
import uk.gov.pay.connector.model.gateway.AuthorisationGatewayRequest;
import uk.gov.pay.connector.model.gateway.GatewayResponse;
import uk.gov.pay.connector.resources.PaymentGatewayName;
import uk.gov.pay.connector.service.*;

import java.time.ZoneOffset;
import java.util.Optional;
import java.util.function.Function;

import static fj.data.Either.left;
import static fj.data.Either.right;
import static java.util.UUID.randomUUID;
import static uk.gov.pay.connector.model.domain.GatewayAccount.CREDENTIALS_MERCHANT_ID;
import static uk.gov.pay.connector.service.worldpay.WorldpayOrderRequestBuilder.*;
import static uk.gov.pay.connector.util.XMLUnmarshaller.unmarshall;

public class WorldpayPaymentProvider extends BasePaymentProvider<BaseResponse> {

    public WorldpayPaymentProvider(GatewayClient client) {
        super(client, false, null);
    }
    public WorldpayPaymentProvider(GatewayClient client, boolean isNotificationEndpointSecured, String notificationDomain) {
        super(client, isNotificationEndpointSecured, notificationDomain);
    }

    @Override
    public String getPaymentGatewayName() {
        return PaymentGatewayName.WORLDPAY.getName();
    }

    @Override
    public Optional<String> generateTransactionId() {
        return Optional.of(randomUUID().toString());
    }

    @Override
    public GatewayResponse authorise(AuthorisationGatewayRequest request) {
        return sendReceive(request, buildAuthoriseOrderFor(), WorldpayOrderStatusResponse.class);
    }

    @Override
    public GatewayResponse capture(CaptureGatewayRequest request) {
        return sendReceive(request, buildCaptureOrderFor(), WorldpayCaptureResponse.class);
    }

    @Override
    public GatewayResponse refund(RefundGatewayRequest request) {
        return sendReceive(request, buildRefundOrderFor(), WorldpayRefundResponse.class);
    }

    @Override
    public GatewayResponse cancel(CancelGatewayRequest request) {
        return sendReceive(request, buildCancelOrderFor(), WorldpayCancelResponse.class);

    }

    @Override
    public Boolean isNotificationEndpointSecured() {
        return this.isNotificationEndpointSecured;
    }

    @Override
    public String getNotificationDomain() {
        return this.notificationDomain;
    }

    @Override
    public Either<String, Notifications<String>> parseNotification(String payload) {
        try {
            Notifications.Builder<String> builder = Notifications.builder();
            WorldpayNotification worldpayNotification = unmarshall(payload, WorldpayNotification.class);
            builder.addNotificationFor(
                worldpayNotification.getTransactionId(),
                worldpayNotification.getReference(),
                worldpayNotification.getStatus(),
                worldpayNotification.getBookingDate().atStartOfDay(ZoneOffset.UTC)
            );

            return right(builder.build());
        } catch (Exception e) {
            return left(e.getMessage());
        }
    }

    @Override
    public StatusMapper getStatusMapper() {
        return WorldpayStatusMapper.get();
    }

    private Function<AuthorisationGatewayRequest, GatewayOrder> buildAuthoriseOrderFor() {
        return request -> aWorldpayAuthoriseOrderRequestBuilder()
                .withSessionId(request.getChargeExternalId())
                .with3dsRequired(request.getGatewayAccount().isRequires3ds())
                .withTransactionId(request.getTransactionId().orElse(""))
                .withMerchantCode(request.getGatewayAccount().getCredentials().get(CREDENTIALS_MERCHANT_ID))
                .withDescription(request.getDescription())
                .withAmount(request.getAmount())
                .withAuthorisationDetails(request.getAuthorisationDetails())
                .build();
    }

    private Function<CaptureGatewayRequest, GatewayOrder> buildCaptureOrderFor() {
        return request -> aWorldpayCaptureOrderRequestBuilder()
                .withDate(DateTime.now(DateTimeZone.UTC))
                .withMerchantCode(request.getGatewayAccount().getCredentials().get(CREDENTIALS_MERCHANT_ID))
                .withAmount(request.getAmount())
                .withTransactionId(request.getTransactionId())
                .build();
    }

    private Function<RefundGatewayRequest, GatewayOrder> buildRefundOrderFor() {
        return request -> aWorldpayRefundOrderRequestBuilder()
                .withReference(request.getReference())
                .withMerchantCode(request.getGatewayAccount().getCredentials().get(CREDENTIALS_MERCHANT_ID))
                .withAmount(request.getAmount())
                .withTransactionId(request.getTransactionId())
                .build();
    }

    private Function<CancelGatewayRequest, GatewayOrder> buildCancelOrderFor() {
        return request -> aWorldpayCancelOrderRequestBuilder()
                .withTransactionId(request.getTransactionId())
                .withMerchantCode(request.getGatewayAccount().getCredentials().get(CREDENTIALS_MERCHANT_ID))
                .build();
    }
}
