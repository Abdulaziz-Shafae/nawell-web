package com.example.capstone_3.Service;

import com.example.capstone_3.Api.ApiException;
import com.example.capstone_3.Model.*;
import com.example.capstone_3.Repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ExchangeCompletionTests {
    private final ExchangeRepository exchanges = mock(ExchangeRepository.class);
    private final AccountRepository accounts = mock(AccountRepository.class);
    private final LearningRequestRepository requests = mock(LearningRequestRepository.class);
    private final TokenTransactionRepository transactions = mock(TokenTransactionRepository.class);
    private final ExchangeService service = new ExchangeService(new AccountAccessService(accounts), exchanges, accounts, requests,
            mock(SkillOfferRepository.class), transactions, mock(AccountNameHelper.class), mock(BrevoEmailService.class));
    private Exchange exchange;
    private Account provider;

    @BeforeEach
    void prepareAcceptedExchange() {
        Account requester = new Account();
        requester.setId(1);
        requester.setStatus("ACTIVE");
        provider = new Account();
        provider.setId(2);
        provider.setStatus("ACTIVE");
        provider.setTokenBalance(20);
        LearningRequest request = new LearningRequest();
        request.setRequesterAccount(requester);
        request.setProviderAccount(provider);
        request.setStatus("MATCHED");
        exchange = new Exchange();
        exchange.setId(3);
        exchange.setLearningRequest(request);
        exchange.setSkillOffer(new SkillOffer());
        exchange.setTokenAmount(10);
        exchange.setTokensReserved(true);
        exchange.setStatus("ACCEPTED");
        when(accounts.findAccountById(1)).thenReturn(requester);
        when(exchanges.findExchangeForUpdate(3)).thenReturn(exchange);
        when(accounts.findAccountForTokenUpdate(2)).thenReturn(provider);
        when(accounts.refundTokens(2, 10)).thenReturn(1);
    }

    @Test
    void completionPaysProviderOnceAndClosesRequest() {
        service.completeExchange(1, 3);
        assertEquals("COMPLETED", exchange.getStatus());
        assertEquals("CLOSED", exchange.getLearningRequest().getStatus());
        assertFalse(exchange.getTokensReserved());
        assertNotNull(exchange.getCompletedAt());
        ArgumentCaptor<TokenTransaction> ledger = ArgumentCaptor.forClass(TokenTransaction.class);
        verify(transactions).save(ledger.capture());
        assertEquals("TEACHING", ledger.getValue().getType());
        assertEquals(10, ledger.getValue().getAmount());
        assertSame(provider, ledger.getValue().getAccount());
        assertThrows(ApiException.class, () -> service.completeExchange(1, 3));
        verify(accounts, times(1)).refundTokens(2, 10);
    }

    @Test
    void unreservedExchangeCannotBeCompleted() {
        exchange.setTokensReserved(false);
        assertThrows(ApiException.class, () -> service.completeExchange(1, 3));
        verify(accounts, never()).refundTokens(anyInt(), anyInt());
        verifyNoInteractions(transactions);
    }

    @Test
    void unrelatedAccountCannotCompleteExchange() {
        Account unrelated = new Account();
        unrelated.setId(4);
        unrelated.setStatus("ACTIVE");
        when(accounts.findAccountById(4)).thenReturn(unrelated);
        assertThrows(ApiException.class, () -> service.completeExchange(4, 3));
        verify(accounts, never()).refundTokens(anyInt(), anyInt());
    }

    @Test
    void failedCreditDoesNotMarkExchangeCompleted() {
        when(accounts.refundTokens(2, 10)).thenReturn(0);
        assertThrows(ApiException.class, () -> service.completeExchange(1, 3));
        assertEquals("ACCEPTED", exchange.getStatus());
        assertTrue(exchange.getTokensReserved());
        verify(exchanges, never()).save(any());
        verifyNoInteractions(transactions);
    }
}
