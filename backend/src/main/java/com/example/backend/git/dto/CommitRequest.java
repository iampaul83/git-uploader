package com.example.backend.git.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CommitRequest(
                @NotBlank(message = "請輸入 Commit 訊息") @Size(max = 200, message = "Commit 訊息過長") String message) {
}
