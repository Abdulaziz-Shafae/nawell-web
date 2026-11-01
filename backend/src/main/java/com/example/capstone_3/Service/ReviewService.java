package com.example.capstone_3.Service;

import com.example.capstone_3.Api.ApiException;
import com.example.capstone_3.DtoIn.ReviewDtoIn;
import com.example.capstone_3.DtoIn.CreateReviewDtoIn;
import com.example.capstone_3.Model.LearningRequest;
import com.example.capstone_3.Model.Account;
import com.example.capstone_3.Model.Exchange;
import com.example.capstone_3.Model.Review;
import com.example.capstone_3.Repository.AccountRepository;
import com.example.capstone_3.Repository.ExchangeRepository;
import com.example.capstone_3.Repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ReviewService {
    private final AccountAccessService accountAccessService;

    private final ReviewRepository reviewRepository;
    private final ExchangeRepository exchangeRepository;
    private final AccountRepository accountRepository;

    public List<Review> get() {
        return reviewRepository.findAll();
    }

    public List<Review> getByExchangeId(Integer exchangeId) {
        return reviewRepository.findReviewsByExchange_Id(exchangeId);
    }

    public List<Review> getByReviewedAccountId(Integer accountId) {
        if (accountRepository.findAccountById(accountId) == null) {
            throw new ApiException("No account found");
        }

        return reviewRepository.findReviewsByReviewedAccount_Id(accountId);
    }

    public Double getAverageRating(Integer accountId) {
        Account account = accountRepository.findAccountById(accountId);
        if (account == null) {
            throw new ApiException("No account found");
        }

        List<Review> reviews = reviewRepository.findReviewsByReviewedAccount_Id(accountId);
        return reviews.stream().mapToInt(Review::getRating).average().orElse(0.0);
    }

    public void add(Integer exchangeId, Integer reviewerId, CreateReviewDtoIn dto) {
        Account reviewer = accountAccessService.requireActive(reviewerId);
        Exchange exchange = exchangeRepository.findExchangeById(exchangeId);

        if (exchange == null) {
            throw new ApiException("No exchange found");
        }

        if (!"COMPLETED".equals(exchange.getStatus())) {
            throw new ApiException("Only completed exchanges can be reviewed");
        }

        LearningRequest request = exchange.getLearningRequest();
        if (request == null || request.getRequesterAccount() == null || request.getProviderAccount() == null) {
            throw new ApiException("Exchange participants not found");
        }
        Integer learnerId = request.getRequesterAccount().getId();
        Integer providerId = request.getProviderAccount().getId();
        if (!reviewerId.equals(learnerId) && !reviewerId.equals(providerId)) {
            throw new ApiException("Only exchange participants can write a review");
        }
        Integer otherAccountId = reviewerId.equals(learnerId) ? providerId : learnerId;
        if (!otherAccountId.equals(dto.getReviewedAccountId())) {
            throw new ApiException("You can only review the other exchange participant");
        }

        Account reviewed = accountRepository.findAccountById(dto.getReviewedAccountId());
        if (reviewed == null) {
            throw new ApiException("No reviewed account found");
        }

        if (reviewer.getId().equals(reviewed.getId())) {
            throw new ApiException("You cannot review your own account");
        }

        Review existingReview = reviewRepository.findReviewByExchange_IdAndReviewerAccount_Id(exchangeId, reviewerId);

        if (existingReview != null) {
            throw new ApiException("You have already reviewed this exchange");
        }

        Review review = new Review();
        review.setRating(dto.getRating());
        review.setComment(dto.getComment());
        review.setExchange(exchange);
        review.setReviewerAccount(reviewer);
        review.setReviewedAccount(reviewed);

        reviewRepository.save(review);
    }

    public void update(Integer id, Integer accountId, ReviewDtoIn dto) {
        Account reviewer = accountAccessService.requireActive(accountId);
        Review oldReview = reviewRepository.findReviewById(id);
        if (oldReview == null) throw new ApiException("No review found");
        if (oldReview.getReviewerAccount() == null ||
                !accountId.equals(oldReview.getReviewerAccount().getId())) {
            throw new ApiException("Only the review author can update this review");
        }
        Exchange exchange = oldReview.getExchange();
        if (exchange == null || !"COMPLETED".equals(exchange.getStatus())) {
            throw new ApiException("Only completed exchanges can be reviewed");
        }
        LearningRequest request = exchange.getLearningRequest();
        if (request == null || request.getRequesterAccount() == null ||
                request.getProviderAccount() == null || oldReview.getReviewedAccount() == null) {
            throw new ApiException("Exchange participants not found");
        }
        Integer learnerId = request.getRequesterAccount().getId();
        Integer providerId = request.getProviderAccount().getId();
        Integer expectedReviewedId = accountId.equals(learnerId) ? providerId
                : accountId.equals(providerId) ? learnerId : null;
        if (expectedReviewedId == null ||
                !expectedReviewedId.equals(oldReview.getReviewedAccount().getId())) {
            throw new ApiException("Review accounts do not belong to the exchange");
        }
        // Associations cannot be reassigned through an edit.
        if (!exchange.getId().equals(dto.getExchangeId()) ||
                !accountId.equals(dto.getReviewerAccountId()) ||
                !expectedReviewedId.equals(dto.getReviewedAccountId())) {
            throw new ApiException("Review participants and exchange cannot be changed");
        }
        oldReview.setRating(dto.getRating());
        oldReview.setComment(dto.getComment());
        reviewRepository.save(oldReview);
    }

    public void delete(Integer id) {
        Review review = reviewRepository.findReviewById(id);
        if (review == null) {
            throw new ApiException("No review found");
        }

        reviewRepository.delete(review);
    }
}
