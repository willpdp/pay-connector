package uk.gov.pay.connector.service;

import fj.data.Either;
import uk.gov.pay.connector.dao.ChargeDao;
import uk.gov.pay.connector.dao.GatewayAccountDao;
import uk.gov.pay.connector.model.*;
import uk.gov.pay.connector.model.domain.Card;
import uk.gov.pay.connector.model.domain.ChargeStatus;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

import static fj.data.Either.left;
import static fj.data.Either.right;
import static java.lang.String.format;
import static uk.gov.pay.connector.model.CaptureRequest.captureRequest;
import static uk.gov.pay.connector.model.GatewayError.baseGatewayError;
import static uk.gov.pay.connector.model.GatewayErrorType.ChargeNotFound;
import static uk.gov.pay.connector.model.domain.ChargeStatus.*;

public class CardService {
    public static final String GATEWAY_ACCOUNT_ID_KEY = "gateway_account_id";
    public static final String PAYMENT_PROVIDER_KEY = "payment_provider";
    public static final String GATEWAY_TRANSACTION_ID_KEY = "gateway_transaction_id";
    public static final String AMOUNT_KEY = "amount";

    private final GatewayAccountDao accountDao;
    private final ChargeDao chargeDao;
    private final PaymentProviders providers;

    public CardService(GatewayAccountDao accountDao, ChargeDao chargeDao, PaymentProviders providers) {
        this.accountDao = accountDao;
        this.chargeDao = chargeDao;
        this.providers = providers;
    }

    public Either<GatewayError, GatewayResponse> doAuthorise(String chargeId, Card cardDetails) {
        return chargeDao
                .findById(chargeId)
                .map(authoriseFor(chargeId, cardDetails))
                .orElseGet(chargeNotFound(chargeId));
    }

    public Either<GatewayError, GatewayResponse> doCapture(String chargeId) {
        return chargeDao
                .findById(chargeId)
                .map(captureFor(chargeId))
                .orElseGet(chargeNotFound(chargeId));
    }

    private Function<Map<String, Object>, Either<GatewayError, GatewayResponse>> captureFor(String chargeId) {
        return charge -> hasStatus(charge, AUTHORISATION_SUCCESS) ?
                right(captureFor(chargeId, charge)) :
                left(captureErrorMessageFor((String) charge.get(STATUS_KEY)));
    }

    private Function<Map<String, Object>, Either<GatewayError,GatewayResponse>> authoriseFor(String chargeId, Card cardDetails) {
        return charge -> hasStatus(charge, CREATED) ?
                right(authoriseFor(chargeId, cardDetails, charge)) :
                left(authoriseErrorMessageFor(chargeId));
    }

    private GatewayResponse captureFor(String chargeId, Map<String, Object> charge) {

        String transactionId = String.valueOf(charge.get(GATEWAY_TRANSACTION_ID_KEY));

        CaptureRequest request = captureRequest(transactionId, String.valueOf(charge.get(AMOUNT_KEY)));
        CaptureResponse response = paymentProviderFor(charge).capture(request);

        if (response.isSuccessful()) {
            chargeDao.updateStatus(chargeId, CAPTURED);
        }
        return response;
    }

    private GatewayResponse authoriseFor(String chargeId, Card cardDetails, Map<String, Object> charge) {

        AuthorisationRequest request = authorisationRequest(String.valueOf(charge.get(AMOUNT_KEY)), cardDetails);
        AuthorisationResponse response = paymentProviderFor(charge).authorise(request);

        if (response.getNewChargeStatus() != null) {
            chargeDao.updateStatus(chargeId, response.getNewChargeStatus());
            chargeDao.updateGatewayTransactionId(chargeId, response.getTransactionId());
        }

        return response;
    }

    private PaymentProvider paymentProviderFor(Map<String, Object> charge) {
        Optional<Map<String, Object>> maybeAccount = accountDao.findById((String) charge.get(GATEWAY_ACCOUNT_ID_KEY));
        String paymentProviderName = String.valueOf(maybeAccount.get().get(PAYMENT_PROVIDER_KEY));
        return providers.resolve(paymentProviderName);
    }

    private AuthorisationRequest authorisationRequest(String amountValue, Card card) {
        return new AuthorisationRequest(card, amountValue, "This is the description");
    }

    private boolean hasStatus(Map<String, Object> charge, ChargeStatus status) {
        return status.getValue().equals(charge.get(STATUS_KEY));
    }

    private GatewayError captureErrorMessageFor(String currentStatus) {
        return baseGatewayError(formattedError("Cannot capture a charge with status %s.", currentStatus));
    }

    private GatewayError authoriseErrorMessageFor(String chargeId) {
        return baseGatewayError(formattedError("Card already processed for charge with id %s.", chargeId));
    }

    private Supplier<Either<GatewayError, GatewayResponse>> chargeNotFound(String chargeId) {
        return () -> left(new GatewayError(formattedError("Charge with id [%s] not found.", chargeId), ChargeNotFound));
    }


    private String formattedError(String messageTemplate, String... params) {
        return format(messageTemplate, params);
    }
}