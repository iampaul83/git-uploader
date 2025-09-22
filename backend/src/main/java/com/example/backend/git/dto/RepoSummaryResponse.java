package com.example.backend.git.dto;

public record RepoSummaryResponse(
                String id,
                String project,
                String repository,
                String branch,
                String url,
                String path,
                String yearBranchPath) {
}
