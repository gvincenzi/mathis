package com.gist.mathis.configuration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import com.gist.mathis.model.entity.AuthorityEnum;
import com.gist.mathis.model.entity.MathisUser;
import com.gist.mathis.service.security.MathisUserDetailsService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class MathisAdminInitializer implements ApplicationRunner {
    @Autowired
    private MathisUserDetailsService mathisUserService;

    @Value("${owner.adminPassword}")
    private String defaultAdminPassword;

    @Override
    public void run(ApplicationArguments args) {
        if (!mathisUserService.existsByUsername("admin")) {
    		MathisUser user = new MathisUser();
    		user.setUsername("admin");
    		user.setPassword(defaultAdminPassword);
    		user.setFirstname("ADMIN");
    		user.setLastname("ADMIN");
    		user.setAuth(AuthorityEnum.ROLE_ADMIN);
    		mathisUserService.saveUser(user);
    		log.info("ADMIN user initialized");
        }
    }
}
