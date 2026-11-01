package com.example.capstone_3.Service;

import com.example.capstone_3.Api.ApiException;
import com.example.capstone_3.DtoIn.SessionDtoIn;
import com.example.capstone_3.Model.Exchange;
import com.example.capstone_3.Model.Session;
import com.example.capstone_3.Model.SessionParticipant;
import com.example.capstone_3.Model.SkillOffer;
import com.example.capstone_3.Repository.ExchangeRepository;
import com.example.capstone_3.Repository.SessionParticipantRepository;
import com.example.capstone_3.Repository.SessionRepository;
import com.example.capstone_3.Repository.SkillOfferRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SessionService {
    private final AccountAccessService accountAccessService;
    private final BrevoEmailService brevoEmailService;

    private final SessionRepository sessionRepository;
    private final SkillOfferRepository skillOfferRepository;
    private final ExchangeRepository exchangeRepository;
    private final SessionParticipantRepository sessionParticipantRepository;

    public List<Session> get() {
        return sessionRepository.findAll();
    }

    public void add(SessionDtoIn sessionDtoIn) {
        SkillOffer skillOffer = skillOfferRepository.findSkillOfferById(sessionDtoIn.getSkillOfferId());

        if (skillOffer == null) {
            throw new ApiException("No skill offer found");
        }

        Session session = new Session();
        session.setTitle(sessionDtoIn.getTitle());
        session.setScheduledAt(sessionDtoIn.getScheduledAt());
        session.setDurationMinutes(sessionDtoIn.getDurationMinutes());
        session.setMode(sessionDtoIn.getMode());
        session.setMeetingLink(sessionDtoIn.getMeetingLink());
        session.setLocation(sessionDtoIn.getLocation());
        session.setStatus(sessionDtoIn.getStatus() != null ? sessionDtoIn.getStatus() : "SCHEDULED");
        session.setSkillOffer(skillOffer);

        sessionRepository.save(session);
    }

    public void update(Integer id, SessionDtoIn sessionDtoIn) {
        Session oldSession = sessionRepository.findSessionById(id);

        if (oldSession == null) {
            throw new ApiException("No session found");
        }

SkillOffer skillOffer = skillOfferRepository.findSkillOfferById(sessionDtoIn.getSkillOfferId());

        if (skillOffer == null) {
            throw new ApiException("No skill offer found");
        }

        oldSession.setTitle(sessionDtoIn.getTitle());
        oldSession.setScheduledAt(sessionDtoIn.getScheduledAt());
        oldSession.setDurationMinutes(sessionDtoIn.getDurationMinutes());
        oldSession.setMode(sessionDtoIn.getMode());
        oldSession.setMeetingLink(sessionDtoIn.getMeetingLink());
        oldSession.setLocation(sessionDtoIn.getLocation());
        oldSession.setStatus(sessionDtoIn.getStatus());
        oldSession.setSkillOffer(skillOffer);

        sessionRepository.save(oldSession);
    }

    public void delete(Integer id) {
        Session oldSession = sessionRepository.findSessionById(id);

        if (oldSession == null) {
            throw new ApiException("No session found");
        }

sessionRepository.delete(oldSession);
    }

    public void createSession(Integer accountId, Integer offerId, SessionDtoIn sessionDtoIn) {
        accountAccessService.requireActive(accountId);

        SkillOffer skillOffer = skillOfferRepository.findSkillOfferById(offerId);

        if (skillOffer == null) {
            throw new ApiException("No skill offer found");
        }

        if (!"ACTIVE".equals(skillOffer.getStatus())) {
            throw new ApiException("Skill offer is not active");
        }

        requireProvider(accountId, skillOffer);

        Session session = new Session();
        session.setTitle(sessionDtoIn.getTitle());
        session.setScheduledAt(sessionDtoIn.getScheduledAt());
        session.setDurationMinutes(sessionDtoIn.getDurationMinutes());
        session.setMode(sessionDtoIn.getMode());
        session.setMeetingLink(sessionDtoIn.getMeetingLink());
        session.setLocation(sessionDtoIn.getLocation());
        session.setStatus("SCHEDULED");
        session.setSkillOffer(skillOffer);

        sessionRepository.save(session);

        List<Exchange> exchanges = exchangeRepository.findBySkillOffer_IdAndStatusIn(offerId, List.of("ACCEPTED", "IN_PROGRESS"));

        for (Exchange exchange : exchanges) {
            if (exchange.getLearningRequest() != null && exchange.getLearningRequest().getRequesterAccount() != null) {
                String learnerEmail = exchange.getLearningRequest().getRequesterAccount().getEmail();

                brevoEmailService.sendSessionEmail(
                        learnerEmail,
                        "New Session Created",
                        "A new session has been created."
                                + "\nSession: " + session.getTitle()
                                + "\nDate: " + session.getScheduledAt()
                                + "\nDuration: " + session.getDurationMinutes() + " minutes"
                                + "\nMeeting Link: " + session.getMeetingLink()
                );
            }
        }
    }

    public void joinSession(Integer accountId, Integer sessionId, Integer exchangeId) {
        accountAccessService.requireActive(accountId);
        Session session = sessionRepository.findSessionById(sessionId);

        if (session == null) {
            throw new ApiException("No session found");
        }

        Exchange exchange = exchangeRepository.findExchangeById(exchangeId);

        if (exchange == null) {
            throw new ApiException("No exchange found");
        }

        if (exchange.getLearningRequest() == null || exchange.getLearningRequest().getRequesterAccount() == null || !accountId.equals(exchange.getLearningRequest().getRequesterAccount().getId())) {
            throw new ApiException("Only the exchange learner can join this session");
        }

        if (!"ACCEPTED".equals(exchange.getStatus()) && !"IN_PROGRESS".equals(exchange.getStatus())) {
            throw new ApiException("Only accepted or in-progress exchanges can join a session");
        }

        if (session.getSkillOffer() == null || exchange.getSkillOffer() == null) {
            throw new ApiException("Session or exchange skill offer not found");
        }

        if (!session.getSkillOffer().getId().equals(exchange.getSkillOffer().getId())) {
            throw new ApiException("Exchange does not belong to this skill offer");
        }

        if (!"SCHEDULED".equals(session.getStatus())) {
            throw new ApiException("Session is not scheduled");
        }

        if (sessionParticipantRepository.findSessionParticipantBySession_IdAndExchange_Id(sessionId, exchangeId) != null) {
            throw new ApiException("You have already joined this session");
        }

        SessionParticipant participant = new SessionParticipant();
        participant.setSession(session);
        participant.setExchange(exchange);
        participant.setStatus("JOINED");

        sessionParticipantRepository.save(participant);
        String learnerEmail =
                exchange.getLearningRequest()
                        .getRequesterAccount()
                        .getEmail();

        brevoEmailService.sendSessionEmail(
                learnerEmail,
                "Session Joined",
                "You have successfully joined the session: "
                        + session.getTitle()
                        + "\nDate: "
                        + session.getScheduledAt()
                        + "\nMeeting Link: "
                        + session.getMeetingLink()
        );
    }

    public void updateAttendance(Integer accountId, Integer sessionId, Integer exchangeId, String status) {
        accountAccessService.requireActive(accountId);
        Session session = sessionRepository.findSessionById(sessionId);
        if (session == null) {
            throw new ApiException("No session found");
        }
        requireProvider(accountId, session.getSkillOffer());
        if (status == null || !(status.equals("ATTENDED") || status.equals("ABSENT") || status.equals("CANCELLED"))) {
            throw new ApiException("Status must be ATTENDED, ABSENT, or CANCELLED");
        }

        SessionParticipant participant = sessionParticipantRepository.findSessionParticipantBySession_IdAndExchange_Id(sessionId, exchangeId);

        if (participant == null) {
            throw new ApiException("No participant found for this session and exchange");
        }

        participant.setStatus(status);
        sessionParticipantRepository.save(participant);
    }

    public List<Session> getSessionsByOffer(Integer offerId) {
        SkillOffer skillOffer = skillOfferRepository.findSkillOfferById(offerId);

        if (skillOffer == null) {
            throw new ApiException("No skill offer found");
        }

        return sessionRepository.findBySkillOffer_Id(offerId);
    }

    public List<Session> getSessionsByExchange(Integer accountId, Integer exchangeId) {
        accountAccessService.requireActive(accountId);
        Exchange exchange = exchangeRepository.findExchangeById(exchangeId);

        if (exchange == null) {
            throw new ApiException("No exchange found");
        }

        if (exchange.getLearningRequest() == null || exchange.getLearningRequest().getRequesterAccount() == null || exchange.getLearningRequest().getProviderAccount() == null) {
            throw new ApiException("Exchange participants not found");
        }
        if (!accountId.equals(exchange.getLearningRequest().getRequesterAccount().getId()) && !accountId.equals(exchange.getLearningRequest().getProviderAccount().getId())) {
            throw new ApiException("Only exchange participants can view these sessions");
        }
        return sessionRepository.findDistinctBySessionParticipants_Exchange_Id(exchangeId);
    }



    private void requireProvider(Integer accountId, SkillOffer skillOffer) {
        if (skillOffer == null || skillOffer.getProviderAccount() == null || !accountId.equals(skillOffer.getProviderAccount().getId())) {
            throw new ApiException("Only the offer provider can manage this session");
        }
    }

}
