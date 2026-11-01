package com.example.capstone_3.Controller;

import com.example.capstone_3.Api.ApiException;
import com.example.capstone_3.Api.ApiResponse;
import com.example.capstone_3.DtoIn.AccountDtoIn;
import com.example.capstone_3.DtoIn.LoginDtoIn;
import com.example.capstone_3.DtoIn.RegisterCompanyDtoIn;
import com.example.capstone_3.DtoIn.RegisterIndividualDtoIn;
import com.example.capstone_3.Service.AccountService;
import com.example.capstone_3.Service.EmailVerificationService;
import com.example.capstone_3.Service.PendingSignupService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/account")
public class AccountController {

    private final AccountService accountService;
    private final EmailVerificationService emailVerificationService;
    private final PendingSignupService pendingSignupService;

    @GetMapping("/get")
    public ResponseEntity<?> get(){
        return ResponseEntity.status(200).body(accountService.get());
    }


    @PostMapping("/add")
    public ResponseEntity<?> add(@RequestBody @Valid AccountDtoIn accountDtoIn){
        accountService.add(accountDtoIn);
        return ResponseEntity.status(200).body(new ApiResponse("account added"));
    }


    @PutMapping("/update/{id}")
    public ResponseEntity<?> update(@PathVariable Integer id, @RequestBody @Valid AccountDtoIn accountDtoIn){
        accountService.update(id, accountDtoIn);
        return ResponseEntity.status(200).body(new ApiResponse("account updated"));
    }


    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> delete(@PathVariable Integer id){
        accountService.delete(id);
        return ResponseEntity.status(200).body(new ApiResponse("account deleted"));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody @Valid LoginDtoIn loginDtoIn, HttpSession session, HttpServletRequest request) {

        Integer accountId = accountService.login(loginDtoIn);

        request.changeSessionId();
        session.setAttribute("accountId", accountId);

        return ResponseEntity.status(200).body(new ApiResponse("Logged in successfully"));
    }


    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpSession session){

        if((Integer) session.getAttribute("accountId") == null){
            throw new ApiException("Already logged out");
        }

        session.invalidate();

        return ResponseEntity.status(200).body(new ApiResponse("Logged out successfully"));
    }

    @PostMapping("/send-verification-email")
    public ResponseEntity<?> sendVerificationEmail(HttpSession session) {

        Integer accountId = (Integer) session.getAttribute("accountId");

        if (accountId == null) {
            throw new ApiException("Please log in first");
        }

        emailVerificationService.sendVerificationEmail(accountId);

        return ResponseEntity.status(200).body(new ApiResponse("Verification email submitted. Please check your inbox"));
    }

    @GetMapping("/verify-email")
    public ResponseEntity<?> verifyEmail(@RequestParam Integer accountId, @RequestParam String token) {

        emailVerificationService.verifyEmail(accountId, token);

        return ResponseEntity.status(200).body(new ApiResponse("Email verified successfully. You can close this page"));
    }

    @PostMapping("/signup/start/individual")
    public ResponseEntity<?> startIndividual(@RequestBody @Valid RegisterIndividualDtoIn dto) {
        pendingSignupService.startIndividual(dto);
        return ResponseEntity.ok(new ApiResponse("Check your email to complete registration"));
    }

    @PostMapping("/signup/start/company")
    public ResponseEntity<?> startCompany(@RequestBody @Valid RegisterCompanyDtoIn dto) {
        pendingSignupService.startCompany(dto);
        return ResponseEntity.ok(new ApiResponse("Check your email to complete registration"));
    }

    @PostMapping("/signup/confirm")
    public ResponseEntity<?> confirmSignup(@RequestBody java.util.Map<String,String> input, HttpServletRequest request) {
        String token=input.get("token");
        Integer id=pendingSignupService.confirm(token);
        request.getSession(true);
        request.changeSessionId();
        request.getSession().setAttribute("accountId",id);
        return ResponseEntity.ok(new ApiResponse("Email verified and account created successfully"));
    }

    @GetMapping("/profile")
    public ResponseEntity<?> getFullProfile( HttpSession session) {
        return ResponseEntity.status(200).body(accountService.getFullProfile((Integer) session.getAttribute("accountId")));
    }

    @GetMapping("/dashboard")
    public ResponseEntity<?> getDashboard(HttpSession session) {
        return ResponseEntity.status(200).body(accountService.getDashboard((Integer) session.getAttribute("accountId")));
    }

}