package com.bloodcare.bloodcare.service;

import com.bloodcare.bloodcare.entity.BloodRequest;
import com.bloodcare.bloodcare.entity.BloodStock;
import com.bloodcare.bloodcare.entity.Donor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    public void sendResetLink(String toEmail, String link) {

        ensureEmailConfigured();

        SimpleMailMessage message = new SimpleMailMessage();

        message.setTo(toEmail);
        applyFrom(message);

        message.setSubject("BloodCare - Reset Password");

        message.setText(
                "Dear User,\n\n"
                        + "We received a request to reset your BloodCare account password.\n\n"
                        + "Please click the secure link below to continue:\n\n"
                        + link + "\n\n"
                        + "This link is valid for a limited time.\n\n"
                        + "If you did not request this password reset, you can safely ignore this email.\n\n"
                        + "Regards,\n"
                        + "BloodCare Support Team"
        );

        sendMail(message);
    }

    public void sendResetPasswordEmail(String email, String resetLink) {

        ensureEmailConfigured();

        SimpleMailMessage message = new SimpleMailMessage();

        message.setTo(email);
        applyFrom(message);

        message.setSubject("BloodCare - Password Reset Request");

        message.setText(
                "Dear User,\n\n"
                        + "We received a request to reset your BloodCare account password.\n\n"
                        + "Click the secure link below to reset your password:\n\n"
                        + resetLink + "\n\n"
                        + "For your security, this link will expire after some time.\n\n"
                        + "If you did not request this reset, please ignore this email.\n\n"
                        + "Thank you,\n"
                        + "BloodCare Support Team"
        );

        sendMail(message);
    }

    public void sendVisitApprovalEmail(String email, String name, String hospital) {

        ensureEmailConfigured();

        SimpleMailMessage message = new SimpleMailMessage();

        message.setTo(email);
        applyFrom(message);

        message.setSubject("BloodCare - Donation Visit Approved");

        message.setText(
                "Dear " + safe(name, "Donor") + ",\n\n"
                        + "We are pleased to inform you that your blood donation visit request "
                        + "has been approved successfully.\n\n"
                        + "Hospital: " + safe(hospital, "-") + "\n\n"
                        + "Please visit the hospital at your scheduled time and carry a valid ID proof if required.\n\n"
                        + "Thank you for your valuable contribution toward saving lives.\n\n"
                        + "Warm Regards,\n"
                        + "BloodCare Team"
        );

        sendMail(message);
    }

    public void sendVisitRejectionEmail(String email, String name, String hospital) {

        ensureEmailConfigured();

        SimpleMailMessage message = new SimpleMailMessage();

        message.setTo(email);
        applyFrom(message);

        message.setSubject("BloodCare - Donation Visit Update");

        message.setText(
                "Dear " + safe(name, "Donor") + ",\n\n"
                        + "We regret to inform you that your blood donation visit request "
                        + "has not been approved at this time.\n\n"
                        + "Hospital: " + safe(hospital, "-") + "\n\n"
                        + "You may try again later or contact support for additional information.\n\n"
                        + "Thank you for your willingness to help others through blood donation.\n\n"
                        + "Regards,\n"
                        + "BloodCare Team"
        );

        sendMail(message);
    }

    public void sendEligibleDonorEmail(String email, String name, String bloodGroup, String city) {

        ensureEmailConfigured();

        SimpleMailMessage message = new SimpleMailMessage();

        message.setTo(email);
        applyFrom(message);

        message.setSubject("BloodCare - Urgent Blood Request Match");

        message.setText(
                "Dear " + safe(name, "Donor") + ",\n\n"
                        + "A blood request matching your donor profile has been identified nearby.\n\n"
                        + "Blood Group: " + safe(bloodGroup, "-") + "\n"
                        + "City: " + safe(city, "-") + "\n\n"
                        + "Your support could help save a life.\n\n"
                        + "Please login to your BloodCare donor dashboard and respond if you are available to donate.\n\n"
                        + "Thank you for your kindness and support.\n\n"
                        + "Best Regards,\n"
                        + "BloodCare Team"
        );

        sendMail(message);
    }

    public void sendApprovedRequestMatchEmail(
            String email,
            String donorName,
            BloodRequest request,
            String donorBloodGroup,
            String donorCity
    ) {

        if (email == null || email.isBlank() || request == null) {
            return;
        }

        ensureEmailConfigured();

        SimpleMailMessage message = new SimpleMailMessage();

        message.setTo(email);
        applyFrom(message);

        message.setSubject("BloodCare - Approved Blood Request Needs Your Support");

        String body =
                "Dear " + safe(donorName, "Donor") + ",\n\n"
                        + "An approved blood request has been matched with your donor profile.\n\n"
                        + "Blood Group: " + safe(donorBloodGroup, "-") + "\n"
                        + "City: " + safe(donorCity, "-") + "\n\n"
                        + "We kindly request you to login to BloodCare and respond if you are available to donate.\n\n"
                        + "Your support can help save someone's life.\n\n"
                        + "Thank you,\n"
                        + "BloodCare Team";

        message.setText(body);

        sendMail(message);
    }

    public void sendDonorAcceptedEmail(
            String email,
            String receiverName,
            BloodRequest request,
            Donor donor
    ) {

        ensureEmailConfigured();

        SimpleMailMessage message = new SimpleMailMessage();

        message.setTo(email);
        applyFrom(message);

        message.setSubject("BloodCare - Donor Accepted Your Request");

        message.setText(
                "Dear " + safe(receiverName, "User") + ",\n\n"
                        + "Good news! A donor has accepted your blood request.\n\n"
                        + "Please login to your BloodCare account to view donor details "
                        + "and further instructions.\n\n"
                        + "We wish you a safe and speedy recovery.\n\n"
                        + "Regards,\n"
                        + "BloodCare Team"
        );

        sendMail(message);
    }

    public void sendBloodRequestApprovalEmail(
            String email,
            String receiverName,
            BloodRequest request
    ) {

        if (email == null || email.isBlank() || request == null) {
            return;
        }

        ensureEmailConfigured();

        SimpleMailMessage message = new SimpleMailMessage();

        message.setTo(email);
        applyFrom(message);

        message.setSubject("BloodCare - Blood Request Approved");

        message.setText(
                "Dear " + safe(receiverName, "User") + ",\n\n"
                        + "Your blood request has been reviewed and approved successfully.\n\n"
                        + "Our system is now searching for matching donors and available blood banks.\n\n"
                        + "We will notify you as soon as updates are available.\n\n"
                        + "Thank you for using BloodCare.\n\n"
                        + "Regards,\n"
                        + "BloodCare Team"
        );

        sendMail(message);
    }

    public void sendBloodBankAvailableEmail(
            String email,
            String receiverName,
            BloodRequest request,
            BloodStock stock
    ) {

        if (email == null || email.isBlank() || request == null || stock == null) {
            return;
        }

        ensureEmailConfigured();

        SimpleMailMessage message = new SimpleMailMessage();

        message.setTo(email);
        applyFrom(message);

        message.setSubject("BloodCare - Blood Available");

        message.setText(
                "Dear " + safe(receiverName, "User") + ",\n\n"
                        + "We are happy to inform you that the requested blood is currently available "
                        + "at a connected blood bank.\n\n"
                        + "Please login to your BloodCare account to view complete details and next steps.\n\n"
                        + "We hope this helps you receive timely support.\n\n"
                        + "Best Regards,\n"
                        + "BloodCare Team"
        );

        sendMail(message);
    }

    private void sendMail(SimpleMailMessage message) {

        try {

            System.out.println("==================================");
            System.out.println("Sending email...");
            System.out.println("TO = " + java.util.Arrays.toString(message.getTo()));
            System.out.println("FROM = " + message.getFrom());
            System.out.println("MAIL USER ENV = " + System.getenv("SPRING_MAIL_USERNAME"));
            System.out.println("MAIL PASS EXISTS = "
                    + (System.getenv("SPRING_MAIL_PASSWORD") != null));

            mailSender.send(message);

            System.out.println("MAIL SENT SUCCESSFULLY");
            System.out.println("==================================");

        } catch (Exception e) {

            System.out.println("==================================");
            System.out.println("MAIL ERROR");
            e.printStackTrace();
            System.out.println("==================================");

            throw new RuntimeException(e);
        }
    }

    public boolean isEmailConfigured() {
        return true;
    }

    private void ensureEmailConfigured() {

    }

    private void applyFrom(SimpleMailMessage message) {
        message.setFrom("bloodcares.app@gmail.com");
    }

    public String describeEmailFailure(Exception exception) {

        if (exception instanceof MailException) {
            return exception.getMessage();
        }

        return exception.getMessage() == null
                ? "Email sending failed"
                : exception.getMessage();
    }

    private String safe(String value, String fallback) {

        if (value == null || value.isBlank()) {
            return fallback;
        }

        return value;
    }
}
