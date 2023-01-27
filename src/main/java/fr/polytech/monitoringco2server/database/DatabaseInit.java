package fr.polytech.monitoringco2server.database;

import fr.polytech.monitoringco2server.database.documents.Account;
import fr.polytech.monitoringco2server.database.repositories.AccountRepository;
import fr.polytech.monitoringco2server.security.UserRoles;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DatabaseInit {
	private final Logger logger = LoggerFactory.getLogger(this.getClass().getName());

	final AccountRepository accountRepository;

	@Value("${monitoringco2.admin.defaultusername}")
	String defaultAdminUsername;

	@Value("${monitoringco2.admin.defaultpassword}")
	String defaultAdminPassword;

	public DatabaseInit(AccountRepository accountRepository) {
		this.accountRepository = accountRepository;
	}

	@PostConstruct
	void initDatabase(){
		logger.info("Initialisation de la base de donnée");

		accountRepository.count().doOnError(throwable -> {logger.error("Impossible d'obtenir le nombre d'utilisateurs : "+throwable.getLocalizedMessage());})
				.subscribe(accountCount -> {
			if(accountCount == 0){
				Account adminAccount = new Account(defaultAdminUsername,
						new BCryptPasswordEncoder().encode(defaultAdminPassword),
						"Administrateur",
						"",
						true,
						UserRoles.ADMINISTRATEUR);
				accountRepository.save(adminAccount).subscribe(account -> {
					logger.info("Aucun compte : ajout du compte Administateur par défaut");
				});
			}
		});
	}
}
