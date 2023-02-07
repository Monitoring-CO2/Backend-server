package fr.polytech.monitoringco2server.security;

import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public enum UserRoles {
	ADMINISTRATEUR(List.of(UserAuthorities.ADD_DEVICE, UserAuthorities.REMOVE_DEVICE));

	public final Collection<UserAuthorities> authorities;
	UserRoles(Collection<UserAuthorities> authorities){
		this.authorities = authorities;
	}

	public Collection<UserAuthorities> getAuthorities(){
		return authorities;
	}
	public Collection<GrantedAuthority> grantedAuthorities(){
		return authorities.stream().map(it -> it.authority).collect(Collectors.toList());
	}
}
