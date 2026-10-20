package com.foodpassport.auth;

import com.foodpassport.user.User;
import java.time.Duration;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

/**
 * Creates the login token (a JWT). A JWT is three base64 parts: header.claims.signature.
 * The signature is made with our secret, so nobody can forge or alter a token without it.
 */
@Service
public class TokenService {

    private final JwtEncoder encoder;
    private final Duration ttl;

    public TokenService(JwtEncoder encoder, @Value("${foodpassport.jwt.ttl}") Duration ttl) {
        this.encoder = encoder;
        this.ttl = ttl;
    }

    public String issue(User user) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("food-passport")
                .issuedAt(now)
                .expiresAt(now.plus(ttl))
                .subject(user.getId().toString())
                .claim("username", user.getUsername())
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }
}
