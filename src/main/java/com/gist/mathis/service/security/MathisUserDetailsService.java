package com.gist.mathis.service.security;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
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
    	user.setPassword(bCryptPasswordEncoder.encode(user.getPassword()));
        return userRepository.save(user);
    }

    public void deleteUser(Long userId) {
        userRepository.deleteById(userId);
    }
    
    public MathisUser findOrCreateByTelegram(Update update, Long chatId) {
    	MathisUser user = userRepository.findByUsername(Long.toString(chatId)).orElseGet(() -> {
			MathisUser newUser = new MathisUser();
			newUser.setUsername(Long.toString(chatId));
			newUser.setFirstname(update.getMessage().getFrom().getFirstName());
			newUser.setLastname(update.getMessage().getFrom().getLastName());
			newUser.setPassword(bCryptPasswordEncoder.encode(Long.toString(chatId)));
			return saveUser(newUser);
		});
    	
    	return user;
    }
    
	public MathisUser getMathisUser(String username) throws UsernameNotFoundException {
		return userRepository.findByUsername(username).orElseThrow(() -> new UsernameNotFoundException(username));
	}

	public List<MathisUser> findAll() {
		return userRepository.findAll();
	}

	public void updateUser(MathisUser existing) {
		userRepository.save(existing);
	}

	public Optional<MathisUser> findById(Long userId) {
		return userRepository.findById(userId);
	}
}
