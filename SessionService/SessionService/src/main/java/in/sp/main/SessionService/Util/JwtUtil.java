package in.sp.main.SessionService.Util;


// Token Generate + Validate

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;


import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Base64;
import java.util.Date;

import static java.security.KeyRep.Type.SECRET;

@Component
public class JwtUtil {

    private final Key key;

    public JwtUtil(@org.springframework.beans.factory.annotation.Value("${jwt.secret}") String secretKey) {
        this.key = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
    }


    public String generateToken(String role, String sessionId){

        long EXPIRATION = 60 * 60 * 1000;
        return Jwts
                .builder()
                .claim("role",role)
                .claim("sessionId",sessionId)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis()+ EXPIRATION))
                .signWith(key)
                .compact();
    }

    public Jws<Claims> validateToken(String token){

        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token);
    }

    public Claims extractClaims(String token) {
        return validateToken(token).getBody();
    }

    public boolean isExpired(String token) {
        try {
            Date expiration = extractClaims(token).getExpiration();
            return expiration.before(new Date());
        } catch (Exception e) {
            return true;
        }
    }

}
