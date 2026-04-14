package com.gist.mathis.service.ingester;

import com.gist.mathis.model.entity.RawKnowledge;
import com.gist.mathis.model.entity.RawKnowledgeSourceEnum;
import com.gist.mathis.model.repository.RawKnowledgeRepository;
import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.search.FlagTerm;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

@Slf4j
@IngesterScheduler(configKey = "mathis.ingesters.inboxmail")
@Component
public class InboxMailKnowledgeIngester implements KnowledgeIngester {

    @Autowired
    private InboxMailKnowledgeIngesterProperties properties;

    @Autowired
    private RawKnowledgeRepository rawKnowledgeRepository;

    @Override
    public RawKnowledgeSourceEnum getSourceName() {
        return RawKnowledgeSourceEnum.MAIL;
    }

    @Override
    public void ingest() {
        log.info("[{}][{}] Start ingestion", getSourceName(), getClass().getSimpleName());
        int count = 0;
        Properties props = new Properties();
        props.put("mail.store.protocol", properties.getProtocol());
        props.put("mail.imap.host", properties.getHost());
        props.put("mail.imap.port", String.valueOf(properties.getPort()));
        props.put("mail.imap.ssl.enable", String.valueOf(properties.getSslEnable()));
        props.put("mail.imap.auth.login.disable", String.valueOf(properties.getAuthLoginDisable()));
        props.put("mail.imap.starttls.enable", String.valueOf(properties.getStarttlsEnable()));
        props.put("mail.imap.connectiontimeout", String.valueOf(properties.getConnectionTimeout()));
        props.put("mail.imap.timeout", String.valueOf(properties.getTimeout()));
        props.put("mail.debug", String.valueOf(properties.getDebug()));
        try {
            Session session = Session.getDefaultInstance(props);
            Store store = session.getStore(properties.getProtocol());
            store.connect(
                properties.getHost(),
                properties.getPort(),
                properties.getUsername(),
                properties.getPassword()
            );

            Folder inbox = store.getFolder(properties.getFolder());
            inbox.open(Folder.READ_WRITE);

            FlagTerm unseenFlagTerm = new FlagTerm(new Flags(Flags.Flag.SEEN), false);
            Message[] messages = inbox.search(unseenFlagTerm);

            for (Message message : messages) {
                try {
                    String subject = message.getSubject();
                    Address[] froms = message.getFrom();
                    String from = (froms != null && froms.length > 0) ? ((InternetAddress)froms[0]).getAddress() : null;
                    Date sentDate = message.getSentDate();
                    String uniqueName = subject + " (" + from + ") " + sentDate;

                    Optional<RawKnowledge> byName = rawKnowledgeRepository.findByNameAndSource(uniqueName, getSourceName());
                    if(byName.isPresent()) {
                        log.info("Mail already ingested: {}", uniqueName);
                        continue;
                    }

                    String body = IngesterUtil.extractTextFromMessage(message);

                    RawKnowledge mailKnowledge = new RawKnowledge();
                    mailKnowledge.setSource(getSourceName());
                    mailKnowledge.setName(uniqueName);
                    mailKnowledge.setDescription(body);

                    mailKnowledge.getMetadata().put("subject", subject);
                    mailKnowledge.getMetadata().put("from", from);
                    mailKnowledge.getMetadata().put("sentDate", sentDate != null ? sentDate.toString() : null);

                    mailKnowledge.getMetadata().put("to", IngesterUtil.getAddressesAsString(message.getRecipients(Message.RecipientType.TO)));
                    mailKnowledge.getMetadata().put("cc", IngesterUtil.getAddressesAsString(message.getRecipients(Message.RecipientType.CC)));
                    mailKnowledge.getMetadata().put("bcc", IngesterUtil.getAddressesAsString(message.getRecipients(Message.RecipientType.BCC)));

                    rawKnowledgeRepository.save(mailKnowledge);
                    message.setFlag(Flags.Flag.SEEN, true);
                    count++;
                    log.info("Mail saved: {} > {}", mailKnowledge.getId(), mailKnowledge.getName());

                } catch (Exception ex) {
                    log.error("Error processing mail", ex);
                }
            }

            inbox.close(false);
            store.close();

        } catch (Exception e) {
            log.error("Error reading mails", e);
        }
        log.info("[{}][{}] End ingestion - {} RawKnowledge(s) ingested", getSourceName(), getClass().getSimpleName(), count);
    }
}

