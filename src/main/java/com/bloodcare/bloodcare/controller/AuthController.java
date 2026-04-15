package com.bloodcare.bloodcare.controller;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import com.bloodcare.bloodcare.entity.User;
import com.bloodcare.bloodcare.repository.UserRepository;
import com.bloodcare.bloodcare.service.EmailService;
import com.bloodcare.bloodcare.dto.LoginRequest;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmailService emailService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Value("${app.base-url:http://localhost:8082}")
    private String appBaseUrl;

    /* =====================
       SIGNUP (USER ONLY)
    ===================== */
    @PostMapping("/signup")
    public ResponseEntity<?> signup(@RequestBody User user) {
        String email = normalizeEmail(user.getEmail());
        String mobile = normalizeMobile(user.getMobile());
        String password = user.getPassword();

        if (user.getName() == null || user.getName().trim().length() < 3) {
            return ResponseEntity.badRequest().body(message("Please enter a valid full name"));
        }

        if (email == null) {
            return ResponseEntity.badRequest().body(message("Please enter a valid email address"));
        }

        if (mobile == null) {
            return ResponseEntity.badRequest().body(message("Mobile number must be exactly 10 digits"));
        }

        if (password == null || password.isBlank()) {
            return ResponseEntity.badRequest().body(message("Password is required"));
        }

        if (userRepository.findByEmail(email) != null) {
            return ResponseEntity.status(409).body(message("Email already exists"));
        }

        if (userRepository.findByMobile(mobile) != null) {
            return ResponseEntity.status(409).body(message("Mobile number already exists"));
        }

        user.setName(user.getName().trim());
        user.setEmail(email);
        user.setMobile(mobile);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole((user.getRole() == null || user.getRole().isBlank()) ? "USER" : user.getRole().trim().toUpperCase());

        try {
            User savedUser = userRepository.save(user);
            savedUser.setPassword(null);

            return ResponseEntity.ok(savedUser);
        } catch (DataIntegrityViolationException ex) {
            return ResponseEntity.status(409).body(message("Mobile number already exists"));
        }
    }

    /* =====================
       LOGIN (USER ONLY)
    ===================== */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request,
                                   HttpSession session) {

        User user = userRepository.findByEmail(request.getEmail());

        if (user == null ||
            !passwordEncoder.matches(request.getPassword(), user.getPassword())) {

            return ResponseEntity.status(401).body(message("Invalid credentials"));
        }
        
        if (user.isBlocked()) {
            if (user.isBlockedByAdmin()) {
                return ResponseEntity.status(403).body(message("Your account has been blocked by admin"));
            }
            user.setBlocked(false);
            user.setBlockedByAdmin(false);
            userRepository.save(user);
        }

        user.setPassword(null);
        session.setAttribute("user", user);

        return ResponseEntity.ok(user);
    }

    /* =====================
       USER SESSION CHECK
    ===================== */
    @GetMapping("/check-session")
    public ResponseEntity<?> checkSession(HttpSession session) {

        User user = (User) session.getAttribute("user");

        if (user == null) {
            return ResponseEntity.status(401).body(message("Not logged in"));
        }
        
        User latestUser = userRepository.findByEmail(user.getEmail());
        if (latestUser == null) {
            session.removeAttribute("user");
            return ResponseEntity.status(401).body(message("Not logged in"));
        }

        if (latestUser.isBlocked()) {
            if (latestUser.isBlockedByAdmin()) {
                session.removeAttribute("user");
                return ResponseEntity.status(401).body(message("Not logged in"));
            }
            latestUser.setBlocked(false);
            latestUser.setBlockedByAdmin(false);
            latestUser = userRepository.save(latestUser);
        }
        
        latestUser.setPassword(null);

        return ResponseEntity.ok(latestUser);
    }

    /* =====================
       USER LOGOUT
    ===================== */
    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpSession session) {

        session.removeAttribute("user");
        return ResponseEntity.ok("Logged out successfully");
    }

    /* =====================
       FORGOT PASSWORD
    ===================== */
    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(
            @RequestParam String email,
            HttpServletRequest request) {
        String normalizedEmail = normalizeEmail(email);
        if (normalizedEmail == null) {
            return ResponseEntity.badRequest().body(message("Please enter a valid email address"));
        }

        User user = userRepository.findByEmail(normalizedEmail);

        if (user == null) {
            return ResponseEntity.badRequest().body(message("Email not registered"));
        }

        String token = UUID.randomUUID().toString();
        user.setResetToken(token);
        user.setResetTokenExpiry(LocalDateTime.now().plusMinutes(15));

        userRepository.save(user);
        String requestBaseUrl = buildBaseUrl(request);
        String baseUrl = requestBaseUrl == null || requestBaseUrl.isBlank()
                ? normalizeBaseUrl(appBaseUrl)
                : requestBaseUrl;
        String resetLink = baseUrl + "/reset-password?token=" + token;

        try {
            emailService.sendResetLink(user.getEmail(), resetLink);
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(503).body(message("Password reset email is not configured right now. Please contact support."));
        } catch (Exception ex) {
            return ResponseEntity.status(500).body(message(emailService.describeEmailFailure(ex)));
        }

        return ResponseEntity.ok(message("Reset link sent to email"));
    }

    /* =====================
       RESET PASSWORD
    ===================== */
    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody Map<String, String> req) {

        String token = req.get("token");
        String newPassword = req.get("password");

        User user = userRepository.findByResetToken(token);

        if (user == null) {
            return ResponseEntity.badRequest().body(message("Invalid token"));
        }

        if (user.getResetTokenExpiry().isBefore(LocalDateTime.now())) {
            return ResponseEntity.badRequest().body(message("Token expired"));
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setResetToken(null);
        user.setResetTokenExpiry(null);

        userRepository.save(user);

        return ResponseEntity.ok(message("Password updated successfully"));
    }

    private Map<String, String> message(String value) {
        Map<String, String> response = new LinkedHashMap<>();
        response.put("message", value);
        return response;
    }

    private String normalizeEmail(String value) {
        if (value == null) {
            return null;
        }

        String normalized = value.trim().toLowerCase();
        return normalized.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$") ? normalized : null;
    }

    private String normalizeMobile(String value) {
        if (value == null) {
            return null;
        }

        String digitsOnly = value.replaceAll("\\D", "");
        return digitsOnly.matches("\\d{10}") ? digitsOnly : null;
    }

    private String buildBaseUrl(HttpServletRequest request) {
        String forwardedProto = request.getHeader("X-Forwarded-Proto");
        String forwardedHost = request.getHeader("X-Forwarded-Host");
        if (forwardedHost != null && !forwardedHost.isBlank()) {
            String scheme = (forwardedProto == null || forwardedProto.isBlank()) ? request.getScheme() : forwardedProto;
            return normalizeBaseUrl(scheme + "://" + forwardedHost);
        }

        String scheme = request.getScheme();
        String serverName = request.getServerName();
        int serverPort = request.getServerPort();
        String baseUrl = scheme + "://" + serverName;
        if ((scheme.equals("http") && serverPort != 80)
                || (scheme.equals("https") && serverPort != 443)) {
            baseUrl += ":" + serverPort;
        }
        return normalizeBaseUrl(baseUrl);
    }

    private String normalizeBaseUrl(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}
