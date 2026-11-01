package com.example.capstone_3.Service;

import com.example.capstone_3.Api.ApiException;
import com.example.capstone_3.DtoIn.AccountDtoIn;
import com.example.capstone_3.DtoIn.LoginDtoIn;
import com.example.capstone_3.DtoIn.RegisterCompanyDtoIn;
import com.example.capstone_3.DtoIn.RegisterIndividualDtoIn;
import com.example.capstone_3.DtoOut.DashboardDtoOut;
import com.example.capstone_3.Model.Account;
import com.example.capstone_3.Model.CompanyProfile;
import com.example.capstone_3.Model.IndividualProfile;
import com.example.capstone_3.Repository.*;
import com.example.capstone_3.DtoOut.IndividualProfileDtoOut;
import com.example.capstone_3.DtoOut.CompanyProfileDtoOut;

import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AccountService {
    private final AccountAccessService accountAccessService;
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private final AccountRepository accountRepository;
    private final IndividualProfileRepository individualProfileRepository;
    private final LearningRequestRepository learningRequestRepository;
    private final SkillOfferRepository skillOfferRepository;
    private final ExchangeRepository exchangeRepository;
    private final TokenTransactionRepository tokenTransactionRepository;
    private final ReviewRepository reviewRepository;
    private final CompanyProfileRepository companyProfileRepository;

    public List<Account> get(){
        return accountRepository.findAll();
    }


    public void add(AccountDtoIn accountDtoIn){

        Account oldAccount = accountRepository.findAccountByEmail(accountDtoIn.getEmail());

        if(oldAccount != null){
            throw new ApiException("Email already exists");
        }

        Account account = new Account();

        account.setEmail(accountDtoIn.getEmail());
        account.setPassword(passwordEncoder.encode(accountDtoIn.getPassword()));
        account.setAccountType(accountDtoIn.getAccountType());

        account.setTokenBalance(3);
        account.setStatus("ACTIVE");
        account.setEmailVerified(false);
        account.setCreatedAt(LocalDateTime.now());

        accountRepository.save(account);
    }


    public void update(Integer id, AccountDtoIn accountDtoIn){

        Account oldAccount = accountRepository.findAccountById(id);

        if(oldAccount == null){
            throw new ApiException("No account found");
        }

        Account emailAccount = accountRepository.findAccountByEmail(accountDtoIn.getEmail());

        if(emailAccount != null && !emailAccount.getId().equals(id)){
            throw new ApiException("Email already exists");
        }

        oldAccount.setEmail(accountDtoIn.getEmail());
        oldAccount.setPassword(passwordEncoder.encode(accountDtoIn.getPassword()));
        oldAccount.setAccountType(accountDtoIn.getAccountType());

        accountRepository.save(oldAccount);
    }


    public void delete(Integer id){

        Account oldAccount = accountRepository.findAccountById(id);

        if(oldAccount == null){
            throw new ApiException("No account found");
        }

        accountRepository.delete(oldAccount);
    }


    @Transactional
    public Integer login(LoginDtoIn loginDtoIn) {

        Account oldAcc = accountRepository.findAccountByEmail(loginDtoIn.getEmail());
        String stored = oldAcc == null ? null : oldAcc.getPassword();
        boolean bcryptHash = stored != null && stored.matches("^\\$2[aby]\\$[0-9]{2}\\$.{53}$");
        boolean valid = stored != null && (bcryptHash
                ? passwordEncoder.matches(loginDtoIn.getPassword(), stored)
                : java.security.MessageDigest.isEqual(
                    stored.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                    loginDtoIn.getPassword().getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        if (!valid) throw new ApiException("Invalid email or password");
        // Migrate legacy plaintext credentials only after a successful login.
        if (!bcryptHash) {
            oldAcc.setPassword(passwordEncoder.encode(loginDtoIn.getPassword()));
            accountRepository.save(oldAcc);
        }

        accountAccessService.checkActive(oldAcc);

        return oldAcc.getId();
    }

    @Transactional
    public void registerIndividual(RegisterIndividualDtoIn dtoIn) {

        Account oldAccount = accountRepository.findAccountByEmail(dtoIn.getEmail());

        if (oldAccount != null) {
            throw new ApiException("Email already exists");
        }

        if (individualProfileRepository.existsByPhone(dtoIn.getPhone())) {
            throw new ApiException("Phone already exists");
        }

        Account account = new Account();

        account.setEmail(dtoIn.getEmail());
        account.setPassword(passwordEncoder.encode(dtoIn.getPassword()));
        account.setAccountType("INDIVIDUAL");
        account.setTokenBalance(3);
        account.setStatus("ACTIVE");
        account.setEmailVerified(false);
        account.setCreatedAt(LocalDateTime.now());

        IndividualProfile profile = new IndividualProfile();

        profile.setName(dtoIn.getName());
        profile.setPhone(dtoIn.getPhone());
        profile.setBio(dtoIn.getBio());
        profile.setCity(dtoIn.getCity());
        profile.setProfileImage(dtoIn.getProfileImage());

        profile.setAccount(account);
        account.setIndividualProfile(profile);

        accountRepository.save(account);
    }

    @Transactional
    public void registerCompany(RegisterCompanyDtoIn dtoIn) {

        Account oldAccount = accountRepository.findAccountByEmail(dtoIn.getEmail());

        if (oldAccount != null) {
            throw new ApiException("Email already exists");
        }

        if (companyProfileRepository.existsByPhone(dtoIn.getPhone())) {
            throw new ApiException("Phone already exists");
        }

        Account account = new Account();

        account.setEmail(dtoIn.getEmail());
        account.setPassword(passwordEncoder.encode(dtoIn.getPassword()));
        account.setAccountType("COMPANY");
        account.setTokenBalance(3);
        account.setStatus("ACTIVE");
        account.setEmailVerified(false);
        account.setCreatedAt(LocalDateTime.now());

        CompanyProfile profile = new CompanyProfile();

        profile.setName(dtoIn.getName());
        profile.setDescription(dtoIn.getDescription());
        profile.setPhone(dtoIn.getPhone());
        profile.setCity(dtoIn.getCity());
        profile.setLogo(dtoIn.getLogo());
        profile.setVerified(false);

        profile.setAccount(account);
        account.setCompanyProfile(profile);

        accountRepository.save(account);
    }

    @Transactional(readOnly = true)
    public Object getFullProfile(Integer accountId) {

        Account account = accountAccessService.requireActive(accountId);

        if ("INDIVIDUAL".equals(account.getAccountType())) {

            IndividualProfile profile = account.getIndividualProfile();

            if (profile == null) {
                throw new ApiException("Individual profile not found");
            }

            return new IndividualProfileDtoOut(account.getId(), account.getEmail(), account.getAccountType(), account.getTokenBalance(), account.getStatus(), account.getEmailVerified(), account.getCreatedAt(), profile.getName(), profile.getPhone(), profile.getBio(), profile.getCity(), profile.getProfileImage());
        }

        if ("COMPANY".equals(account.getAccountType())) {

            CompanyProfile profile = account.getCompanyProfile();

            if (profile == null) {
                throw new ApiException("Company profile not found");
            }

            return new CompanyProfileDtoOut(account.getId(), account.getEmail(), account.getAccountType(), account.getTokenBalance(), account.getStatus(), account.getEmailVerified(), account.getCreatedAt(), profile.getName(), profile.getDescription(), profile.getPhone(), profile.getCity(), profile.getLogo(), profile.getVerified());
        }

        if ("ADMIN".equals(account.getAccountType())) {
            return Map.of("accountId", account.getId(), "email", account.getEmail(), "accountType", "ADMIN", "name", "Administrator", "tokenBalance", account.getTokenBalance(), "emailVerified", account.getEmailVerified(), "status", account.getStatus());
        }
        throw new ApiException("Account type does not have an individual or company profile");
    }

    @Transactional
    public DashboardDtoOut getDashboard(Integer accountId) {

        Account account = accountAccessService.requireActive(accountId);

        Map<String, Long> exchangesByStatus = new LinkedHashMap<>();

        for (String status : List.of("PENDING", "ACCEPTED", "IN_PROGRESS", "COMPLETED", "CANCELLED", "DISPUTED")) {
            exchangesByStatus.put(status, exchangeRepository.countRelatedExchangesByStatus(accountId, status));
        }

        Double averageRating = reviewRepository.findAverageRating(accountId);

        if (averageRating != null) {
            averageRating = Math.round(averageRating * 100.0) / 100.0;
        }

        DashboardDtoOut dtoOut = new DashboardDtoOut();

        dtoOut.setAccountId(accountId);
        dtoOut.setTokenBalance(account.getTokenBalance());
        dtoOut.setReservedTokens(exchangeRepository.sumReservedTokens(accountId));
        dtoOut.setRequestsSent(learningRequestRepository.countByRequesterAccount_Id(accountId));
        dtoOut.setRequestsReceived(learningRequestRepository.countByProviderAccount_Id(accountId));
        dtoOut.setPublishedOffers(skillOfferRepository.countByProviderAccount_Id(accountId));
        dtoOut.setActiveOffers(skillOfferRepository.countByProviderAccount_IdAndStatus(accountId, "ACTIVE"));
        dtoOut.setTotalExchanges(exchangeRepository.countRelatedExchanges(accountId));
        dtoOut.setExchangesByStatus(exchangesByStatus);
        dtoOut.setTokensEarnedFromTeaching(tokenTransactionRepository.sumTeachingTokens(accountId));
        dtoOut.setReceivedReviews(reviewRepository.countByReviewedAccount_Id(accountId));
        dtoOut.setAverageRating(averageRating);

        return dtoOut;
    }

}
