package com.app.nino.model.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RefreshTokenRequest {

    @NotBlank(message = "refreshToken khong duoc de trong")
    private String refreshToken;

    @Size(max = 100, message = "deviceId toi da 100 ky tu")
    private String deviceId;
}
