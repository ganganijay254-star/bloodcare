package com.bloodcare.bloodcare.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import java.net.InetAddress;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bloodcare.bloodcare.entity.Certificate;
import com.bloodcare.bloodcare.entity.Donor;
import com.bloodcare.bloodcare.entity.User;
import com.bloodcare.bloodcare.entity.VisitRequest;
import com.bloodcare.bloodcare.repository.CertificateRepository;
import com.bloodcare.bloodcare.repository.DonorRepository;
import com.bloodcare.bloodcare.repository.VisitRequestRepository;

@RestController
@RequestMapping("/api/certificate")
public class CertificateController {

    @Value("${app.base-url:}")
    private String configuredBaseUrl;

    @Autowired
    private CertificateRepository certificateRepository;

    @Autowired
    private DonorRepository donorRepository;

    @Autowired
    private VisitRequestRepository visitRequestRepository;

    @GetMapping("/my-certificates")
    public ResponseEntity<?> getMyCertificates(HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("success", false, "message", "Please login first."));
        }

        try {
            ensureCertificatesForApprovedVisits(user);
            List<Certificate> certificates = certificateRepository.findByDonorUserOrderByCreatedDateDesc(user);
            return ResponseEntity.ok(certificates);
        } catch (Exception e) {
            Donor donor = donorRepository.findByUser(user);
            if (donor != null) {
                ensureCertificatesForApprovedVisits(user);
                List<Certificate> certificates = certificateRepository.findByDonor(donor);
                certificates.sort(Comparator.comparing(Certificate::getCreatedDate, Comparator.nullsLast(Comparator.reverseOrder())));
                return ResponseEntity.ok(certificates);
            }
            return ResponseEntity.ok(List.of());
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getCertificateById(@PathVariable Long id) {
        Certificate certificate = certificateRepository.findById(id).orElse(null);
        if (certificate == null) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Certificate not found."));
        }
        return ResponseEntity.ok(certificate);
    }

    @GetMapping("/verify/{certificateNumber}")
    public ResponseEntity<?> verifyCertificate(@PathVariable String certificateNumber) {
        Certificate certificate = certificateRepository.findByCertificateNumber(certificateNumber);
        if (certificate == null) {
            return ResponseEntity.status(404).body(Map.of(
                    "success", false,
                    "message", "Certificate not found for the supplied verification code."));
        }
        return ResponseEntity.ok(toCertificatePayload(certificate));
    }

    @GetMapping("/by-number/{certificateNumber}")
    public ResponseEntity<?> getCertificateByNumber(@PathVariable String certificateNumber) {
        Certificate certificate = certificateRepository.findByCertificateNumber(certificateNumber);
        if (certificate == null) {
            return ResponseEntity.status(404).body(Map.of("success", false, "message", "Certificate not found."));
        }
        return ResponseEntity.ok(toCertificatePayload(certificate));
    }

    @GetMapping("/public-base-url")
    public ResponseEntity<?> getPublicBaseUrl(HttpServletRequest request) {
        if (configuredBaseUrl != null && !configuredBaseUrl.isBlank()) {
            return ResponseEntity.ok(trimTrailingSlash(configuredBaseUrl));
        }

        String forwardedProto = headerValue(request, "X-Forwarded-Proto");
        String forwardedHost = headerValue(request, "X-Forwarded-Host");
        String forwardedPort = headerValue(request, "X-Forwarded-Port");

        String scheme = forwardedProto != null ? forwardedProto : request.getScheme();
        String host = forwardedHost != null ? forwardedHost : request.getServerName();
        int port = parsePort(forwardedPort, request.getServerPort());

        if (host != null && host.contains(",")) {
            host = host.split(",")[0].trim();
        }

        if (host != null && host.contains(":")) {
            String[] parts = host.split(":", 2);
            host = parts[0];
            if (forwardedPort == null && parts.length > 1) {
                port = parsePort(parts[1], port);
            }
        }

        if ("localhost".equalsIgnoreCase(host) || "127.0.0.1".equals(host)) {
            try {
                host = InetAddress.getLocalHost().getHostAddress();
            } catch (Exception ignored) {
                host = request.getServerName();
            }
        }

        StringBuilder base = new StringBuilder();
        base.append(scheme).append("://").append(host);
        if (!(("http".equalsIgnoreCase(scheme) && port == 80) || ("https".equalsIgnoreCase(scheme) && port == 443))) {
            base.append(":").append(port);
        }

        return ResponseEntity.ok(base.toString());
    }

    private void ensureCertificatesForApprovedVisits(User user) {
        Donor donor = donorRepository.findByUser(user);
        if (donor == null) {
            return;
        }

        List<VisitRequest> approvedVisits = visitRequestRepository.findByUserAndStatus(user, "APPROVED");
        for (VisitRequest visit : approvedVisits) {
            if ("RECEIVER".equalsIgnoreCase(visit.getRequestType())) {
                continue;
            }
            if (certificateRepository.existsByVisitRequest(visit)) {
                continue;
            }

            String certificateNumber = "CERT-" + visit.getId() + "-" + System.currentTimeMillis();

            Certificate certificate = new Certificate();
            certificate.setDonor(donor);
            certificate.setVisitRequest(visit);
            certificate.setCertificateNumber(certificateNumber);
            certificate.setHospitalName(visit.getHospitalName());
            certificate.setUnits(visit.getUnits());
            certificate.setDonationDate(visit.getVisitDate() != null
                    ? visit.getVisitDate()
                    : (visit.getRequestDate() != null ? visit.getRequestDate() : LocalDate.now()));
            certificate.setCreatedDate(LocalDateTime.now());
            certificate.setQrCode("/verify-certificate?cert=" + certificateNumber);
            certificateRepository.save(certificate);
        }
    }

    private Map<String, Object> toCertificatePayload(Certificate certificate) {
        Donor donor = certificate.getDonor();
        User donorUser = donor != null ? donor.getUser() : null;

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("success", true);
        payload.put("id", certificate.getId());
        payload.put("certificateNumber", defaultString(certificate.getCertificateNumber()));
        payload.put("hospitalName", defaultString(certificate.getHospitalName()));
        payload.put("units", certificate.getUnits());
        payload.put("donationDate", certificate.getDonationDate());
        payload.put("createdDate", certificate.getCreatedDate());
        payload.put("qrCode", defaultString(certificate.getQrCode()));
        payload.put("donorName", donorUser != null ? defaultString(donorUser.getName()) : "Donor");
        payload.put("bloodGroup", donor != null ? defaultString(donor.getBloodGroup()) : "");
        return payload;
    }

    private String trimTrailingSlash(String value) {
        return value.replaceAll("/+$", "");
    }

    private String headerValue(HttpServletRequest request, String name) {
        String value = request.getHeader(name);
        return value == null || value.isBlank() ? null : value.trim();
    }

    private int parsePort(String value, int fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private String defaultString(String value) {
        return value == null ? "" : value;
    }
}
