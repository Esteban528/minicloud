package com.estebandev.minicloud.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;

import com.estebandev.minicloud.service.CustomUserDetailsService;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
@EnableMethodSecurity
public class SecurityConfig {
    private final PasswordEncoder passwordEncoder;
    private final CustomUserDetailsService userDetailsService;

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity) throws Exception {
        return httpSecurity
                .csrf(Customizer.withDefaults())
                .httpBasic(http -> http.disable())
                .formLogin(login -> {
                    login
                            .failureHandler(auhAuthenticationFailureHandler())
                            .loginPage("/login")
                            .usernameParameter("email")
                            .passwordParameter("password")
                            .successForwardUrl("/");

                })
                .authorizeHttpRequests(httz -> {
                    httz
                            .requestMatchers(
                                    "/register/**", "/login/**", "/", "/recoveryportal/**", "/passwordrecovery/**",
                                    "/css/**", "/js/**", "/images/**")
                            .permitAll()
                            .requestMatchers("/admin/**").hasAuthority("ADMIN_DASHBOARD")
                            .requestMatchers("/files/**").hasAuthority("FILE_DASHBOARD")
                            .anyRequest().authenticated();
                })
                .logout(logout -> {
                    logout
                            .logoutUrl("/logout")
                            .logoutSuccessUrl("/login?logout")
                            .invalidateHttpSession(true)
                            .deleteCookies("JSESSIONID")
                            .clearAuthentication(true)
                            .permitAll();
                })
                .headers(headerConfig -> headerConfig.cacheControl(cache -> {
                    cache.disable();
                }))
                .build();
    }

    @Bean
    AuthenticationManager authenticationManager(HttpSecurity httpSecurity) {
        return new ProviderManager(daoAuthenticationProvider());
    }

    AuthenticationProvider daoAuthenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return provider;
    }

    @Bean
    AuthenticationFailureHandler auhAuthenticationFailureHandler() {
        return new CustomAuthenticationFailureHandler();
    }
}
