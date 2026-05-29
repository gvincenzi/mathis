package com.gist.mathis.service.ingester;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

@Data
@Component
@ConfigurationProperties(prefix = "spring.mail.reader")
public class InboxMailKnowledgeIngesterProperties {
    private String protocol;
    private String host;
    private int port;
    private String username;
    private String password;
    private String folder = "INBOX";
    
    private Boolean sslEnable = true;            // mail.imap.ssl.enable
    private Boolean starttlsEnable = false;      // mail.imap.starttls.enable
    private Boolean authLoginDisable = false;    // mail.imap.auth.login.disable
    private Boolean debug = false;               // mail.debug
    private Integer connectionTimeout = 10000;   // mail.imap.connectiontimeout
    private Integer timeout = 10000;             // mail.imap.timeout
}

