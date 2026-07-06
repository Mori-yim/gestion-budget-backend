package com.budgetcam.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

@Service
public class JwtService {
    @Value("${jwt.secret}") private String secretKey;
    @Value("${jwt.expiration}") private long expiration;

    public String generateToken(UserDetails u) {
        return Jwts.builder().setSubject(u.getUsername())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getKey(), SignatureAlgorithm.HS256).compact();
    }
    public boolean isTokenValid(String token, UserDetails u) {
        return extractUsername(token).equals(u.getUsername()) &&
               !Jwts.parserBuilder().setSigningKey(getKey()).build()
                   .parseClaimsJws(token).getBody().getExpiration().before(new Date());
    }
    public String extractUsername(String token) {
        return Jwts.parserBuilder().setSigningKey(getKey()).build()
               .parseClaimsJws(token).getBody().getSubject();
    }
    //private Key getKey() { return Keys.hmacShaKeyFor(Decoders.BASE64.decode(secretKey)); }
    private Key getKey() {

        return Keys.hmacShaKeyFor(

                secretKey.getBytes(StandardCharsets.UTF_8)

        );

    }
}
