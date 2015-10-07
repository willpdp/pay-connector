package uk.gov.pay.connector.it.resources;

import com.google.common.io.Resources;
import com.jayway.restassured.response.ResponseBodyExtractionOptions;
import org.junit.Test;
import uk.gov.pay.connector.it.base.CardResourceITestBase;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.UUID;

import static com.google.common.io.Resources.getResource;
import static com.jayway.restassured.RestAssured.given;
import static javax.ws.rs.core.MediaType.TEXT_XML;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static uk.gov.pay.connector.model.domain.ChargeStatus.AUTHORISATION_SUCCESS;
import static uk.gov.pay.connector.resources.PaymentProviderValidator.WORLDPAY_PROVIDER;

public class NotificationResourceITest extends CardResourceITestBase {

    private static final String RESPONSE_EXPECTED_BY_WORLDPAY = "[OK]";
    private static final String NOTIFICATION_PATH = "/v1/api/notifications/worldpay";

    public NotificationResourceITest() {
        super(WORLDPAY_PROVIDER);
    }

    @Test
    public void shouldHandleAWorldpayNotification() throws Exception {

        String transactionId = UUID.randomUUID().toString();
        String chargeId = createNewChargeWith(AUTHORISATION_SUCCESS, transactionId);

        worldpay.mockInquiryResponse(transactionId, "REFUSED");

        ResponseBodyExtractionOptions body = given().port(app.getLocalPort())
                .body(notificationPayloadForTransaction(transactionId))
                .contentType(TEXT_XML)
                .post(NOTIFICATION_PATH)
                .then()
                .statusCode(200)
                .extract().body();

        assertThat(body.asString(), is(RESPONSE_EXPECTED_BY_WORLDPAY));

        assertFrontendChargeStatusIs(chargeId, "AUTHORISATION REJECTED");
    }

    @Test
    public void shouldNotAddUnknownStatusToDatabase() throws Exception {
        String transactionId = UUID.randomUUID().toString();
        String chargeId = createNewChargeWith(AUTHORISATION_SUCCESS, transactionId);

        worldpay.mockInquiryResponse(transactionId, "PAID IN FULL WITH CABBAGES");

        ResponseBodyExtractionOptions body = given().port(app.getLocalPort())
                .body(notificationPayloadForTransaction(transactionId))
                .contentType(TEXT_XML)
                .post(NOTIFICATION_PATH)
                .then()
                .statusCode(200)
                .extract().body();

        assertThat(body.asString(), is(RESPONSE_EXPECTED_BY_WORLDPAY));

        assertFrontendChargeStatusIs(chargeId, "AUTHORISATION SUCCESS");
    }

    @Test
    public void shouldReturnErrorIfInquiryForChargeStatusFailed() throws Exception {
        String transactionId = UUID.randomUUID().toString();
        String chargeId = createNewChargeWith(AUTHORISATION_SUCCESS, transactionId);

        worldpay.mockErrorResponse();

        given().port(app.getLocalPort())
                .body(notificationPayloadForTransaction(transactionId))
                .contentType(TEXT_XML)
                .post(NOTIFICATION_PATH)
                .then()
                .statusCode(500);

        assertFrontendChargeStatusIs(chargeId, "AUTHORISATION SUCCESS");
    }

    private String notificationPayloadForTransaction(String transactionId) throws IOException {
        URL resource = getResource("templates/worldpay/notification.xml");
        return Resources.toString(resource, Charset.defaultCharset()).replace("{{transactionId}}", transactionId);
    }
}
