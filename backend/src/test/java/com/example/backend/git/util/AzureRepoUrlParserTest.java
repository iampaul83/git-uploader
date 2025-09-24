package com.example.backend.git.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AzureRepoUrlParserTest {

        @Test
        void parseShouldExtractProjectRepoAndBranchFromQuery() {
                String url = "https://dev.azure.com/org/sample/_git/repo?version=GBfeature%2Fdemo";
                AzureRepoUrlParser.Result result = AzureRepoUrlParser.parse(url).orElseThrow();
                assertThat(result.project()).isEqualTo("sample");
                assertThat(result.repository()).isEqualTo("repo");
                assertThat(result.branch()).isEqualTo("feature/demo");
        }

        @Test
        void parseShouldReturnEmptyBranchWhenNotSpecified() {
                String url = "https://dev.azure.com/org/sample/_git/repo";
                AzureRepoUrlParser.Result result = AzureRepoUrlParser.parse(url).orElseThrow();
                assertThat(result.branch()).isNull();
        }

        @Test
        void createFolderNameShouldSanitizeValues() {
                String folderName = AzureRepoUrlParser.createFolderName("Sample Project", "Repo.Name", "feature/demo");
                assertThat(folderName).isEqualTo("sample-project_repo.name_feature-demo");
        }
}
