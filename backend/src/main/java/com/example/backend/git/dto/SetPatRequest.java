package com.example.backend.git.dto;

import jakarta.validation.constraints.NotBlank;

public record SetPatRequest(@NotBlank(message = "PAT 不可為空") String pat) {
}
