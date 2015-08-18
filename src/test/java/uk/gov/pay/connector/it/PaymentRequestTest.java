package uk.gov.pay.connector.it;

import com.jayway.restassured.http.ContentType;
import org.junit.Rule;
import org.junit.Test;
import uk.gov.pay.connector.util.DropwizardAppWithPostgresRule;

import static com.jayway.restassured.RestAssured.given;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class PaymentRequestTest {

    @Rule
    public DropwizardAppWithPostgresRule app = new DropwizardAppWithPostgresRule();

    @Test
    public void makePaymentAndRetrieveAmount() throws Exception {
        int expectedAmount = 2113;
        String payId = given().port(app.getLocalPort())
                .contentType(ContentType.JSON)
                .body(String.format("{\"amount\":%d}", expectedAmount))
                .post("/api/payment")
                .then()
                .statusCode(200)
                .extract()
                .path("pay_id");

        int amount = given().port(app.getLocalPort())
                .get("/frontend/payment/" + payId)
                .then()
                .statusCode(200)
                .extract()
                .path("amount");

        assertThat(amount, is(expectedAmount));
    }
}
