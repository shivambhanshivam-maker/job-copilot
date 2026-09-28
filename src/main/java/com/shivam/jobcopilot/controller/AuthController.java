package com.shivam.jobcopilot.controller;

import com.shivam.jobcopilot.dto.AuthResponse;
import com.shivam.jobcopilot.dto.LoginRequest;
import com.shivam.jobcopilot.dto.SignupRequest;
import com.shivam.jobcopilot.entity.ApplicationUpdate;
import com.shivam.jobcopilot.entity.JobApplication;
import com.shivam.jobcopilot.entity.User;
import com.shivam.jobcopilot.repository.AllowedEmailRepository;
import com.shivam.jobcopilot.repository.AdvisorRepository;
import com.shivam.jobcopilot.repository.JobApplicationRepository;
import com.shivam.jobcopilot.repository.UserRepository;
import com.shivam.jobcopilot.security.JwtUtil;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final AllowedEmailRepository allowedEmailRepository;
    private final AdvisorRepository advisorRepository;
    private final JobApplicationRepository jobApplicationRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthController(UserRepository userRepository,
                          AllowedEmailRepository allowedEmailRepository,
                          AdvisorRepository advisorRepository,
                          JobApplicationRepository jobApplicationRepository,
                          PasswordEncoder passwordEncoder,
                          JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.allowedEmailRepository = allowedEmailRepository;
        this.advisorRepository = advisorRepository;
        this.jobApplicationRepository = jobApplicationRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/signup")
    public ResponseEntity<AuthResponse> signup(@RequestBody SignupRequest request) {
        if (!allowedEmailRepository.existsByEmail(request.email())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        if (userRepository.existsByEmail(request.email())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }

        User user = new User();
        user.setEmail(request.email());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setName(request.name());
        User saved = userRepository.save(user);
        seedSampleApplications(saved);

        String token = jwtUtil.generateToken(saved.getId());
        return ResponseEntity.ok(new AuthResponse(token, saved.getId(), saved.getEmail(), saved.getName(), false, "STUDENT"));
    }

    private void seedSampleApplications(User user) {
        LocalDateTime now = LocalDateTime.now();
        String sampleNote = "[Sample] This is a sample application. Feel free to delete it.";

        // 1. Strategy & Operations Manager at Revolut — Rejected (full progression)
        JobApplication app1 = new JobApplication();
        app1.setUserId(user.getId());
        app1.setCompany("Revolut");
        app1.setJobTitle("Strategy & Operations Manager");
        app1.setRoleCategory("Strategy & Operations");
        app1.setApplicationStatus("Rejected");
        app1.setRecruiterName("Sarah Mitchell");
        app1.setRecruiterEmail("s.mitchell@revolut.com");
        app1.setCreatedAt(now.minusDays(25));
        app1.setFirstRespondedAt(now.minusDays(18));
        app1.setNotes(sampleNote);
        app1.getUpdates().addAll(List.of(
                new ApplicationUpdate(now.minusDays(25), "Applied via company website."),
                new ApplicationUpdate(now.minusDays(18), "Recruiter reached out to schedule a screening call."),
                new ApplicationUpdate(now.minusDays(12), "Completed first-round interview with hiring manager."),
                new ApplicationUpdate(now.minusDays(3), "Received rejection email after final round.")
        ));

        // 2. Product Manager at Google — Interview
        JobApplication app2 = new JobApplication();
        app2.setUserId(user.getId());
        app2.setCompany("Google");
        app2.setJobTitle("Product Manager");
        app2.setRoleCategory("Product Management");
        app2.setApplicationStatus("Interview");
        app2.setRecruiterName("James Park");
        app2.setRecruiterEmail("jpark@google.com");
        app2.setInterviewDate(now.plusDays(5));
        app2.setCreatedAt(now.minusDays(14));
        app2.setFirstRespondedAt(now.minusDays(7));
        app2.setNotes(sampleNote);
        app2.getUpdates().addAll(List.of(
                new ApplicationUpdate(now.minusDays(14), "Applied through LinkedIn."),
                new ApplicationUpdate(now.minusDays(7), "Recruiter scheduled a technical screen."),
                new ApplicationUpdate(now.minusDays(1), "Interview confirmed for next week.")
        ));

        // 3. Software Engineer II at Microsoft — Applied (fresh)
        JobApplication app3 = new JobApplication();
        app3.setUserId(user.getId());
        app3.setCompany("Microsoft");
        app3.setJobTitle("Software Engineer II");
        app3.setRoleCategory("Software Engineering");
        app3.setCreatedAt(now.minusDays(2));
        app3.setApplicationStatus("Applied");
        app3.setNotes(sampleNote);
        app3.getUpdates().addAll(List.of(
                new ApplicationUpdate(now.minusDays(2), "Applied via Microsoft careers portal.")
        ));

        jobApplicationRepository.saveAll(List.of(app1, app2, app3));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
        User user = userRepository.findByEmail(request.email()).orElse(null);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        boolean advisorAccess = advisorRepository.existsByUserIdAndIsActiveTrue(user.getId());
        String accountMode = normalizeAccountMode(request.accountMode());

        if ("ADVISOR".equals(accountMode) && !advisorAccess) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        if ("STUDENT".equals(accountMode) && advisorAccess) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        String token = jwtUtil.generateToken(user.getId());
        return ResponseEntity.ok(new AuthResponse(token, user.getId(), user.getEmail(), user.getName(), advisorAccess, accountMode));
    }

    private String normalizeAccountMode(String accountMode) {
        if (accountMode == null || accountMode.isBlank()) {
            return "STUDENT";
        }
        return "ADVISOR".equalsIgnoreCase(accountMode) ? "ADVISOR" : "STUDENT";
    }
}
