package com.meyisoft.dental.system.models.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AuthResponse {
    private String token;
    @Builder.Default
    private String type = "Bearer";
    private Object user; // Optional details
    private String giro;
    private String plan;
    private String temporaryPin;
}
