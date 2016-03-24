package uk.gov.pay.connector.unit.service;

import fj.data.Either;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import uk.gov.pay.connector.model.*;
import uk.gov.pay.connector.model.domain.ChargeEntity;
import uk.gov.pay.connector.model.domain.ChargeStatus;
import uk.gov.pay.connector.service.CardCaptureService;

import javax.persistence.OptimisticLockException;
import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;
import static uk.gov.pay.connector.model.ErrorType.*;
import static uk.gov.pay.connector.model.domain.ChargeStatus.*;

public class CardCaptureServiceTest extends CardServiceTest {
    private final CardCaptureService cardCaptureService = new CardCaptureService(mockedAccountDao, mockedChargeDao, mockedProviders);

    @Test
    public void shouldCaptureACharge() throws Exception {
        Long chargeId = 1L;
        String gatewayTxId = "theTxId";

        ChargeEntity charge = createNewChargeWith(chargeId, AUTHORISATION_SUCCESS);
        charge.setGatewayTransactionId(gatewayTxId);

        when(mockedChargeDao.findByExternalId(charge.getExternalId()))
                .thenReturn(Optional.of(charge));
        when(mockedChargeDao.merge(any()))
                .thenReturn(charge);

        mockSuccessfulCapture();

        Either<ErrorResponse, GatewayResponse> response = cardCaptureService.doCapture(charge.getExternalId());

        assertTrue(response.isRight());
        assertThat(response.right().value(), is(aSuccessfulResponse()));
        ArgumentCaptor<ChargeEntity> argumentCaptor = ArgumentCaptor.forClass(ChargeEntity.class);
        verify(mockedChargeDao).mergeAndNotifyStatusHasChanged(argumentCaptor.capture());

        assertThat(argumentCaptor.getValue().getStatus(), is(CAPTURE_SUBMITTED.getValue()));

        ArgumentCaptor<CaptureRequest> request = ArgumentCaptor.forClass(CaptureRequest.class);
        verify(mockedPaymentProvider, times(1)).capture(request.capture());
        assertThat(request.getValue().getTransactionId(), is(gatewayTxId));
    }

    @Test
    public void shouldGetAChargeNotFoundWhenChargeDoesNotExist() {

        String chargeId = "jgk3erq5sv2i4cds6qqa9f1a8a";

        when(mockedChargeDao.findByExternalId(chargeId))
                .thenReturn(Optional.empty());

        Either<ErrorResponse, GatewayResponse> response = cardCaptureService.doCapture(chargeId);

        assertTrue(response.isLeft());
        ErrorResponse errorResponse = response.left().value();

        assertThat(errorResponse.getErrorType(), is(CHARGE_NOT_FOUND));
        assertThat(errorResponse.getMessage(), is("Charge with id [jgk3erq5sv2i4cds6qqa9f1a8a] not found."));
    }

    @Test
    public void shouldGetAOperationAlreadyInProgressWhenStatusIsCaptureReady() throws Exception {
        Long chargeId = 1234L;

        ChargeEntity charge = createNewChargeWith(chargeId, ChargeStatus.CAPTURE_READY);

        when(mockedChargeDao.findByExternalId(charge.getExternalId()))
                .thenReturn(Optional.of(charge));
        when(mockedChargeDao.merge(any()))
                .thenReturn(charge);

        Either<ErrorResponse, GatewayResponse> response = cardCaptureService.doCapture(charge.getExternalId());

        assertTrue(response.isLeft());
        ErrorResponse gatewayError = response.left().value();

        assertThat(gatewayError.getErrorType(), is(OPERATION_ALREADY_IN_PROGRESS));
        assertThat(gatewayError.getMessage(), is("Capture for charge already in progress, " + charge.getExternalId()));
    }

    @Test
    public void shouldGetAIllegalErrorWhenInvalidStatus() throws Exception {
        Long chargeId = 1234L;

        ChargeEntity charge = createNewChargeWith(chargeId, ChargeStatus.CREATED);

        when(mockedChargeDao.findByExternalId(charge.getExternalId()))
                .thenReturn(Optional.of(charge));
        when(mockedChargeDao.merge(any()))
                .thenReturn(charge);

        Either<ErrorResponse, GatewayResponse> response = cardCaptureService.doCapture(charge.getExternalId());

        assertTrue(response.isLeft());
        ErrorResponse gatewayError = response.left().value();

        assertThat(gatewayError.getErrorType(), is(ILLEGAL_STATE_ERROR));
        assertThat(gatewayError.getMessage(), is("Charge not in correct state to be processed, " + charge.getExternalId()));
    }

    @Test
    public void shouldGetAConflictErrorWhenConflicting() throws Exception {
        Long chargeId = 1234L;

        ChargeEntity charge = createNewChargeWith(chargeId, ChargeStatus.CREATED);

        when(mockedChargeDao.findByExternalId(charge.getExternalId()))
                .thenReturn(Optional.of(charge));
        when(mockedChargeDao.merge(any()))
                .thenThrow(new OptimisticLockException());

        Either<ErrorResponse, GatewayResponse> response = cardCaptureService.doCapture(charge.getExternalId());

        assertTrue(response.isLeft());
        ErrorResponse gatewayError = response.left().value();

        assertThat(gatewayError.getErrorType(), is(CONFLICT_ERROR));
        assertThat(gatewayError.getMessage(), is("Capture for charge conflicting, " + charge.getExternalId()));
    }

    @Test
    public void shouldUpdateChargeWithCaptureErrorWhenCaptureFails() {
        Long chargeId = 1L;
        String gatewayTxId = "theTxId";
        ChargeEntity charge = createNewChargeWith(chargeId, AUTHORISATION_SUCCESS);
        charge.setGatewayTransactionId(gatewayTxId);

        ChargeEntity reloadedCharge = mock(ChargeEntity.class);

        when(mockedChargeDao.findByExternalId(charge.getExternalId()))
                .thenReturn(Optional.of(charge));
        when(mockedChargeDao.merge(any()))
                .thenReturn(charge)
                .thenReturn(reloadedCharge);

        mockUnsuccessfulCapture();

        Either<ErrorResponse, GatewayResponse> response = cardCaptureService.doCapture(charge.getExternalId());

        assertTrue(response.isRight());
        assertThat(response.right().value(), is(anUnSuccessfulResponse()));

        verify(reloadedCharge, times(1)).setStatus(CAPTURE_ERROR);
    }

    private void mockSuccessfulCapture() {
        when(mockedProviders.resolve(providerName))
                .thenReturn(mockedPaymentProvider);
        when(mockedPaymentProvider.capture(any()))
                .thenReturn(CaptureResponse.successfulCaptureResponse(CAPTURE_SUBMITTED));
    }

    private void mockUnsuccessfulCapture() {
        when(mockedProviders.resolve(providerName))
                .thenReturn(mockedPaymentProvider);
        when(mockedPaymentProvider.capture(any()))
                .thenReturn(CaptureResponse.captureFailureResponse(ErrorResponse.baseError("error")));
    }
}