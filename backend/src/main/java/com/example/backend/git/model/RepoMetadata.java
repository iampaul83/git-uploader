package com.example.backend.git.model;

import java.time.Instant;

public record RepoMetadata(String url, String project, String repository, String branch, String branchFolder,
                Instant createdAt) {
}
