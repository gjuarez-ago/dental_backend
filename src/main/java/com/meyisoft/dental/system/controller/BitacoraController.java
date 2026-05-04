package com.meyisoft.dental.system.controller;

import com.meyisoft.dental.system.models.response.ApiResponse;
import com.meyisoft.dental.system.models.response.BitacoraResponse;
import com.meyisoft.dental.system.security.UserPrincipal;
import com.meyisoft.dental.system.service.BitacoraService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/bitacora")
@RequiredArgsConstructor
public class BitacoraController {

    private final BitacoraService bitacoraService;

    @GetMapping
    @PreAuthorize("hasAnyRole('OWNER', 'SUPER_ADMIN')")
    public ApiResponse<Page<BitacoraResponse>> listar(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return ApiResponse.success(bitacoraService.listar(principal.getTenantId(), page, size));
    }
}
