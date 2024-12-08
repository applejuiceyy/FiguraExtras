package com.github.applejuiceyy.figuraextras.services;

import com.github.applejuiceyy.figuraextras.util.NewLineNormaliserInputStream;
import com.github.applejuiceyy.figuraextras.util.Util;
import com.mojang.logging.LogUtils;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

public class AvatarPostProcessor {
    public static final AvatarPostProcessor INSTANCE = new AvatarPostProcessor();
    public static Logger logger = LogUtils.getLogger();
    private final Map<Path, PostProcessingInstance> runningPosts = new HashMap<>();

    private AvatarPostProcessor() {

    }

    private static boolean isValidNameLike(int b) {
        return b >= 'a' && b <= 'z' || b >= 'A' && b <= 'Z' || b == '-' || b >= '0' && b <= '9';
    }

    public synchronized PostProcessingInstance addPostProcessing(Path path, ProcessBuilder... builder) throws IOException {
        PostProcessingInstance value = new PostProcessingInstance(path, builder);
        runningPosts.put(path, value);
        return value;
    }

    public synchronized @Nullable PostProcessingInstance getPostProcessor(Path path) {
        return runningPosts.get(path);
    }

    interface ReaderState {
        ReaderState read(int b);
    }

    static class LookOutState implements ReaderState {
        private final BiConsumer<String, Map<String, String>> processor;
        int bangs = 0;

        public LookOutState(BiConsumer<String, Map<String, String>> processor) {
            this.processor = processor;
        }

        @Override
        public ReaderState read(int b) {
            String bang = "::";
            if (b == bang.charAt(bangs)) {
                if (++bangs == bang.length()) {
                    return new ActiveNameState(processor);
                }
            } else {
                bangs = 0;
            }


            return this;
        }
    }

    static class ActiveNameState implements ReaderState {
        private final BiConsumer<String, Map<String, String>> processor;
        StringBuilder commandName = new StringBuilder();

        public ActiveNameState(BiConsumer<String, Map<String, String>> processor) {
            this.processor = processor;
        }

        @Override
        public ReaderState read(int b) {
            if (isValidNameLike(b)) {
                commandName.append((char) b);
            } else if (b == ' ') {
                return new ArgumentNameCollectingState(commandName.toString(), new HashMap<>(), processor);
            } else {
                logger.warn("Post-Processor process let out an invalid {} character while printing the name", (char) b);
                return new LookOutState(processor);
            }
            return this;
        }
    }

    static class ArgumentNameCollectingState implements ReaderState {
        private final BiConsumer<String, Map<String, String>> processor;
        StringBuilder argumentName = new StringBuilder();
        String commandName;
        Map<String, String> arguments;

        public ArgumentNameCollectingState(String commandNameString, Map<String, String> arguments, BiConsumer<String, Map<String, String>> processor) {
            commandName = commandNameString;
            this.arguments = arguments;
            this.processor = processor;
        }

        @Override
        public ReaderState read(int b) {
            if (isValidNameLike(b)) {
                argumentName.append((char) b);
            } else if (b == '=') {
                return new ArgumentValueCollectingState(commandName, arguments, argumentName.toString(), processor);
            } else if (argumentName.isEmpty()) {
                try {
                    processor.accept(commandName, arguments);
                } catch (Exception e) {
                    logger.error("Error while handling IPC command: ", e);
                }
                return new LookOutState(processor);
            } else {
                logger.warn("Post-Processor process let out an invalid {} character while printing the argument name", (char) b);
                return new LookOutState(processor);
            }

            return this;
        }
    }

    static class ArgumentValueCollectingState implements ReaderState {

        private final String commandName;
        private final Map<String, String> arguments;
        private final String argumentName;
        private final StringBuilder argumentValue = new StringBuilder();
        private final BiConsumer<String, Map<String, String>> processor;

        public ArgumentValueCollectingState(String commandName, Map<String, String> arguments, String argumentName, BiConsumer<String, Map<String, String>> processor) {
            this.commandName = commandName;
            this.arguments = arguments;
            this.argumentName = argumentName;
            this.processor = processor;
        }

        @Override
        public ReaderState read(int b) {
            if (isValidNameLike(b)) {
                argumentValue.append((char) b);
            } else {
                arguments.put(argumentName, argumentValue.toString());
                if (b == ',') {
                    return new ArgumentNameCollectingState(commandName, arguments, processor);
                } else {
                    try {
                        processor.accept(commandName, arguments);
                    } catch (Exception e) {
                        logger.error("Error while handling IPC command: ", e);
                    }
                    LookOutState lookOutState = new LookOutState(processor);
                    return lookOutState.read(b);
                }
            }

            return this;
        }
    }

    static public class ProcessExitException extends RuntimeException {
        public final int processIndex;
        public final int exitCode;

        public ProcessExitException(String message, int processIndex, int exitCode) {
            super(message);
            this.processIndex = processIndex;
            this.exitCode = exitCode;
        }
    }

    public class PostProcessingInstance {
        ProcessState[] processes;
        Process[] rp;
        CompletableFuture<?> onExit;

        public PostProcessingInstance(Path path, ProcessBuilder... builders) throws IOException {
            processes = new ProcessState[builders.length];
            CompletableFuture<?>[] futures = new CompletableFuture[builders.length];
            rp = new Process[builders.length];

            for (int i = 0, buildersLength = builders.length; i < buildersLength; i++) {
                ProcessBuilder builder = builders[i];
                builder.redirectOutput(ProcessBuilder.Redirect.PIPE);

                Process process = builder.start();
                InputStream inputStream = new NewLineNormaliserInputStream(process.getInputStream());

                ProcessState processState;
                processes[i] = processState = new ProcessState();
                processState.process = process;
                processState.progress = 0;
                processState.total = 1;

                rp[i] = process;

                futures[i] = process.onExit();

                Util.thread(() -> {
                    ReaderState state = new LookOutState((a, b) -> processCommand(a, b, processState));
                    try {
                        int b;
                        while ((b = inputStream.read()) != -1) {
                            state = state.read(b);
                        }
                    } catch (IOException e) {
                        process.destroy();
                        throw new RuntimeException(e);
                    }
                }, "IPC Output Reader");
            }

            onExit = CompletableFuture.allOf(futures).whenComplete((p, t) -> {
                synchronized (AvatarPostProcessor.this) {
                    runningPosts.remove(path);
                }
                for (int i = 0, processesLength = processes.length; i < processesLength; i++) {
                    ProcessState process = processes[i];
                    int i1 = process.process.exitValue();
                    if (i1 != 0) {
                        throw new ProcessExitException("A process exited non-graciously", i, i1);
                    }
                }
            });
        }

        private synchronized void processCommand(String commandName, Map<String, String> arguments, ProcessState state) {
            if (commandName.equals("total") || commandName.equals("progress")) {
                if (!arguments.containsKey("value")) {
                    logger.warn("Total command has no value");
                    return;
                }
                float value = Float.parseFloat(arguments.get("value"));
                if (value < 0) {
                    logger.warn("{} value argument cannot be negative", commandName);
                    return;
                }
                if (commandName.equals("total")) {
                    state.total = value;
                    state.progress = Math.min(state.total, state.progress);
                } else {
                    state.progress = value;
                    state.total = Math.max(state.total, state.progress);
                }
                return;
            }
            logger.error("Unknown IPC command {}", commandName);
        }

        public synchronized float getProgress() {
            float total = 0, value = 0;
            for (ProcessState process : processes) {
                total += process.total;
                value += process.progress;
            }
            if (total <= 0) {
                return 0;
            }
            return value / total;
        }

        public CompletableFuture<?> onExit() {
            return onExit;
        }

        public Process[] getProcesses() {
            return rp;
        }

        static class ProcessState {
            Process process;
            float total;
            float progress;
        }
    }
}
