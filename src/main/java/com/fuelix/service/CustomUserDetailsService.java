package com.fuelix.service;

import com.fuelix.model.User;
import com.fuelix.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String nic) throws UsernameNotFoundException {
        User user = userRepository.findByNic(nic)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with NIC: " + nic));

        return new org.springframework.security.core.userdetails.User(
                user.getNic(),
                user.getPassword(),
                new ArrayList<>()
        );
    }
}