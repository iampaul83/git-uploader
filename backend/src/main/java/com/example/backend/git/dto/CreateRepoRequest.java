package com.example.backend.git.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateRepoRequest(@NotBlank(message = "請提供 Repository URL") String url, String branch) {

        public String normalizedBranch() {
                if (branch == null) {
                        return null;
                }
                String trimmed = branch.trim();
                return trimmed.isEmpty() ? null : trimmed;
        }
}
