package com.example.capstone_3.Controller;

import com.example.capstone_3.Api.ApiResponse;
import com.example.capstone_3.DtoIn.ReviewDtoIn;
import com.example.capstone_3.DtoIn.CreateReviewDtoIn;
import jakarta.servlet.http.HttpSession;
import com.example.capstone_3.Service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/review")
public class ReviewController {

    private final ReviewService reviewService;

    @GetMapping("/get")
    public ResponseEntity<?> get() {
        return ResponseEntity.ok(reviewService.get());
    }

    @PostMapping("/add/{exchangeId}")
    public ResponseEntity<?> add(@PathVariable Integer exchangeId, @RequestBody @Valid CreateReviewDtoIn dto, HttpSession session) {
        reviewService.add(exchangeId, (Integer) session.getAttribute("accountId"), dto);
        return ResponseEntity.ok(new ApiResponse("Review added successfully"));
    }

    @GetMapping("/account/{accountId}")
    public ResponseEntity<?> getByAccountId(@PathVariable Integer accountId) {

        return ResponseEntity.ok(reviewService.getByReviewedAccountId(accountId));
    }

    @GetMapping("/account/{accountId}/average")
    public ResponseEntity<?> getAverageRating(@PathVariable Integer accountId) {

        return ResponseEntity.ok(reviewService.getAverageRating(accountId));
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<?> update(@PathVariable Integer id, @RequestBody @Valid ReviewDtoIn dto, HttpSession session) {
        reviewService.update(id, (Integer) session.getAttribute("accountId"), dto);
        return ResponseEntity.ok(new ApiResponse("Review updated successfully"));
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> delete(@PathVariable Integer id) {
        reviewService.delete(id);
        return ResponseEntity.ok(new ApiResponse("Review deleted successfully"));
    }
}
