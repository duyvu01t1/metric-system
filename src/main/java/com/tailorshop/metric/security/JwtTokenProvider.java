package com.tailorshop.metric.security;

import lombok.Data;

/**
 * JWT Token payload
 */
@Data
public class JwtTokenProvider {

    private String token;
    private String refreshToken;
    private String tokenType = "Bearer";
    private long expiresIn;

}
