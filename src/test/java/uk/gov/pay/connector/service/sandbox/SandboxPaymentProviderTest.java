package uk.gov.pay.connector.service.sandbox;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import uk.gov.pay.connector.model.CancelGatewayRequest;
import uk.gov.pay.connector.model.CaptureGatewayRequest;
import uk.gov.pay.connector.model.GatewayError;
import uk.gov.pay.connector.model.RefundGatewayRequest;
import uk.gov.pay.connector.model.domain.Card;
import uk.gov.pay.connector.model.domain.ChargeEntityFixture;
import uk.gov.pay.connector.model.domain.RefundEntityFixture;
import uk.gov.pay.connector.model.gateway.AuthorisationGatewayRequest;
import uk.gov.pay.connector.model.gateway.GatewayResponse;
import uk.gov.pay.connector.service.BaseAuthoriseResponse;
import uk.gov.pay.connector.service.BaseCancelResponse;
import uk.gov.pay.connector.service.BaseCaptureResponse;
import uk.gov.pay.connector.service.BaseRefundResponse;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsSame.sameInstance;
import static uk.gov.pay.connector.model.ErrorType.GENERIC_GATEWAY_ERROR;

public class SandboxPaymentProviderTest {

    private SandboxPaymentProvider provider;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Before
    public void setup() {
        provider = new SandboxPaymentProvider();
    }

    @Test
    public void getPaymentGatewayName_shouldGetExpectedName() {
        Assert.assertThat(provider.getPaymentGatewayName(), is("sandbox"));
    }

    @Test
    public void GetStatusMapper_shouldGetExpectedInstance() {
        Assert.assertThat(provider.getStatusMapper(), sameInstance(SandboxStatusMapper.get()));
    }

    @Test
    public void generateTransactionId_shouldGenerateANonNullValue() {
        assertThat(provider.generateTransactionId(), is(notNullValue()));
    }

    @Test
    public void parseNotification_shouldFailParsingNotification() throws Exception {

        String notification = "{\"transaction_id\":\"1\",\"status\":\"BOOM\", \"reference\":\"abc\"}";

        expectedException.expect(UnsupportedOperationException.class);
        expectedException.expectMessage(is("Sandbox account does not support notifications"));

        provider.parseNotification(notification);
    }

    @Test
    public void authorise_shouldBeAuthorisedWhenCardNumIsExpectedToSucceedForAuthorisation() {

        Card card = new Card();
        card.setCardNo("4242424242424242");
        GatewayResponse gatewayResponse = provider.authorise(new AuthorisationGatewayRequest(ChargeEntityFixture.aValidChargeEntity().build(), card));

        assertThat(gatewayResponse.isSuccessful(), is(true));
        assertThat(gatewayResponse.isFailed(), is(false));
        assertThat(gatewayResponse.getGatewayError().isPresent(), is(false));
        assertThat(gatewayResponse.getBaseResponse().isPresent(), is(true));
        assertThat(gatewayResponse.getBaseResponse().get() instanceof BaseAuthoriseResponse, is(true));

        BaseAuthoriseResponse authoriseResponse = (BaseAuthoriseResponse) gatewayResponse.getBaseResponse().get();
        assertThat(authoriseResponse.isAuthorised(), is(true));
        assertThat(authoriseResponse.getTransactionId(), is(notNullValue()));
        assertThat(authoriseResponse.getErrorCode(), is(nullValue()));
        assertThat(authoriseResponse.getErrorMessage(), is(nullValue()));
    }

    @Test
    public void authorise_shouldNotBeAuthorisedWhenCardNumIsExpectedToBeRejectedForAuthorisation() {

        Card card = new Card();
        card.setCardNo("4000000000000069");
        GatewayResponse gatewayResponse = provider.authorise(new AuthorisationGatewayRequest(ChargeEntityFixture.aValidChargeEntity().build(), card));

        assertThat(gatewayResponse.isSuccessful(), is(true));
        assertThat(gatewayResponse.isFailed(), is(false));
        assertThat(gatewayResponse.getGatewayError().isPresent(), is(false));
        assertThat(gatewayResponse.getBaseResponse().isPresent(), is(true));
        assertThat(gatewayResponse.getBaseResponse().get() instanceof BaseAuthoriseResponse, is(true));

        BaseAuthoriseResponse authoriseResponse = (BaseAuthoriseResponse) gatewayResponse.getBaseResponse().get();
        assertThat(authoriseResponse.isAuthorised(), is(false));
        assertThat(authoriseResponse.getTransactionId(), is(notNullValue()));
        assertThat(authoriseResponse.getErrorCode(), is(nullValue()));
        assertThat(authoriseResponse.getErrorMessage(), is(nullValue()));
    }

    @Test
    public void authorise_shouldGetGatewayErrorWhenCardNumIsExpectedToFailForAuthorisation() {

        Card card = new Card();
        card.setCardNo("4000000000000119");
        GatewayResponse gatewayResponse = provider.authorise(new AuthorisationGatewayRequest(ChargeEntityFixture.aValidChargeEntity().build(), card));

        assertThat(gatewayResponse.isSuccessful(), is(false));
        assertThat(gatewayResponse.isFailed(), is(true));
        assertThat(gatewayResponse.getGatewayError().isPresent(), is(true));
        assertThat(gatewayResponse.getBaseResponse().isPresent(), is(false));

        GatewayError gatewayError = (GatewayError) gatewayResponse.getGatewayError().get();
        assertThat(gatewayError.getErrorType(), is(GENERIC_GATEWAY_ERROR));
        assertThat(gatewayError.getMessage(), is("This transaction could be not be processed."));
    }

    @Test
    public void authorise_shouldGetGatewayErrorWhenCardNumDoesNotExistForAuthorisation() {

        Card card = new Card();
        card.setCardNo("3456789987654567");
        GatewayResponse gatewayResponse = provider.authorise(new AuthorisationGatewayRequest(ChargeEntityFixture.aValidChargeEntity().build(), card));

        assertThat(gatewayResponse.isSuccessful(), is(false));
        assertThat(gatewayResponse.isFailed(), is(true));
        assertThat(gatewayResponse.getGatewayError().isPresent(), is(true));
        assertThat(gatewayResponse.getBaseResponse().isPresent(), is(false));

        GatewayError gatewayError = (GatewayError) gatewayResponse.getGatewayError().get();
        assertThat(gatewayError.getErrorType(), is(GENERIC_GATEWAY_ERROR));
        assertThat(gatewayError.getMessage(), is("Unsupported card details."));
    }

    @Test
    public void capture_shouldSucceedWhenCapturingAnyCharge() {

        GatewayResponse gatewayResponse = provider.capture(CaptureGatewayRequest.valueOf(ChargeEntityFixture.aValidChargeEntity().build()));

        assertThat(gatewayResponse.isSuccessful(), is(true));
        assertThat(gatewayResponse.isFailed(), is(false));
        assertThat(gatewayResponse.getGatewayError().isPresent(), is(false));
        assertThat(gatewayResponse.getBaseResponse().isPresent(), is(true));
        assertThat(gatewayResponse.getBaseResponse().get() instanceof BaseCaptureResponse, is(true));

        BaseCaptureResponse captureResponse = (BaseCaptureResponse) gatewayResponse.getBaseResponse().get();
        assertThat(captureResponse.getTransactionId(), is(notNullValue()));
        assertThat(captureResponse.getErrorCode(), is(nullValue()));
        assertThat(captureResponse.getErrorMessage(), is(nullValue()));
    }

    @Test
    public void capture_shouldSucceedWhenCancellingAnyCharge() {

        GatewayResponse gatewayResponse = provider.cancel(CancelGatewayRequest.valueOf(ChargeEntityFixture.aValidChargeEntity().build()));

        assertThat(gatewayResponse.isSuccessful(), is(true));
        assertThat(gatewayResponse.isFailed(), is(false));
        assertThat(gatewayResponse.getGatewayError().isPresent(), is(false));
        assertThat(gatewayResponse.getBaseResponse().isPresent(), is(true));
        assertThat(gatewayResponse.getBaseResponse().get() instanceof BaseCancelResponse, is(true));

        BaseCancelResponse cancelResponse = (BaseCancelResponse) gatewayResponse.getBaseResponse().get();
        assertThat(cancelResponse.getTransactionId(), is(notNullValue()));
        assertThat(cancelResponse.getErrorCode(), is(nullValue()));
        assertThat(cancelResponse.getErrorMessage(), is(nullValue()));
    }

    @Test
    public void refund_shouldSucceedWhenRefundingAnyCharge() {

        GatewayResponse gatewayResponse = provider.refund(RefundGatewayRequest.valueOf(RefundEntityFixture.aValidRefundEntity().build()));

        assertThat(gatewayResponse.isSuccessful(), is(true));
        assertThat(gatewayResponse.isFailed(), is(false));
        assertThat(gatewayResponse.getGatewayError().isPresent(), is(false));
        assertThat(gatewayResponse.getBaseResponse().isPresent(), is(true));
        assertThat(gatewayResponse.getBaseResponse().get() instanceof BaseRefundResponse, is(true));

        BaseRefundResponse refundResponse = (BaseRefundResponse) gatewayResponse.getBaseResponse().get();
        assertThat(refundResponse.getTransactionId(), is(notNullValue()));
        assertThat(refundResponse.getErrorCode(), is(nullValue()));
        assertThat(refundResponse.getErrorMessage(), is(nullValue()));
    }
}