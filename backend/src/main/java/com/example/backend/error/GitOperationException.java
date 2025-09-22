package com.example.backend.error;

public class GitOperationException extends RuntimeException {

        private final String command;
        private final String stdout;
        private final String stderr;

        public GitOperationException(String message, String command, String stdout, String stderr) {
                super(message);
                this.command = command;
                this.stdout = stdout;
                this.stderr = stderr;
        }

        public String getCommand() {
                return command;
        }

        public String getStdout() {
                return stdout;
        }

        public String getStderr() {
                return stderr;
        }
}
