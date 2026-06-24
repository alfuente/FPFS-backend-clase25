package com.shopeasy.dto.response;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.Set;

@Data
public class UserResponse {
    private Long id;
    private String name;
    private String email;
    private boolean enabled;
    private Set<String> roles;
    private LocalDateTime createdAt;
}
