package com.naon.grid.modules.app.service;

import com.naon.grid.exception.BadRequestException;
import com.naon.grid.modules.app.domain.GridUser;
import com.naon.grid.modules.app.repository.GridUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Slf4j
@Service("appUserDetailsService")
@RequiredArgsConstructor
public class AppUserDetailsService implements UserDetailsService {

    private final GridUserRepository gridUserRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        GridUser user = gridUserRepository.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("用户不存在: " + username));

        if (user.getStatus() == 0) {
            throw new BadRequestException("用户已被禁用");
        }

        return new User(user.getEmail(), user.getPassword() != null ? user.getPassword() : "", Collections.emptyList());
    }
}
