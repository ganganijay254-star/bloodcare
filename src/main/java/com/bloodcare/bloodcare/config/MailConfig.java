package com.bloodcare.bloodcare.config;

import java.util.Properties;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

@Configuration
public class MailConfig {

    @Value("${spring.mail.host:smtp-relay.brevo.com}")
    private String host;

    @Value("${spring.mail.port:587}")
    private int port;

    @Value("${spring.mail.username:}")
    private String username;

    @Value("${spring.mail.password:}")
    private String password;

    @Value("${spring.mail.default-encoding:UTF-8}")
    private String defaultEncoding;

    @Value("${spring.mail.properties.mail.transport.protocol:smtp}")
    private String transportProtocol;

    @Value("${spring.mail.properties.mail.smtp.auth:true}")
    private String smtpAuth;

    @Value("${spring.mail.properties.mail.smtp.starttls.enable:true}")
    private String startTlsEnable;

    @Value("${spring.mail.properties.mail.smtp.starttls.required:true}")
    private String startTlsRequired;

    @Value("${spring.mail.properties.mail.smtp.ssl.trust:smtp-relay.brevo.com}")
    private String sslTrust;

    @Value("${spring.mail.properties.mail.smtp.connectiontimeout:5000}")
    private String connectionTimeout;

    @Value("${spring.mail.properties.mail.smtp.timeout:5000}")
    private String timeout;

    @Value("${spring.mail.properties.mail.smtp.writetimeout:5000}")
    private String writeTimeout;

    @Bean
    @Primary
    public JavaMailSender javaMailSender() {

        JavaMailSenderImpl sender = new JavaMailSenderImpl();

        sender.setHost(normalize(host));
        sender.setPort(port);
        sender.setUsername(normalize(username));
        sender.setPassword(normalizePassword(password));
        sender.setDefaultEncoding(normalize(defaultEncoding));

        Properties props = sender.getJavaMailProperties();

        props.put("mail.transport.protocol", normalize(transportProtocol));
        props.put("mail.smtp.auth", normalize(smtpAuth));
        props.put("mail.smtp.starttls.enable", normalize(startTlsEnable));
        props.put("mail.smtp.starttls.required", normalize(startTlsRequired));
        props.put("mail.smtp.ssl.trust", normalize(sslTrust));
        props.put("mail.smtp.connectiontimeout", normalize(connectionTimeout));
        props.put("mail.smtp.timeout", normalize(timeout));
        props.put("mail.smtp.writetimeout", normalize(writeTimeout));

        return sender;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private String normalizePassword(String value) {
        return value == null ? "" : value.replaceAll("\\s+", "");
    }
}
