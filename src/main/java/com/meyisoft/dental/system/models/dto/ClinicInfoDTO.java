package com.meyisoft.dental.system.models.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClinicInfoDTO {
    private String clinicName;
    private String doctorName;
    private String banco;
    private String cuentaBancaria;
    private String clabeInterbancaria;
    private double depositPercentage;
    private Integer diasAnticipacionReserva;
}
