package uk.gov.pay.connector.model.domain;

import com.google.common.collect.ImmutableMap;
import uk.gov.pay.connector.command.StateTransitionGraphVizRenderer;

import java.util.List;
import java.util.Map;

import static uk.gov.pay.connector.model.domain.ChargeStatus.*;

public final class DefaultStateTransitions extends StateTransitions {

    private static final Map<ChargeStatus, List<ChargeStatus>> TRANSITION_TABLE = ImmutableMap.<ChargeStatus, List<ChargeStatus>>builder()

            .put(CREATED,                       validTransitions(ENTERING_CARD_DETAILS, SYSTEM_CANCELLED, EXPIRED))
            .put(ENTERING_CARD_DETAILS,         validTransitions(AUTHORISATION_READY, AUTHORISATION_ABORTED, EXPIRED, USER_CANCELLED, SYSTEM_CANCELLED))
            .put(AUTHORISATION_READY,           validTransitions(AUTHORISATION_SUCCESS, AUTHORISATION_REJECTED, AUTHORISATION_ERROR, AUTHORISATION_3DS_REQUIRED, AUTHORISATION_CANCELLED, AUTHORISATION_SUBMITTED))
            .put(AUTHORISATION_SUBMITTED,       validTransitions(AUTHORISATION_SUCCESS, AUTHORISATION_REJECTED, AUTHORISATION_ERROR, AUTHORISATION_3DS_REQUIRED))
            .put(AUTHORISATION_3DS_REQUIRED,    validTransitions(AUTHORISATION_3DS_READY, USER_CANCELLED, EXPIRED))
            .put(AUTHORISATION_3DS_READY,       validTransitions(AUTHORISATION_SUCCESS, AUTHORISATION_REJECTED, AUTHORISATION_ERROR, AUTHORISATION_CANCELLED))
            .put(AUTHORISATION_SUCCESS,         validTransitions(CAPTURE_APPROVED, CAPTURE_READY, SYSTEM_CANCEL_READY, USER_CANCEL_READY, EXPIRE_CANCEL_READY))
            .put(CAPTURE_APPROVED,              validTransitions(CAPTURE_READY, CAPTURE_ERROR))
            .put(CAPTURE_APPROVED_RETRY,        validTransitions(CAPTURE_READY, CAPTURE_ERROR, CAPTURED))
            .put(CAPTURE_READY,                 validTransitions(CAPTURE_SUBMITTED, CAPTURE_ERROR, CAPTURE_APPROVED_RETRY))
            .put(CAPTURE_SUBMITTED,             validTransitions(CAPTURED)) // can this ever be a capture error?
            .put(EXPIRE_CANCEL_READY,           validTransitions(EXPIRE_CANCEL_SUBMITTED, EXPIRE_CANCEL_FAILED, EXPIRED))
            .put(EXPIRE_CANCEL_SUBMITTED,       validTransitions(EXPIRE_CANCEL_FAILED, EXPIRED))
            .put(SYSTEM_CANCEL_READY,           validTransitions(SYSTEM_CANCEL_SUBMITTED, SYSTEM_CANCEL_ERROR, SYSTEM_CANCELLED))
            .put(SYSTEM_CANCEL_SUBMITTED,       validTransitions(SYSTEM_CANCEL_ERROR, SYSTEM_CANCELLED))
            .put(USER_CANCEL_READY,             validTransitions(USER_CANCEL_SUBMITTED, USER_CANCEL_ERROR, USER_CANCELLED))
            .put(USER_CANCEL_SUBMITTED,         validTransitions(USER_CANCEL_ERROR, USER_CANCELLED))
            .build();

    DefaultStateTransitions() {
        super(TRANSITION_TABLE);
    }

    public static StateTransitionGraphVizRenderer dumpGraphViz() {
        return new StateTransitionGraphVizRenderer(TRANSITION_TABLE);
    }

}
