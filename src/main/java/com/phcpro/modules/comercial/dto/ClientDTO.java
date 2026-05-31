package com.phcpro.modules.comercial.dto;

public record ClientDTO(
    Long id,
    String name,
    String taxId,
    String email,
    String address
) {}
