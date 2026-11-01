package com.example.capstone_3.Service;

import com.example.capstone_3.Api.ApiException;
import com.example.capstone_3.DtoIn.OfferEvaluationDtoIn;
import com.example.capstone_3.Model.*;
import com.example.capstone_3.Repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openai.client.OpenAIClient;
import com.openai.models.chat.completions.ChatCompletion;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class OfferEvaluationTests {
    private final AccountAccessService access = mock(AccountAccessService.class);
    private final AccountSkillRepository accountSkills = mock(AccountSkillRepository.class);
    private final SkillRepository skills = mock(SkillRepository.class);
    private final SkillOfferRepository offers = mock(SkillOfferRepository.class);
    private final OpenAIClient client = mock(OpenAIClient.class, RETURNS_DEEP_STUBS);
    private final AIService service = new AIService(access, mock(SkillAssessmentService.class), mock(AccountRepository.class),
            accountSkills, skills, mock(SkillSuggestionService.class), offers, client, new ObjectMapper(), mock(LearningRequestRepository.class), mock(ExchangeRepository.class));
    private final AccountSkill owned = new AccountSkill();
    private final OfferEvaluationDtoIn input = new OfferEvaluationDtoIn("20-minute Java variables session with two exercises", "ONLINE", 10, 1);

    @BeforeEach
    void setup() {
        Account account = new Account();
        Skill skill = new Skill(); skill.setId(2); skill.setName("Java");
        owned.setVerified(true);
        when(access.requireActive(1)).thenReturn(account);
        when(skills.findSkillById(2)).thenReturn(skill);
        when(accountSkills.findAccountSkillByAccountAndSkill(account, skill)).thenReturn(owned);
        when(offers.findAllBySkill(skill)).thenReturn(List.of());
    }

    private void aiResponse(String json) {
        ChatCompletion completion = mock(ChatCompletion.class);
        ChatCompletion.Choice choice = mock(ChatCompletion.Choice.class, RETURNS_DEEP_STUBS);
        when(completion.choices()).thenReturn(List.of(choice));
        when(choice.message().content()).thenReturn(Optional.of(json));
        when(client.chat().completions().create(any(ChatCompletionCreateParams.class))).thenReturn(completion);
    }

    @Test
    void insufficientEvidenceReturnsNoSuggestedPriceAndDoesNotSave() {
        aiResponse("{\"verdict\":\"INSUFFICIENT_INFORMATION\",\"suggestedTokens\":null,\"explanation\":\"No reliable comparable offers\",\"suggestions\":[\"Add learning scope\"]}");
        var result = service.evaluateOffer(1, 2, input);
        assertNull(result.getSuggestedTokens()); assertEquals(10, result.getProposedTokens());
        verify(offers, never()).save(any());
    }

    @Test
    void fairPriceMustEqualTheEnteredPrice() {
        aiResponse("{\"verdict\":\"FAIR\",\"suggestedTokens\":10,\"explanation\":\"Comparable scope and duration\",\"suggestions\":[]}");
        assertEquals(10, service.evaluateOffer(1, 2, input).getSuggestedTokens());
        verify(offers, never()).save(any());
    }

    @Test
    void inconsistentPriceIsRejected() {
        aiResponse("{\"verdict\":\"OVERPRICED\",\"suggestedTokens\":12,\"explanation\":\"Comparable evidence\",\"suggestions\":[]}");
        assertThrows(ApiException.class, () -> service.evaluateOffer(1, 2, input));
    }

    @Test
    void unverifiedSkillCannotBeEvaluated() {
        owned.setVerified(false);
        assertThrows(ApiException.class, () -> service.evaluateOffer(1, 2, input));
        verifyNoInteractions(client);
    }
}
