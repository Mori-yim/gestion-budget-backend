//package com.budgetcam.security;
//
//
//import jakarta.servlet.FilterChain;
//import jakarta.servlet.ServletException;
//import jakarta.servlet.http.HttpServletRequest;
//import jakarta.servlet.http.HttpServletResponse;
//import lombok.RequiredArgsConstructor;
//import org.springframework.lang.NonNull;
//import org.springframework.security.authentication.*;
//import org.springframework.security.core.context.SecurityContextHolder;
//import org.springframework.security.core.userdetails.*;
//import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
//import org.springframework.stereotype.*;
//import org.springframework.web.filter.OncePerRequestFilter;
//
//import java.io.IOException;
//
//@Component
//@RequiredArgsConstructor
//class JwtAuthFilter extends OncePerRequestFilter {
//    private final JwtService jwtService;
//    private final UserDetailsService uds;
//
//    @Override
//    protected void doFilterInternal(@NonNull HttpServletRequest req, @NonNull HttpServletResponse res, @NonNull FilterChain chain)
//            throws ServletException, IOException {
//        String auth = req.getHeader("Authorization");
//        if (auth == null || !auth.startsWith("Bearer ")) { chain.doFilter(req, res); return; }
//        try {
//            String jwt = auth.substring(7);
//            String email = jwtService.extractUsername(jwt);
//            if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
//                UserDetails ud = uds.loadUserByUsername(email);
//                if (jwtService.isTokenValid(jwt, ud)) {
//                    var token = new UsernamePasswordAuthenticationToken(ud, null, ud.getAuthorities());
//                    token.setDetails(new WebAuthenticationDetailsSource().buildDetails(req));
//                    SecurityContextHolder.getContext().setAuthentication(token);
//                }
//            }
//        } catch (Exception ignored) {}
//        chain.doFilter(req, res);
//    }
//}
//
//
package com.budgetcam.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        try {

            String jwt = authHeader.substring(7);

            String email = jwtService.extractUsername(jwt);

            if (email != null
                    && SecurityContextHolder.getContext().getAuthentication() == null) {

                UserDetails userDetails =
                        userDetailsService.loadUserByUsername(email);

                if (jwtService.isTokenValid(jwt, userDetails)) {

                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails,
                                    null,
                                    userDetails.getAuthorities()
                            );

                    authToken.setDetails(
                            new WebAuthenticationDetailsSource()
                                    .buildDetails(request)
                    );

                    SecurityContextHolder.getContext()
                            .setAuthentication(authToken);

                }

            }

        }
        catch (Exception ex) {

            log.error(
                    "Erreur JWT : {}",
                    ex.getMessage()
            );

        }

        filterChain.doFilter(request, response);

    }

}
