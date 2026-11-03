package com.mozantech.mozantech_test.component;

import com.mozantech.mozantech_test.dto.event.OrderCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@Slf4j
@RequiredArgsConstructor
public class OrderNotificationListener {

    private final JavaMailSender mailSender;
    @Value("${app.mail.from}") private String from;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOrderCompleted(OrderCompletedEvent e) {
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setFrom(from);
            msg.setTo(e.getUserEmail());
            msg.setSubject("Your order #" + e.getOrderId() + " is complete");
            msg.setText("Olá " + e.getUserName() + ",\n\na tua encomenda de " + e.getQuantity()
                    + "x " + e.getItemName() + " foi realizada... cumprimentos.😊");
            mailSender.send(msg);
            log.info("EMAIL SENT to={} order={}", e.getUserEmail(), e.getOrderId());
        } catch (Exception ex) {
            log.error("ERROR sending email to={} order={}", e.getUserEmail(), e.getOrderId(), ex);
        }
    }
}
