package com.gestudio.crm.identity;

import java.time.Instant;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public record CrmPrincipal(
    UUID userId,
    UUID organizationId,
    String username,
    String displayName,
    String password,
    String role,
    Set<String> permissions,
    boolean active,
    Instant lockedUntil)
    implements UserDetails {

  public CrmPrincipal {
    permissions = Set.copyOf(permissions);
  }

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    Set<GrantedAuthority> authorities = new LinkedHashSet<>();
    authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
    permissions.forEach(code -> authorities.add(new SimpleGrantedAuthority(code)));
    return Set.copyOf(authorities);
  }

  @Override
  public String getUsername() {
    return username;
  }

  @Override
  public String getPassword() {
    return password;
  }

  @Override
  public boolean isAccountNonExpired() {
    return active;
  }

  @Override
  public boolean isAccountNonLocked() {
    return active && (lockedUntil == null || !lockedUntil.isAfter(Instant.now()));
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
