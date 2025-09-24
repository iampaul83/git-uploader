package com.example.backend.git.service;

import com.example.backend.error.InvalidRequestException;
import com.example.backend.error.RepoNotFoundException;
import com.example.backend.git.dto.CommitRequest;
import com.example.backend.git.dto.CommitResponse;
import com.example.backend.git.dto.CreateRepoRequest;
import com.example.backend.git.dto.RepoSummaryResponse;
import com.example.backend.git.model.RepoMetadata;
import com.example.backend.git.util.AzureRepoUrlParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.Year;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class RepoService {

        private static final Logger logger = LoggerFactory.getLogger(RepoService.class);
        private static final String METADATA_FILE = ".git-uploader.json";

        private final WorkspaceService workspaceService;
        private final PatService patService;
        private final GitCommandRunner gitCommandRunner;
        private final ObjectMapper objectMapper;

        public RepoService(WorkspaceService workspaceService, PatService patService, GitCommandRunner gitCommandRunner,
                        ObjectMapper objectMapper) {
                this.workspaceService = workspaceService;
                this.patService = patService;
                this.gitCommandRunner = gitCommandRunner;
                this.objectMapper = objectMapper;
        }

        public List<RepoSummaryResponse> listRepositories() {
                Path reposRoot = workspaceService.getReposRoot();
                List<RepoSummaryResponse> results = new ArrayList<>();
                if (!Files.exists(reposRoot)) {
                        return results;
                }
                try (var stream = Files.list(reposRoot)) {
                        stream.filter(Files::isDirectory).sorted(Comparator.comparing(Path::getFileName))
                                        .forEach(repoDirectory -> loadSummary(repoDirectory).ifPresent(results::add));
                        return results;
                } catch (IOException exception) {
                        throw new InvalidRequestException("無法讀取 repos 目錄: " + exception.getMessage());
                }
        }

        public RepoSummaryResponse addRepository(CreateRepoRequest request) {
                AzureRepoUrlParser.Result parsed = AzureRepoUrlParser.parse(request.url())
                                .orElseThrow(() -> new InvalidRequestException("無法解析 Azure DevOps Repo URL"));
                String branch = determineBranch(request, parsed);
                String folderName = AzureRepoUrlParser.createFolderName(parsed.project(), parsed.repository(), branch);
                Path targetDirectory = workspaceService.getReposRoot().resolve(folderName);
                if (Files.exists(targetDirectory)) {
                        throw new InvalidRequestException("目標資料夾已存在: " + folderName);
                }

                Map<String, String> gitEnv = patService.buildGitEnvironment();
                gitCommandRunner.runAndEnsureSuccess(workspaceService.getReposRoot(),
                                gitCommandRunner.command("git", "clone", parsed.url(), folderName), gitEnv,
                                "無法 clone 遠端 Repository");

                configureRepository(targetDirectory);
                ensureBranch(targetDirectory, branch, gitEnv);
                Path branchFolder = ensureYearBranchFolder(targetDirectory, branch);

                RepoMetadata metadata = new RepoMetadata(parsed.url(), parsed.project(), parsed.repository(), branch,
                                branchFolder.getFileName().toString(), Instant.now());
                writeMetadata(targetDirectory, metadata);
                return toSummary(targetDirectory, metadata);
        }

        public CommitResponse commitAndPush(String repoId, CommitRequest request) {
                Path repoDirectory = workspaceService.getReposRoot().resolve(repoId);
                if (!Files.exists(repoDirectory) || !Files.isDirectory(repoDirectory)) {
                        throw new RepoNotFoundException("找不到指定的 Repository: " + repoId);
                }

                logger.info("Starting commitAndPush for repository: {}", repoId);

                RepoMetadata metadata = readMetadata(repoDirectory)
                                .orElseThrow(() -> new RepoNotFoundException("Repository 缺少 metadata: " + repoId));
                Map<String, String> gitEnv = patService.buildGitEnvironment();
                String branch = metadata.branch();
                String sanitizedFolder = metadata.branchFolder();
                if (!StringUtils.hasText(sanitizedFolder)) {
                        sanitizedFolder = sanitizeBranchFolder(metadata.branch());
                }
                String year = String.valueOf(Year.now().getValue());
                Path branchFolder = repoDirectory.resolve(year).resolve(sanitizedFolder);
                try {
                        Files.createDirectories(branchFolder);
                } catch (IOException exception) {
                        throw new InvalidRequestException("無法建立提交目錄: " + exception.getMessage());
                }

                gitCommandRunner.runAndEnsureSuccess(repoDirectory,
                                gitCommandRunner.command("git", "checkout", branch), Map.of(), "無法切換到分支 " + branch);
                gitCommandRunner.runAndEnsureSuccess(repoDirectory,
                                gitCommandRunner.command("git", "fetch", "origin", branch), gitEnv,
                                "無法取得最新遠端內容");
                gitCommandRunner.runAndEnsureSuccess(repoDirectory,
                                gitCommandRunner.command("git", "merge", "--ff-only", "origin/" + branch), gitEnv,
                                "遠端已有更新且無法 fast-forward，請手動處理後重試。");

                String commitScope = year + "/" + sanitizedFolder;
                gitCommandRunner.runAndEnsureSuccess(repoDirectory,
                                gitCommandRunner.command("git", "add", "--", commitScope), Map.of(), "無法加入變更");

                GitCommandRunner.CommandResult diffResult = gitCommandRunner.run(repoDirectory,
                                gitCommandRunner.command("git", "diff", "--cached", "--quiet"), Map.of());
                if (diffResult.exitCode() == 0) {
                        gitCommandRunner.run(repoDirectory,
                                        gitCommandRunner.command("git", "reset", "HEAD", "--", commitScope), Map.of());
                        return new CommitResponse(false, "沒有檔案變更，未建立 Commit。");
                }
                if (diffResult.exitCode() != 1) {
                        throw new InvalidRequestException("無法檢查變更狀態: " + diffResult.stderr());
                }

                String commitMessage = request.message().trim();
                gitCommandRunner.runAndEnsureSuccess(repoDirectory,
                                gitCommandRunner.command("git", "commit", "-m", commitMessage), Map.of(), "Commit 失敗");
                gitCommandRunner.runAndEnsureSuccess(repoDirectory,
                                gitCommandRunner.command("git", "push", "origin", branch), gitEnv, "Push 失敗");

                return new CommitResponse(true, "已提交並推送至遠端分支 " + branch + "。");
        }


        private void configureRepository(Path targetDirectory) {
                gitCommandRunner.runAndEnsureSuccess(targetDirectory,
                                gitCommandRunner.command("git", "config", "http.version", "HTTP/1.1"), Map.of(),
                                "無法設定 http.version");
                gitCommandRunner.runAndEnsureSuccess(targetDirectory,
                                gitCommandRunner.command("git", "config", "lfs.skipSmudge", "true"), Map.of(),
                                "無法設定 Git LFS skip smudge");
        }

        private void ensureBranch(Path repository, String branch, Map<String, String> gitEnv) {
                GitCommandRunner.CommandResult lsRemote = gitCommandRunner.run(repository,
                                gitCommandRunner.command("git", "ls-remote", "--heads", "origin", branch), gitEnv);
                boolean exists = lsRemote.isSuccess() && StringUtils.hasText(lsRemote.stdout());
                if (exists) {
                        gitCommandRunner.runAndEnsureSuccess(repository,
                                        gitCommandRunner.command("git", "fetch", "origin", branch), gitEnv,
                                        "無法抓取遠端分支");
                        GitCommandRunner.CommandResult checkout = gitCommandRunner.run(repository,
                                        gitCommandRunner.command("git", "checkout", branch), gitEnv);
                        if (!checkout.isSuccess()) {
                                gitCommandRunner.runAndEnsureSuccess(repository,
                                                gitCommandRunner.command("git", "checkout", "-b", branch, "origin/" + branch),
                                                gitEnv, "無法切換到目標分支");
                        }
                        return;
                }

                gitCommandRunner.runAndEnsureSuccess(repository,
                                gitCommandRunner.command("git", "fetch", "origin", "master"), gitEnv,
                                "無法抓取 master 作為基底");
                gitCommandRunner.runAndEnsureSuccess(repository,
                                gitCommandRunner.command("git", "checkout", "-b", branch, "origin/master"), gitEnv,
                                "無法建立新分支");
                gitCommandRunner.runAndEnsureSuccess(repository,
                                gitCommandRunner.command("git", "push", "--set-upstream", "origin", branch), gitEnv,
                                "無法推送新分支");
        }

        private Path ensureYearBranchFolder(Path repository, String branch) {
                String sanitizedBranch = sanitizeBranchFolder(branch);
                Path yearDirectory = repository.resolve(String.valueOf(Year.now().getValue()));
                Path branchDirectory = yearDirectory.resolve(sanitizedBranch);
                try {
                        Files.createDirectories(branchDirectory);
                } catch (IOException exception) {
                        throw new InvalidRequestException("無法建立分支資料夾: " + exception.getMessage());
                }
                return branchDirectory;
        }

        private String sanitizeBranchFolder(String branch) {
                if (!StringUtils.hasText(branch)) {
                        return "branch";
                }
                String sanitized = branch.trim().replaceAll("[\\/\\s]+", "-");
                return sanitized.isEmpty() ? "branch" : sanitized;
        }

        private void writeMetadata(Path repository, RepoMetadata metadata) {
                try {
                        objectMapper.writerWithDefaultPrettyPrinter().writeValue(repository.resolve(METADATA_FILE).toFile(), metadata);
                } catch (IOException exception) {
                        throw new InvalidRequestException("無法寫入 metadata: " + exception.getMessage());
                }
        }

        private Optional<RepoMetadata> readMetadata(Path repository) {
                Path metadataPath = repository.resolve(METADATA_FILE);
                if (!Files.exists(metadataPath)) {
                        return Optional.empty();
                }
                try {
                        return Optional.of(objectMapper.readValue(metadataPath.toFile(), RepoMetadata.class));
                } catch (IOException exception) {
                        throw new InvalidRequestException("無法讀取 metadata: " + exception.getMessage());
                }
        }

        private Optional<RepoSummaryResponse> loadSummary(Path repository) {
                return readMetadata(repository).map(metadata -> toSummary(repository, metadata));
        }

        private RepoSummaryResponse toSummary(Path repository, RepoMetadata metadata) {
                String year = String.valueOf(Year.now().getValue());
                String folderName = metadata.branchFolder();
                if (!StringUtils.hasText(folderName)) {
                        folderName = sanitizeBranchFolder(metadata.branch());
                }
                String yearBranchPath = year + "/" + folderName;
                String relativePath;
                try {
                        relativePath = workspaceService.getProjectRoot().relativize(repository).toString();
                } catch (IllegalArgumentException exception) {
                        relativePath = repository.toString();
                }
                return new RepoSummaryResponse(repository.getFileName().toString(), metadata.project(),
                                metadata.repository(), metadata.branch(), metadata.url(), relativePath, yearBranchPath);
        }

        private String determineBranch(CreateRepoRequest request, AzureRepoUrlParser.Result parsed) {
                if (StringUtils.hasText(request.normalizedBranch())) {
                        return request.normalizedBranch();
                }
                if (parsed.branch() != null && !parsed.branch().isBlank()) {
                        return parsed.branch();
                }
                throw new InvalidRequestException("請輸入要操作的 Branch 名稱");
        }
}
