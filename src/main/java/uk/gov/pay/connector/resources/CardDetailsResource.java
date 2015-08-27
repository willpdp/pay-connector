package uk.gov.pay.connector.resources;

import com.google.common.base.Optional;
import uk.gov.pay.connector.dao.ChargeDao;
import uk.gov.pay.connector.dao.PayDBIException;
import uk.gov.pay.connector.model.ChargeStatus;
import uk.gov.pay.connector.util.ResponseUtil;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import java.util.Map;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static uk.gov.pay.connector.resources.CardDetailsValidator.isValidCardDetails;

@Path("/")
public class CardDetailsResource {

    private ChargeDao chargeDao;

    public CardDetailsResource(ChargeDao chargeDao) {
        this.chargeDao = chargeDao;
    }

    @POST
    @Path("/v1/frontend/charges/{chargeId}/cards")
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    public Response addCardDetailsForCharge(@PathParam("chargeId") long chargeId, Map<String, Object> cardDetails) throws PayDBIException {

        Optional<Map<String, Object>> maybeCharge = Optional.fromNullable(chargeDao.findById(chargeId));
        if (!maybeCharge.isPresent()) {
            return responseWithChargeNotFound(chargeId);
        } else if (!hasStatusCreated(maybeCharge.get())) {
            return responseWithCardAlreadyProcessed(chargeId);
        }

        if (!isValidCardDetails(cardDetails)) {
            return responseWithInvalidCardDetails();
        }

        chargeDao.updateStatus(chargeId, ChargeStatus.AUTHORIZATION_SUBMITTED);

        //here comes the code for authorization - always successful for the scope of this story.

        chargeDao.updateStatus(chargeId, ChargeStatus.AUTHORIZATION_SUCCESS);

        return Response.noContent().build();

    }

    private boolean hasStatusCreated(Map<String, Object> charge) {
        return ChargeStatus.CREATED.getValue().equals(charge.get("status"));
    }

    private Response responseWithInvalidCardDetails() {
        return ResponseUtil.badResponse("Values do not match expected format/length.");
    }

    private Response responseWithCardAlreadyProcessed(long chargeId) {
        return ResponseUtil.badResponse(String.format("Card already processed for charge with id %s.", chargeId));
    }

    private Response responseWithChargeNotFound(long chargeId) {
        return ResponseUtil.notFoundResponse(String.format("Parent charge with id %s not found.", chargeId));
    }
}