package com.gist.mathis.service.security;

import java.util.HashSet;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.Update;

import com.gist.mathis.model.entity.MathisUser;
import com.gist.mathis.model.repository.MathisUserRepository;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class MathisUserDetailsService implements UserDetailsService{
	private BCryptPasswordEncoder bCryptPasswordEncoder = new BCryptPasswordEncoder();

    @Autowired
    private MathisUserRepository userRepository;
	
	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		MathisUser user = userRepository.findByUsername(username).orElseThrow(() -> new UsernameNotFoundException(username));
		Set<GrantedAuthority> grantedAuthorities = new HashSet<>();

        user.getAuthorities()
          .forEach(authority -> grantedAuthorities.add(new SimpleGrantedAuthority(authority.getAuthority())));
        return new User(user.getUsername(), user.getPassword(), grantedAuthorities);
	}

    public MathisUser saveUser(MathisUser user) {
        return userRepository.save(user);
    }

    public void deleteUser(Long userId) {
        userRepository.deleteById(userId);
    }
    
    public MathisUser findOrCreateByTelegram(Update update) {
    	MathisUser user = userRepository.findByUsername(Long.toString(update.getMessage().getChatId())).orElseGet(() -> {
			MathisUser newUser = new MathisUser();
			newUser.setUsername(Long.toString(update.getMessage().getChatId()));
			newUser.setFirstname(update.getMessage().getFrom().getFirstName());
			newUser.setLastname(update.getMessage().getFrom().getLastName());
			newUser.setPassword(bCryptPasswordEncoder.encode(Long.toString(update.getMessage().getChatId())));
			return saveUser(newUser);
		});
    	
    	return user;
    }
}
