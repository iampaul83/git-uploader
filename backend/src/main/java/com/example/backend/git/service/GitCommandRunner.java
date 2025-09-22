package com.example.backend.git.service;

import com.example.backend.error.GitOperationException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class GitCommandRunner {

        public CommandResult run(Path workingDirectory, List<String> command, Map<String, String> environmentOverrides) {
                ProcessBuilder builder = new ProcessBuilder(command);
                if (workingDirectory != null) {
                        builder.directory(workingDirectory.toFile());
                }
                Map<String, String> environment = builder.environment();
                environment.put("GIT_TERMINAL_PROMPT", "0");
                if (environmentOverrides != null) {
                        environment.putAll(environmentOverrides);
                }
                builder.redirectErrorStream(false);
                try {
                        Process process = builder.start();
                        String stdout = readFully(process.getInputStream());
                        String stderr = readFully(process.getErrorStream());
                        int exitCode = process.waitFor();
                        return new CommandResult(exitCode, stdout, stderr, new ArrayList<>(command));
                } catch (InterruptedException exception) {
                        Thread.currentThread().interrupt();
                        throw new GitOperationException("執行 git 指令被中斷", String.join(" ", command), "",
                                        exception.getMessage());
                } catch (IOException exception) {
                        throw new GitOperationException("執行 git 指令失敗: " + exception.getMessage(),
                                        String.join(" ", command), "", exception.getMessage());
                }
        }

        public void runAndEnsureSuccess(Path workingDirectory, List<String> command,
                        Map<String, String> environmentOverrides, String errorMessage) {
                CommandResult result = run(workingDirectory, command, environmentOverrides);
                if (!result.isSuccess()) {
                        throw new GitOperationException(errorMessage, result.getCommandLine(), result.stdout(),
                                        result.stderr());
                }
        }

        public record CommandResult(int exitCode, String stdout, String stderr, List<String> command) {

                public boolean isSuccess() {
                        return exitCode == 0;
                }

                public String getCommandLine() {
                        return command.stream().collect(Collectors.joining(" "));
                }
        }

        private String readFully(InputStream inputStream) throws IOException {
                byte[] buffer = inputStream.readAllBytes();
                if (buffer.length == 0) {
                        return "";
                }
                return new String(buffer, StandardCharsets.UTF_8).trim();
        }

        public List<String> command(String... parts) {
                List<String> command = new ArrayList<>();
                for (String part : parts) {
                        command.add(part);
                }
                return command;
        }
}
