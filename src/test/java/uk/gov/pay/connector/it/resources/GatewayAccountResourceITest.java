package uk.gov.pay.connector.it.resources;

import com.google.common.collect.ImmutableMap;
import com.jayway.restassured.response.ValidatableResponse;
import org.hamcrest.core.Is;
import org.junit.Test;

import static com.jayway.restassured.http.ContentType.JSON;
import static java.lang.String.format;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static uk.gov.pay.connector.util.JsonEncoder.toJson;

public class GatewayAccountResourceITest extends GatewayAccountResourceTestBase {

    @Test
    public void getAccountShouldReturn404IfAccountIdIsUnknown() throws Exception {

        String unknownAccountId = "92348739";

        givenSetup()
                .get(ACCOUNTS_API_URL + unknownAccountId)
                .then()
                .statusCode(404);
    }

    @Test
    public void getAccountShouldNotReturnCredentials() throws Exception {

        String gatewayAccountId = createAGatewayAccountFor("worldpay");

        givenSetup()
                .get(ACCOUNTS_API_URL + gatewayAccountId)
                .then()
                .statusCode(200)
                .body("credentials", is(nullValue()));
    }

    @Test
    public void createGatewayAccountWithoutPaymentProviderDefaultsToSandbox() throws Exception {

        String payload = toJson(ImmutableMap.of("name", "test account"));

        ValidatableResponse response = givenSetup()
                .body(payload)
                .post(ACCOUNTS_API_URL)
                .then()
                .statusCode(201);

        assertCorrectAccountLocationIn(response);

        assertGettingAccountReturnsProviderName(response, "sandbox");
    }

    @Test
    public void createAccountShouldFailIfPaymentProviderIsNotSandboxOfWorldpay() throws Exception {
        String testProvider = "random";
        String payload = toJson(ImmutableMap.of("payment_provider", testProvider));

        givenSetup()
                .body(payload)
                .post(ACCOUNTS_API_URL)
                .then()
                .statusCode(400)
                .contentType(JSON)
                .body("message", is(format("Unsupported payment provider %s.", testProvider)));
    }

    @Test
    public void getAccountShouldReturn404IfAccountIdIsNotNumeric() throws Exception {

        String unknownAcocuntId = "92348739wsx673hdg";

        givenSetup()
                .get(ACCOUNTS_API_URL + unknownAcocuntId)
                .then()
                .contentType(JSON)
                .statusCode(NOT_FOUND.getStatusCode())
                .body("code", Is.is(404))
                .body("message", Is.is("HTTP 404 Not Found"));
    }

    @Test
    public void createAGatewayAccountForSandbox() throws Exception {

        createAGatewayAccountFor("sandbox");
    }

    @Test
    public void createAGatewayAccountForWorldpay() throws Exception {

        createAGatewayAccountFor("worldpay");
    }

    @Test
    public void createAGatewayAccountForSmartpay() throws Exception {

        createAGatewayAccountFor("smartpay");
    }

}