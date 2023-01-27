package fr.polytech.monitoringco2server.database.documents;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import fr.polytech.monitoringco2server.security.UserRoles;

import java.util.Collection;

@Document(collection = "accounts")
public class Account implements UserDetails {

	@Id
	private ObjectId id;
	private String username;
	private String password;
	private String fullName;
	private String email;
	private boolean active = true;
	private UserRoles userRole;

	public Account(String username, String password, String fullName, String email, boolean active, UserRoles userRole) {
		this.username = username;
		this.password = password;
		this.fullName = fullName;
		this.email = email;
		this.active = active;
		this.userRole = userRole;
	}

	@Override
	public Collection<GrantedAuthority> getAuthorities() {
		return userRole.grantedAuthorities();
	}

	public ObjectId getId() {
		return id;
	}

	public String getFullName(){
		return fullName;
	}

	@Override
	public String getPassword() {
		return password;
	}

	@Override
	public String getUsername() {
		return username;
	}

	@Override
	public boolean isAccountNonExpired() {
		return active;
	}

	@Override
	public boolean isAccountNonLocked() {
		return active;
	}

	@Override
	public boolean isCredentialsNonExpired() {
		return active;
	}

	@Override
	public boolean isEnabled() {
		return active;
	}
}
