package fr.polytech.monitoringco2server.security;

import org.springframework.security.core.authority.SimpleGrantedAuthority;

public enum UserAuthorities {
	ADD_DEVICE("ADD_DEVICE"),
	REMOVE_DEVICE("REMOVE_DEVICE"),
	VIEW_DEVICE_DATA("VIEW_DEVICE_DATA");

	public final SimpleGrantedAuthority authority;
	private final String authorityStr;
	UserAuthorities(String authorityStr){
		this.authorityStr = authorityStr;
		authority = new SimpleGrantedAuthority(authorityStr);
	}


	@Override
	public String toString() {
		return authorityStr;
	}
}
