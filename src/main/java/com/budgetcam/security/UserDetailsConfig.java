package com.budgetcam.security;

import com.budgetcam.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

@Configuration
@RequiredArgsConstructor
public class UserDetailsConfig {

    private final UserRepository userRepository;

    @Bean
    UserDetailsService userDetailsService(){

        return username ->

                userRepository

                        .findByEmail(username)

                        .orElseThrow(

                                ()->new UsernameNotFoundException(

                                        username

                                )

                        );

    }

}