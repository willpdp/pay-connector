package uk.gov.pay.connector.rules;

import com.google.common.io.Resources;

import java.io.IOException;
import java.nio.charset.Charset;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static javax.ws.rs.core.MediaType.TEXT_XML;
import static uk.gov.pay.connector.util.TransactionId.randomId;

public class WorldpayMockClient {

    public WorldpayMockClient() {
    }

    public void mockInquirySucccess(String gatewayTransactionId, String status) {
        String inquiryResponse = loadFromTemplate("inquiry-success-response.xml", gatewayTransactionId)
                .replace("{{status}}", status);
        paymentServiceResponse(inquiryResponse);
    }

    public void mockInquiryError() {
        String errorResponse = loadFromTemplate("inquiry-error-response.xml", "");
        paymentServiceResponse(errorResponse);
    }

    public void mockAuthorisationSuccess() {
        String gatewayTransactionId = randomId();
        String authoriseResponse = loadFromTemplate("authorisation-success-response.xml", gatewayTransactionId);
        paymentServiceResponse(authoriseResponse);
    }

    public void mockAuthorisationFailure() {
        String gatewayTransactionId = randomId();
        String authoriseResponse = loadFromTemplate("authorisation-failed-response.xml", gatewayTransactionId);
        paymentServiceResponse(authoriseResponse);
    }

    public void mockCaptureSuccess() {
        String gatewayTransactionId = randomId();
        String captureResponse = loadFromTemplate("capture-success-response.xml", gatewayTransactionId);
        paymentServiceResponse(captureResponse);
    }

    public void mockCancelSuccess(String gatewayTransactionId) {
        String cancelResponse = loadFromTemplate("cancel-success-response.xml", gatewayTransactionId);
        paymentServiceResponse(cancelResponse);
    }

    public void mockCancelError() {
        String cancelResponse = loadFromTemplate("cancel-error-response.xml","");
        paymentServiceResponse(cancelResponse);
    }

    public void mockCancelSuccessOnlyFor(String gatewayTransactionId) {
        String cancelSuccessResponse = loadFromTemplate("cancel-success-response.xml", gatewayTransactionId);
        String bodyMatchXpath = "//orderModification[@orderCode = '" + gatewayTransactionId + "']";
        bodyMatchingPaymentServiceResponse(bodyMatchXpath, cancelSuccessResponse);

    }

    public void mockRefundSuccess() {
        String gatewayTransactionId = randomId();
        String refundResponse = loadFromTemplate("refund-success-response.xml", gatewayTransactionId);
        paymentServiceResponse(refundResponse);
    }

    public void mockRefundError() {
        String gatewayTransactionId = randomId();
        String refundResponse = loadFromTemplate("refund-error-response.xml", gatewayTransactionId);
        paymentServiceResponse(refundResponse);
    }

    private void paymentServiceResponse(String responseBody) {
        //FIXME - This mocking approach is very poor. Needs to be revisited. Story PP-900 created.
        stubFor(
                post(urlPathEqualTo("/jsp/merchant/xml/paymentService.jsp"))
                        .willReturn(
                                aResponse()
                                        .withHeader(CONTENT_TYPE, TEXT_XML)
                                        .withStatus(200)
                                        .withBody(responseBody)
                        )
        );
    }

    private void bodyMatchingPaymentServiceResponse(String xpathContent, String responseBody) {
        stubFor(
                post(urlPathEqualTo("/jsp/merchant/xml/paymentService.jsp"))
                        .withRequestBody(matchingXPath(xpathContent))
                        .willReturn(
                                aResponse()
                                        .withHeader(CONTENT_TYPE, TEXT_XML)
                                        .withStatus(200)
                                        .withBody(responseBody)
                        )
        );
    }

    private String loadFromTemplate(String fileName, String gatewayTransactionId) {
        try {
            return Resources.toString(Resources.getResource("templates/worldpay/" + fileName), Charset.defaultCharset())
                    .replace("{{transactionId}}", gatewayTransactionId);
        } catch (IOException e) {
            throw new RuntimeException("Could not load template", e);
        }
    }
}
