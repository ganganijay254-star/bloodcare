package com.bloodcare.bloodcare.service;

import com.bloodcare.bloodcare.entity.BloodRequest;
import com.bloodcare.bloodcare.entity.BloodStock;
import com.bloodcare.bloodcare.entity.Donor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

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
                "Hello,\n\n"
                        + "Click the link below to reset your password:\n\n"
                        + link + "\n\n"
                        + "If you did not request this, please ignore this email.\n\n"
                        + "Regards,\nBloodCare Team"
        );

        try {

            System.out.println("==================================");
            System.out.println("Sending email...");
            System.out.println("TO = " + java.util.Arrays.toString(message.getTo()));
            System.out.println("FROM = " + message.getFrom());
            System.out.println("MAIL USER ENV = " + System.getenv("SPRING_MAIL_USERNAME"));
            System.out.println("MAIL PASS EXISTS = " + (System.getenv("SPRING_MAIL_PASSWORD") != null));

            mailSender.send(message);

            System.out.println("MAIL SENT SUCCESSFULLY");
            System.out.println("==================================");

        } catch (Exception e) {

            System.out.println("==================================");
            System.out.println("MAIL ERROR");
            e.printStackTrace();
            System.out.println("==================================");
        }
    }

    public void sendResetPasswordEmail(String email, String resetLink) {

        ensureEmailConfigured();

        SimpleMailMessage message = new SimpleMailMessage();

        message.setTo(email);
        applyFrom(message);

        message.setSubject("BloodCare - Password Reset Request");

        message.setText(
                "Dear User,\n\n"
                        + "We received a request to reset your password.\n\n"
                        + "Click the link below to reset it:\n\n"
                        + resetLink + "\n\n"
                        + "If you did not request this, please ignore this email.\n\n"
                        + "BloodCare Support Team"
        );

        try {

            System.out.println("==================================");
            System.out.println("Sending email...");
            System.out.println("TO = " + java.util.Arrays.toString(message.getTo()));
            System.out.println("FROM = " + message.getFrom());
            System.out.println("MAIL USER ENV = " + System.getenv("SPRING_MAIL_USERNAME"));
            System.out.println("MAIL PASS EXISTS = " + (System.getenv("SPRING_MAIL_PASSWORD") != null));

            mailSender.send(message);

            System.out.println("MAIL SENT SUCCESSFULLY");
            System.out.println("==================================");

        } catch (Exception e) {

            System.out.println("==================================");
            System.out.println("MAIL ERROR");
            e.printStackTrace();
            System.out.println("==================================");
        }
    }

    public void sendVisitApprovalEmail(String email, String name, String hospital) {

        ensureEmailConfigured();

        SimpleMailMessage message = new SimpleMailMessage();

        message.setTo(email);
        applyFrom(message);

        message.setSubject("BloodCare - Donation Visit Approved");

        message.setText(
                "Dear " + name + ",\n\n"
                        + "Your blood donation visit request has been APPROVED.\n\n"
                        + "Hospital: " + hospital + "\n\n"
                        + "Please visit on your scheduled time.\n\n"
                        + "Thank you for saving lives.\n\n"
                        + "BloodCare Team"
        );

        try {

            System.out.println("==================================");
            System.out.println("Sending email...");
            System.out.println("TO = " + java.util.Arrays.toString(message.getTo()));
            System.out.println("FROM = " + message.getFrom());
            System.out.println("MAIL USER ENV = " + System.getenv("SPRING_MAIL_USERNAME"));
            System.out.println("MAIL PASS EXISTS = " + (System.getenv("SPRING_MAIL_PASSWORD") != null));

            mailSender.send(message);

            System.out.println("MAIL SENT SUCCESSFULLY");
            System.out.println("==================================");

        } catch (Exception e) {

            System.out.println("==================================");
            System.out.println("MAIL ERROR");
            e.printStackTrace();
            System.out.println("==================================");
        }
    }

    public void sendVisitRejectionEmail(String email, String name, String hospital) {

        ensureEmailConfigured();

        SimpleMailMessage message = new SimpleMailMessage();

        message.setTo(email);
        applyFrom(message);

        message.setSubject("BloodCare - Donation Visit Update");

        message.setText(
                "Dear " + name + ",\n\n"
                        + "We regret to inform you that your blood donation visit request\n"
                        + "for hospital: " + hospital + " has been REJECTED.\n\n"
                        + "You may try again later or contact support.\n\n"
                        + "Thank you.\n"
                        + "BloodCare Team"
        );

        try {

            System.out.println("==================================");
            System.out.println("Sending email...");
            System.out.println("TO = " + java.util.Arrays.toString(message.getTo()));
            System.out.println("FROM = " + message.getFrom());
            System.out.println("MAIL USER ENV = " + System.getenv("SPRING_MAIL_USERNAME"));
            System.out.println("MAIL PASS EXISTS = " + (System.getenv("SPRING_MAIL_PASSWORD") != null));

            mailSender.send(message);

            System.out.println("MAIL SENT SUCCESSFULLY");
            System.out.println("==================================");

        } catch (Exception e) {

            System.out.println("==================================");
            System.out.println("MAIL ERROR");
            e.printStackTrace();
            System.out.println("==================================");
        }
    }

    public boolean isEmailConfigured() {
        return true;
    }

    private void ensureEmailConfigured() {

    }

    private void applyFrom(SimpleMailMessage message) {
        message.setFrom("a8461d001@smtp-brevo.com");
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
        if (value == null || value.isBlank()) return fallback;
        return value;
    }
}
