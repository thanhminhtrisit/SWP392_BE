package com.se1908.group01.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RotationResult {
    private Long userId;
    private String newRawToken;
}
