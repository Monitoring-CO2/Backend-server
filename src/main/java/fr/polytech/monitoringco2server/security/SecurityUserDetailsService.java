package fr.polytech.monitoringco2server.security;

import fr.polytech.monitoringco2server.database.repositories.AccountRepository;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class SecurityUserDetailsService implements ReactiveUserDetailsService {
	private final AccountRepository accountRepository;

	public SecurityUserDetailsService(AccountRepository accountRepository) {
		this.accountRepository = accountRepository;
	}

	@Override
	public Mono<UserDetails> findByUsername(String username) {
		return  accountRepository.findByUsername(username).cast(UserDetails.class);
	}

}
