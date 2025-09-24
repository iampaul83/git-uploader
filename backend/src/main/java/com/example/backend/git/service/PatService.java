package com.example.backend.git.service;

import com.example.backend.error.InvalidRequestException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class PatService {

        private static final String MASK_TOKEN = "***";

        private final Path patFile;
        private final Path askPassScript;

        public PatService(WorkspaceService workspaceService) {
                Path dataDirectory = workspaceService.getDataDirectory();
                this.patFile = dataDirectory.resolve("pat.txt");
                this.askPassScript = dataDirectory.resolve("git-askpass.sh");
                ensureAskPassScript();
        }

        public synchronized void updatePat(String pat) {
                if (pat == null || pat.isBlank()) {
                        throw new InvalidRequestException("PAT 不可為空");
                }
                String trimmed = pat.trim();
                try {
                        Files.writeString(patFile, trimmed, StandardCharsets.UTF_8);
                } catch (IOException exception) {
                        throw new InvalidRequestException("無法儲存 PAT: " + exception.getMessage());
                }
        }

        public synchronized Optional<String> readPat() {
                if (!Files.exists(patFile)) {
                        return Optional.empty();
                }
                try {
                        String value = Files.readString(patFile, StandardCharsets.UTF_8).trim();
                        if (value.isEmpty()) {
                                return Optional.empty();
                        }
                        return Optional.of(value);
                } catch (IOException exception) {
                        throw new InvalidRequestException("無法讀取 PAT: " + exception.getMessage());
                }
        }

        public synchronized String requirePat() {
                return readPat().orElseThrow(() -> new InvalidRequestException("尚未設定 Azure DevOps PAT"));
        }

        public synchronized Map<String, String> buildGitEnvironment() {
                String pat = requirePat();
                ensureAskPassScript();
                Map<String, String> environment = new HashMap<>();
                environment.put("GIT_ASKPASS", askPassScript.toString());
                environment.put("GIT_UPLOADER_PAT", pat);
                environment.put("GIT_UPLOADER_USERNAME", "pat");
                environment.put("GIT_TERMINAL_PROMPT", "0");
                return environment;
        }

        public synchronized String maskPat(String pat) {
                if (pat == null || pat.isBlank()) {
                        return MASK_TOKEN;
                }
                String trimmed = pat.trim();
                if (trimmed.length() <= 4) {
                        return MASK_TOKEN + trimmed;
                }
                return MASK_TOKEN + trimmed.substring(trimmed.length() - 4);
        }

        private void ensureAskPassScript() {
                try {
                        if (!Files.exists(askPassScript)) {
                                writeAskPassScript();
                        }
                } catch (IOException exception) {
                        throw new InvalidRequestException("無法建立憑證腳本: " + exception.getMessage());
                }
        }

        private void writeAskPassScript() throws IOException {
                String script = "#!/bin/sh\n" +
                                "if echo \"$1\" | grep -i username >/dev/null 2>&1; then\n" +
                                "  echo \"${GIT_UPLOADER_USERNAME:-pat}\"\n" +
                                "else\n" +
                                "  echo \"${GIT_UPLOADER_PAT:?缺少PAT}\"\n" +
                                "fi\n";
                Files.writeString(askPassScript, script, StandardCharsets.UTF_8);
                try {
                        Files.setPosixFilePermissions(askPassScript,
                                        EnumSet.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_EXECUTE,
                                                        PosixFilePermission.OWNER_WRITE));
                } catch (UnsupportedOperationException ignored) {
                        // Windows 或不支援 POSIX 權限時忽略
                }
        }
}
