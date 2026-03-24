package com.shivam.jobcopilot.controller;

import com.shivam.jobcopilot.dto.AuthResponse;
import com.shivam.jobcopilot.dto.LoginRequest;
import com.shivam.jobcopilot.dto.SignupRequest;
import com.shivam.jobcopilot.entity.User;
import com.shivam.jobcopilot.repository.UserRepository;
import com.shivam.jobcopilot.security.JwtUtil;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthController(UserRepository userRepository,
                          PasswordEncoder passwordEncoder,
                          JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/signup")
    public ResponseEntity<AuthResponse> signup(@RequestBody SignupRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }

        User user = new User();
        user.setEmail(request.email());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setName(request.name());
        User saved = userRepository.save(user);

        String token = jwtUtil.generateToken(saved.getId());
        return ResponseEntity.ok(new AuthResponse(token, saved.getId(), saved.getEmail(), saved.getName()));
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

        String token = jwtUtil.generateToken(user.getId());
        return ResponseEntity.ok(new AuthResponse(token, user.getId(), user.getEmail(), user.getName()));
    }
}
