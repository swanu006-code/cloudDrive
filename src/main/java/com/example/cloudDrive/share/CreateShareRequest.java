package com.example.cloudDrive.share;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record CreateShareRequest(
        @Min(1) @Max(720) Integer expiresInHours
) {}