package com.wang.deerassistant.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserLoginResponse {

    private Long id;
    private String username;
    private String token;
}
