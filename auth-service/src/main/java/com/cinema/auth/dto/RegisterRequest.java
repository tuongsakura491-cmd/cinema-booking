package com.cinema.auth.dto;

import jakarta.validation.constraints.*;

public record RegisterRequest(
        @NotBlank @Size(min = 3, max = 50) String username,
        @NotBlank @Email @Pattern(regexp = "^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$", message = "Email không đúng định dạng") String email,
        @NotBlank @Size(min = 6, max = 100) String password,
        @NotBlank String confirmPassword) {
}
