package com.example.backend.git.service;

import jakarta.annotation.PostConstruct;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.springframework.stereotype.Service;

@Service
public class WorkspaceService {

        private final Path backendRoot;
        private final Path projectRoot;
        private final Path reposRoot;
        private final Path dataDirectory;

        public WorkspaceService() {
                this.backendRoot = Paths.get("").toAbsolutePath().normalize();
                Path parent = backendRoot.getParent();
                if (parent == null) {
                        throw new IllegalStateException("無法定位專案根目錄");
                }
                this.projectRoot = parent;
                this.reposRoot = projectRoot.resolve("repos");
                this.dataDirectory = backendRoot.resolve("data");
        }

        @PostConstruct
        void ensureWorkspace() {
                try {
                        Files.createDirectories(reposRoot);
                        Files.createDirectories(dataDirectory);
                } catch (Exception exception) {
                        throw new IllegalStateException("無法建立必要目錄: " + exception.getMessage(), exception);
                }
        }

        public Path getBackendRoot() {
                return backendRoot;
        }

        public Path getProjectRoot() {
                return projectRoot;
        }

        public Path getReposRoot() {
                return reposRoot;
        }

        public Path getDataDirectory() {
                return dataDirectory;
        }
}
