package com.gist.mathis.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import com.gist.mathis.model.entity.MathisMessage;
import com.gist.mathis.model.repository.MathisMessageRepository;

import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class MathisMessageService {
	@Autowired
	private MathisMessageRepository mathisMessageRepository;
	
	@Autowired
    private JavaMailSender mailSender;
	
	@Value("${spring.mail.from}")
    private String from;
	
	public List<MathisMessage> findAll() {
		return mathisMessageRepository.findAll();
	}

	public MathisMessage updateMathisMessage(MathisMessage mathisMessage) {
		mathisMessage.setUpdatedAt(null);
		mathisMessage = mathisMessageRepository.save(mathisMessage);
        return mathisMessage;
	}

	public void deleteById(Long id) {
		mathisMessageRepository.deleteById(id);
	}

    public void sendByMail(Long id) {
    	log.info("Sending message ID {} by mail",id);
    	
    	if("EMPTY".equals(from)) {
    		log.info("No email account set");
    		return;
    	}
    	
    	try {
	    	Optional<MathisMessage> mathisMessage = mathisMessageRepository.findById(id);
	    	if(mathisMessage.isPresent()) {
	    		MathisMessage mathisMessageToSend = mathisMessage.get();
	    		MimeMessage message = mailSender.createMimeMessage();
		        message.setFrom(from);
		        String recipientString = String.join(",", mathisMessageToSend.getRecipients());
		        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipientString));
		        message.setSubject(mathisMessageToSend.getTitle()); 
		        message.setText(mathisMessageToSend.getBody(), "UTF-8", "html");
		        mailSender.send(message);
		        
		        log.info("Message sent to {} Subject[{}]",mathisMessageToSend.getRecipients(), mathisMessageToSend.getTitle());
		        
		        mathisMessageToSend.setSent(Boolean.TRUE);
		        mathisMessageToSend.setSentAt(LocalDateTime.now());
		        mathisMessageToSend = mathisMessageRepository.save(mathisMessageToSend);
	    	}
    	}catch (MessagingException ex) {
    		log.error(ex.getMessage());
    	}
    }
}
