package com.github.applejuiceyy.figuraextras.vscode.dsp;

import com.github.applejuiceyy.figuraextras.FiguraExtras;
import com.github.applejuiceyy.figuraextras.ducks.AvatarAccess;
import com.github.applejuiceyy.figuraextras.ducks.GlobalsAccess;
import com.github.applejuiceyy.figuraextras.ducks.LuaRuntimeAccess;
import com.github.applejuiceyy.figuraextras.ducks.statics.FiguraLuaPrinterDuck;
import com.github.applejuiceyy.figuraextras.ducks.statics.LuaDuck;
import com.github.applejuiceyy.figuraextras.mixin.debugadapter.figura.LuaRuntimeAccessor;
import com.github.applejuiceyy.figuraextras.tech.captures.SecondaryCallHook;
import com.github.applejuiceyy.figuraextras.util.Event;
import com.github.applejuiceyy.figuraextras.util.Util;
import com.github.applejuiceyy.figuraextras.vscode.DisconnectAware;
import com.mojang.datafixers.util.Either;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Tuple;
import org.eclipse.lsp4j.debug.Thread;
import org.eclipse.lsp4j.debug.*;
import org.eclipse.lsp4j.debug.services.IDebugProtocolClient;
import org.eclipse.lsp4j.debug.services.IDebugProtocolServer;
import org.eclipse.lsp4j.jsonrpc.messages.ResponseErrorCode;
import org.figuramc.figura.FiguraMod;
import org.figuramc.figura.avatar.Avatar;
import org.figuramc.figura.avatar.AvatarManager;
import org.figuramc.figura.lua.FiguraLuaRuntime;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.Nullable;
import org.luaj.vm2.*;
import org.luaj.vm2.compiler.LuaC;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class DebugProtocolServer implements IDebugProtocolServer, DisconnectAware {
    private static final Capabilities capabilities = new Capabilities();
    public static Logger logger = LoggerFactory.getLogger("FiguraExtras:DSP");
    private static DebugProtocolServer INSTANCE;

    static {
        capabilities.setSupportsConfigurationDoneRequest(true);
        capabilities.setSupportsDelayedStackTraceLoading(true);
        ExceptionBreakpointsFilter breakpointsFilter = new ExceptionBreakpointsFilter();
        breakpointsFilter.setFilter("debuggerAPI");
        breakpointsFilter.setLabel("breakpoint call");
        capabilities.setExceptionBreakpointFilters(new ExceptionBreakpointsFilter[]{breakpointsFilter});
        capabilities.setSupportsLoadedSourcesRequest(true);
        capabilities.setSupportsHitConditionalBreakpoints(true);
        capabilities.setSupportsConditionalBreakpoints(true);
        capabilities.setSupportsLogPoints(true);
    }

    final Sourcer sourcer = new Sourcer(this);
    final StackTraceTracker stackTrace = new StackTraceTracker();
    private final HashMap<Either<String, Integer>, List<BreakpointState>> breakpoints = new HashMap<>();
    private final DAInternalInterface internalInterface = new DAInternalInterface();
    InitializeRequestArguments clientCapabilities;
    Map<String, Object> launchArgs;
    IDebugProtocolClient client;
    private int breakpointNextId = 0;
    private CompletableFuture<Void> paused = CompletableFuture.completedFuture(null);
    private int burntBreakpoint = -1;
    private @Nullable StackTraceInspector inspector;
    private Event<Runnable> destroyers = Event.runnable();
    private @Nullable Runnable situationalBreakpointRemover;
    private boolean canBeStoppedByBreakpointCall = false;
    private boolean isInBreakpoint = false;

    /***
     * Almost all of these methods are not for public consumption, refer to getInternalInterface
     */
    public static @Nullable DebugProtocolServer getInstance() {
        return INSTANCE;
    }

    public static @Nullable DAInternalInterface getInternalInterface() {
        return INSTANCE == null ? null : INSTANCE.internalInterface;
    }

    public static void create() {
        if (INSTANCE != null) {
            throw new RuntimeException("There's already a debug protocol server running");
        }
        INSTANCE = new DebugProtocolServer();
    }

    private static boolean isChild(Path child, Path parent) {
        return child.startsWith(parent);
    }

    @Override
    public void onDisconnect() {
        INSTANCE = null;
        destroyers.getSink().run();
        paused.complete(null);
        FiguraExtras.updateInformation();
    }

    public void connect(IDebugProtocolClient client) {
        this.client = client;
        FiguraExtras.updateInformation();
    }

    Avatar getCurrentAvatar() {
        return AvatarManager.getAvatarForPlayer(FiguraMod.getLocalPlayerUUID());
    }

    private void updateBreakpointAvailability(Iterable<Breakpoint> breakpoints, String source) {
        Prototype compilation;
        try {
            compilation = LuaC.instance.compile(new UTF8Stream(new StringReader(source)), "");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        source = source.replace("\r\n", "\n");
        String[] lines = source.split("\n");
        for (Breakpoint breakpoint : breakpoints) {
            findSuitableLineInfo(breakpoint, compilation, lines, breakpoint.getLine(), -1);
        }
    }

    private int findSuitableLineInfo(Breakpoint breakpoint, Prototype proto, String[] lines, int preferred, int best) {
        int[] lineinfo = proto.lineinfo;
        for (int i = 0; i < lineinfo.length; i++) {
            int line = lineinfo[i];
            // remove OP_RETURN as it's probably a synthetic op code added at the end of functions
            // actual returns probably also have some other useful op code like calling a function
            if (preferred <= line && proto.code[i] != LuaC.OP_RETURN) {
                int thisBest = line - preferred;

                if (best != -1 && thisBest > best) {
                    break;
                }


                breakpoint.setLine(line);
                breakpoint.setVerified(true);
                String sourceLine = lines[line - 1];

                int c = 0;
                char ch;
                while (sourceLine.length() > c && ((ch = sourceLine.charAt(c)) == ' ' || ch == '\t')) {
                    c++;
                }

                if (sourceLine.length() == c) {
                    breakpoint.setColumn(null);
                    breakpoint.setEndColumn(null);
                } else {
                    breakpoint.setColumn(c);
                    breakpoint.setEndColumn(sourceLine.length());
                }

                best = line - preferred;
                break;
            }
        }

        for (Prototype prototype : proto.p) {
            int suitableLineInfo = findSuitableLineInfo(breakpoint, prototype, lines, preferred, best);
            if (suitableLineInfo != -1 && suitableLineInfo < best) {
                best = suitableLineInfo;
            }
        }

        return best;
    }

    private boolean isInstructionPausable(int pc, Prototype prototype) {
        if (pc == 0) return true;
        return prototype.lineinfo[pc] != prototype.lineinfo[pc - 1];
    }


    // now the actual inny griddy


    @Override
    public CompletableFuture<Void> disconnect(DisconnectArguments args) {
        if (args.getTerminateDebuggee() != null && args.getTerminateDebuggee() || launchArgs.get("request").equals("launch")) {
            Minecraft.getInstance().stop();
        }
        INSTANCE = null;
        destroyers.getSink().run();
        paused.complete(null);
        FiguraExtras.updateInformation();
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Capabilities> initialize(InitializeRequestArguments args) {
        clientCapabilities = args;
        FiguraExtras.sendBrandedMessage(Component.literal("We have liftoff!").withStyle(ChatFormatting.GOLD));
        if (new Random().nextFloat() < 0.3) {
            FiguraExtras.sendBrandedMessage(Component.literal("There's also a ko-fi! Just a fun fact really")
                    .withStyle(ChatFormatting.GREEN)
                    .withStyle(ChatFormatting.UNDERLINE)
                    .withStyle(style -> style.withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, "https://ko-fi.com/theapplejuice")))
            );
        }
        Util.after(client::initialized, 1000);
        return CompletableFuture.completedFuture(capabilities);
    }

    @Override
    public CompletableFuture<Void> attach(Map<String, Object> args) {
        launchArgs = args;
        AvatarManager.clearAvatars(FiguraMod.getLocalPlayerUUID());
        if (!args.containsKey("avatarPath") || !(args.get("avatarPath") instanceof String)) {
            return Util.fail(ResponseErrorCode.InvalidParams, "avatarPath is not an argument");
        }
        Path path = new File((String) args.get("avatarPath")).toPath();
        Path avatars;
        try {
            avatars = FiguraMod.getFiguraDirectory().resolve("avatars").toRealPath();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        if (!isChild(path, avatars)) {
            return Util.fail(ResponseErrorCode.InvalidParams, "Invalid Avatar Location");
        }
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<ThreadsResponse> threads() {
        ThreadsResponse value = new ThreadsResponse();
        Thread thread = new Thread();
        thread.setName("Avatar thread");
        thread.setId(0);
        value.setThreads(new Thread[]{thread});
        return CompletableFuture.completedFuture(value);
    }

    @Override
    public CompletableFuture<SetBreakpointsResponse> setBreakpoints(SetBreakpointsArguments args) {
        List<Tuple<Breakpoint, SourceBreakpoint>> breakpoints = new ArrayList<>();
        Avatar currentAvatar = getCurrentAvatar();
        Map<String, Varargs> loadedScripts = currentAvatar == null || currentAvatar.luaRuntime == null ? null : ((LuaRuntimeAccessor) currentAvatar.luaRuntime).getLoadedScripts();

        Source source = args.getSource();
        for (SourceBreakpoint breakpoint : args.getBreakpoints()) {
            Breakpoint b = new Breakpoint();
            b.setSource(source);
            b.setId(breakpointNextId++);
            b.setLine(breakpoint.getLine());
            b.setVerified(false);
            breakpoints.add(new Tuple<>(b, breakpoint));
        }

        if (loadedScripts != null) {
            String content = sourcer.getSourceContent(source, true);
            if (content != null) {
                updateBreakpointAvailability(() -> breakpoints.stream().map(Tuple::getA).iterator(), content);
            }
        }
        this.breakpoints.put(sourcer.toEither(source), breakpoints.stream().map(t -> new BreakpointState(this, t.getA(), t.getB())).collect(Collectors.toList()));
        SetBreakpointsResponse setBreakpointsResponse = new SetBreakpointsResponse();
        setBreakpointsResponse.setBreakpoints(breakpoints.stream().map(Tuple::getA).toArray(Breakpoint[]::new));
        return CompletableFuture.completedFuture(setBreakpointsResponse);
    }

    @Override
    public CompletableFuture<BreakpointLocationsResponse> breakpointLocations(BreakpointLocationsArguments args) {
        return IDebugProtocolServer.super.breakpointLocations(args);
    }

    @Override
    public CompletableFuture<SetExceptionBreakpointsResponse> setExceptionBreakpoints(SetExceptionBreakpointsArguments args) {
        canBeStoppedByBreakpointCall = Arrays.asList(args.getFilters()).contains("debuggerAPI");
        return CompletableFuture.completedFuture(new SetExceptionBreakpointsResponse());
    }

    @Override
    public CompletableFuture<Void> configurationDone(ConfigurationDoneArguments args) {
        FiguraExtras.sendBrandedMessage("Setting avatar to conform to debug session");
        AvatarManager.loadLocalAvatar(new File((String) launchArgs.get("avatarPath")).toPath());
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<StackTraceResponse> stackTrace(StackTraceArguments args) {
        if (args.getThreadId() != 0) {
            return Util.fail(ResponseErrorCode.InvalidParams, "Unknown Thread Id");
        }
        if (paused.isDone()) {
            return Util.fail(ResponseErrorCode.InvalidRequest, "Not paused");
        }

        int startRange = 0;
        int endRange = Integer.MAX_VALUE;
        if (args.getStartFrame() != null) {
            startRange = args.getStartFrame();
        }
        if (!(args.getLevels() == null || args.getLevels() == 0)) {
            endRange = args.getLevels() + startRange;
        }
        assert inspector != null; // must be not null while paused
        StackFrame[] stackFrames = inspector.getStackTrace(startRange, endRange);
        StackTraceResponse response = new StackTraceResponse();
        response.setStackFrames(stackFrames);
        response.setTotalFrames(inspector.getStackTraceSize());
        return CompletableFuture.completedFuture(response);
    }

    @Override
    public CompletableFuture<ScopesResponse> scopes(ScopesArguments args) {
        if (paused.isDone()) {
            return Util.fail(ResponseErrorCode.InvalidRequest, "Not paused");
        }
        ScopesResponse response = new ScopesResponse();
        assert inspector != null;
        response.setScopes(inspector.getScopes(args.getFrameId()));
        return CompletableFuture.completedFuture(response);
    }

    @Override
    public CompletableFuture<VariablesResponse> variables(VariablesArguments args) {
        if (paused.isDone()) {
            return Util.fail(ResponseErrorCode.InvalidRequest, "Not paused");
        }
        int startRange = 0;
        int endRange = Integer.MAX_VALUE;
        if (args.getStart() != null) {
            startRange = args.getStart();
        }
        if (!(args.getCount() == null || args.getCount() == 0)) {
            endRange = args.getCount() + startRange;
        }
        VariablesResponse response = new VariablesResponse();
        assert inspector != null;
        response.setVariables(inspector.getVariables(args.getVariablesReference(), startRange, endRange));
        return CompletableFuture.completedFuture(response);
    }

    @Override
    public CompletableFuture<SourceResponse> source(SourceArguments args) {
        int sourceReference = args.getSourceReference();
        String content = ((LuaRuntimeAccess) getCurrentAvatar().luaRuntime).figuraExtrass$getSource(sourceReference);
        SourceResponse response = new SourceResponse();
        response.setContent(content);
        response.setMimeType("text/x-lua");
        return CompletableFuture.completedFuture(response);
    }

    @Override
    public CompletableFuture<LoadedSourcesResponse> loadedSources(LoadedSourcesArguments args) {
        LoadedSourcesResponse loadedSourcesResponse = new LoadedSourcesResponse();
        loadedSourcesResponse.setSources(sourcer.listLoadedSources());
        return CompletableFuture.completedFuture(loadedSourcesResponse);
    }

    @Override
    public CompletableFuture<Void> next(NextArguments args) {
        // statement is default but only has access to line
        if (args.getThreadId() != 0) {
            return Util.fail(ResponseErrorCode.InvalidParams, "Unknown thread");
        }
        StackTraceTracker.LuaFrame luaFrame = stackTrace.frameList.get(stackTrace.frameList.size() - 1).right().orElse(null);

        int line;
        int s;

        if (luaFrame != null) {
            line = luaFrame.line;
            s = stackTrace.frameList.size() - 1;
        } else {
            line = 0;
            s = 0;
        }
        doContextualHook(new SecondaryCallHook() {
            @Override
            public void instruction(LuaClosure luaClosure, Varargs varargs, LuaValue[] stack, int instruction, int pc) {
                if (!isInstructionPausable(pc, luaClosure.p)) return;
                if (luaFrame != null) {
                    int l = luaFrame.closure.p.lineinfo[pc];
                    if (line != l && stackTrace.frameList.size() - 1 == s) {
                        doPause(arg -> arg.setReason(StoppedEventArgumentsReason.STEP));
                    }
                }
            }

            @Override
            public void outOfFunction(LuaClosure luaClosure, Varargs varargs, LuaValue[] stack, LuaDuck.ReturnType type) {
                if (type == LuaDuck.ReturnType.TAIL) return;
                if (stackTrace.frameList.size() - 1 < s && !stackTrace.frameList.isEmpty()) {
                    doPause(arg -> arg.setReason(StoppedEventArgumentsReason.STEP));
                }
            }
        });
        doContinue();
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<ContinueResponse> continue_(ContinueArguments args) {
        if (args.getThreadId() != 0) {
            return Util.fail(ResponseErrorCode.InvalidParams, "Unknown thread");
        }
        doContinue();
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public synchronized CompletableFuture<Void> pause(PauseArguments args) {
        doContextualHook(new SecondaryCallHook() {
            @Override
            public void outOfFunction(LuaClosure luaClosure, Varargs varargs, LuaValue[] stack, LuaDuck.ReturnType type) {
                if (stackTrace.frameList.isEmpty()) return;
                doPause(ev -> ev.setReason(StoppedEventArgumentsReason.PAUSE));
            }

            @Override
            public void instruction(LuaClosure luaClosure, Varargs varargs, LuaValue[] stack, int instruction, int pc) {
                if (!isInstructionPausable(pc, luaClosure.p)) return;
                doPause(ev -> ev.setReason(StoppedEventArgumentsReason.PAUSE));
            }

            @Override
            public void intoJavaFunction(Varargs args, Method val$method, LuaDuck.CallType type) {
                doPause(ev -> ev.setReason(StoppedEventArgumentsReason.PAUSE));
            }

            @Override
            public void outOfJavaFunction(Varargs args, Method val$method, Object result, LuaDuck.ReturnType type) {
                if (stackTrace.frameList.isEmpty()) return;
                doPause(ev -> ev.setReason(StoppedEventArgumentsReason.PAUSE));
            }
        });
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Void> stepIn(StepInArguments args) {
        if (args.getThreadId() != 0) {
            return Util.fail(ResponseErrorCode.InvalidParams, "Unknown thread");
        }
        StackTraceTracker.LuaFrame luaFrame = stackTrace.frameList.get(stackTrace.frameList.size() - 1).right().orElse(null);

        int line;
        int s;

        line = luaFrame != null ? luaFrame.line : 0;
        s = stackTrace.frameList.size() - 1;
        doContextualHook(new SecondaryCallHook() {
            @Override
            public void instruction(LuaClosure luaClosure, Varargs varargs, LuaValue[] stack, int instruction, int pc) {
                if (!isInstructionPausable(pc, luaClosure.p)) return;
                Either<StackTraceTracker.JavaFrame, StackTraceTracker.LuaFrame> either = stackTrace.frameList.get(stackTrace.frameList.size() - 1);
                StackTraceTracker.LuaFrame frame = either.right().orElseThrow();  // must be LuaFrame since we're here
                if (frame != luaFrame || line != luaFrame.closure.p.lineinfo[pc]) {
                    doPause(arg -> arg.setReason(StoppedEventArgumentsReason.STEP));
                }
            }

            @Override
            public void outOfFunction(LuaClosure luaClosure, Varargs varargs, LuaValue[] stack, LuaDuck.ReturnType type) {
                if (type == LuaDuck.ReturnType.TAIL) return;
                if (stackTrace.frameList.size() - 1 < s && !stackTrace.frameList.isEmpty()) {
                    doPause(arg -> arg.setReason(StoppedEventArgumentsReason.STEP));
                }
            }
        });
        doContinue();
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Void> stepOut(StepOutArguments args) {
        if (args.getThreadId() != 0) {
            return Util.fail(ResponseErrorCode.InvalidParams, "Unknown thread");
        }
        int s = stackTrace.frameList.size() - 1;
        doContextualHook(new SecondaryCallHook() {
            @Override
            public void outOfFunction(LuaClosure luaClosure, Varargs varargs, LuaValue[] stack, LuaDuck.ReturnType type) {
                if (type == LuaDuck.ReturnType.TAIL) return;
                if (stackTrace.frameList.size() - 1 < s) {
                    doPause(arg -> arg.setReason(StoppedEventArgumentsReason.STEP));
                }
            }
        });
        doContinue();
        return CompletableFuture.completedFuture(null);
    }

    @Blocking
    public void doPause(Consumer<StoppedEventArguments> filler) {
        if (isInBreakpoint) return;
        if (situationalBreakpointRemover != null) {
            situationalBreakpointRemover.run();
        }
        StoppedEventArguments arguments = new StoppedEventArguments();
        arguments.setThreadId(0);
        filler.accept(arguments);
        inspector = new StackTraceInspector(this, stackTrace);
        client.stopped(arguments);
        paused = new CompletableFuture<>();
        paused.join();
    }

    private void doContinue() {
        inspector = null;
        ContinuedEventArguments continuedEventArguments = new ContinuedEventArguments();
        continuedEventArguments.setThreadId(0);
        client.continued(continuedEventArguments);
        paused.complete(null);
    }

    private void doContextualHook(SecondaryCallHook hook) {
        com.github.applejuiceyy.figuraextras.mixin.figura.LuaRuntimeAccessor luaRuntime = (com.github.applejuiceyy.figuraextras.mixin.figura.LuaRuntimeAccessor) getCurrentAvatar().luaRuntime;
        GlobalsAccess globals = ((GlobalsAccess) luaRuntime.getUserGlobals());
        situationalBreakpointRemover = globals.figuraExtrass$getCaptureEventSource().subscribe(hook);
    }

    // it's better to separate what is DA and internal state and just internal talking to eachother
    public class DAInternalInterface {
        List<Either<StackTraceTracker.JavaFrame, StackTraceTracker.LuaFrame>> copyOnError;

        public void scriptInitializing(String str) {
            sourcer.regularSourceRegistered(str);
            Either<String, Integer> left = Either.left(str);
            if (breakpoints.containsKey(left)) {
                Avatar currentAvatar = getCurrentAvatar();
                Map<String, String> scripts = ((LuaRuntimeAccessor) currentAvatar.luaRuntime).getScripts();

                List<BreakpointState> breakpointStates = breakpoints.get(left);
                updateBreakpointAvailability(breakpointStates.stream().map(bs -> bs.breakpoint)::iterator, scripts.get(str));
                for (BreakpointState newBreakpoint : breakpointStates) {
                    BreakpointEventArguments breakpointEventArguments = new BreakpointEventArguments();
                    breakpointEventArguments.setBreakpoint(newBreakpoint.breakpoint);
                    breakpointEventArguments.setReason(BreakpointEventArgumentsReason.CHANGED);
                    client.breakpoint(breakpointEventArguments);
                }
            }
        }

        public void avatarErrored(Throwable e) {
            for (Map.Entry<Either<String, Integer>, List<BreakpointState>> entry : breakpoints.entrySet()) {
                for (BreakpointState breakpoint : entry.getValue()) {
                    breakpoint.breakpoint.setVerified(false);
                    BreakpointEventArguments breakpointEventArguments = new BreakpointEventArguments();
                    breakpointEventArguments.setBreakpoint(breakpoint.breakpoint);
                    breakpointEventArguments.setReason(BreakpointEventArgumentsReason.CHANGED);
                    client.breakpoint(breakpointEventArguments);
                }
            }
        }

        public boolean cares(Avatar avatar) {
            return avatar == AvatarManager.getLoadedAvatar(FiguraMod.getLocalPlayerUUID());
        }

        public boolean shouldBeStoppedByDebuggerAPI() {
            return canBeStoppedByBreakpointCall;
        }

        public boolean isPaused() {
            return !paused.isDone();
        }

        public void luaRuntimeBooting(FiguraLuaRuntime runtime) {
            ((LuaRuntimeAccess) runtime).figuraExtrass$dynamicLoadsEvent().subscribe(new SourceListener() {
                @Override
                public void added(String prototype, String source, int name) {
                    sourcer.registerNewDynamicLoad(prototype, name);
                }

                @Override
                public void removed(String chunkName, String source, int name) {
                    sourcer.unregisterDynamicLoad(chunkName, name);
                }
            });
        }

        public void avatarBooting(Avatar owner, Globals userGlobals) {
            destroyers.subscribe(((AvatarAccess) owner).figuraExtrass$getChatRedirect().getSource().subscribe((component, kind) -> {
                String category = kind == FiguraLuaPrinterDuck.Kind.ERRORS ? OutputEventArgumentsCategory.STDERR : OutputEventArgumentsCategory.STDOUT;
                OutputEventArguments outputEventArguments = new OutputEventArguments();
                outputEventArguments.setCategory(category);
                outputEventArguments.setOutput(component.getString());
                if (stackTrace.isActive) {
                    List<Either<StackTraceTracker.JavaFrame, StackTraceTracker.LuaFrame>> toUse;
                    if (kind == FiguraLuaPrinterDuck.Kind.ERRORS && copyOnError != null) {
                        toUse = copyOnError;
                    } else {
                        toUse = stackTrace.frameList;
                    }
                    if (toUse.size() > 0) {
                        Either<StackTraceTracker.JavaFrame, StackTraceTracker.LuaFrame> last = toUse.get(toUse.size() - 1);
                        if (last.right().isPresent()) {
                            StackTraceTracker.LuaFrame luaFrame = last.right().get();
                            outputEventArguments.setLine(luaFrame.closure.p.lineinfo[luaFrame.pc]);
                            outputEventArguments.setSource(sourcer.toSource(luaFrame.closure.p));
                        }
                    }
                }
                client.output(outputEventArguments);
                return true;
            }));
            Runnable runnable = ((GlobalsAccess) userGlobals).figuraExtrass$getCaptureEventSource().subscribe(new SecondaryCallHook() {
                int reentrantCount = 0;

                @Override
                public void intoFunction(LuaClosure luaClosure, Varargs varargs, LuaValue[] stack, LuaDuck.CallType type, String possibleName) {
                    StackTraceTracker.LuaFrame luaFrame = new StackTraceTracker.LuaFrame();
                    luaFrame.closure = luaClosure;
                    luaFrame.possibleName = possibleName;
                    luaFrame.stack = stack;
                    luaFrame.callType = type;
                    luaFrame.varargs = varargs;
                    stackTrace.frameList.add(Either.right(luaFrame));
                }

                @Override
                public void outOfFunction(LuaClosure luaClosure, Varargs varargs, LuaValue[] stack, LuaDuck.ReturnType type) {
                    if (type == LuaDuck.ReturnType.ERROR && copyOnError == null) {
                        copyOnError = new ArrayList<>(stackTrace.frameList);
                    }
                    stackTrace.frameList.remove(stackTrace.frameList.size() - 1);
                }

                @Override
                public void instruction(LuaClosure luaClosure, Varargs varargs, LuaValue[] stack, int instruction, int pc) {
                    copyOnError = null;
                    int p = luaClosure.p.lineinfo[pc];

                    WeakHashMap<Prototype, Integer> markedAsLoadStringed = ((LuaRuntimeAccess) getCurrentAvatar().luaRuntime)
                            .figuraExtrass$getPrototypesMarkedAsLoadStringed();

                    Either<String, Integer> sourceIndex;
                    if (markedAsLoadStringed.containsKey(luaClosure.p)) {
                        sourceIndex = Either.right(markedAsLoadStringed.get(luaClosure.p));
                    } else {
                        sourceIndex = Either.left(luaClosure.p.source.toString());
                    }

                    StackTraceTracker.LuaFrame luaFrame = stackTrace.frameList.get(stackTrace.frameList.size() - 1).right().orElseThrow();
                    luaFrame.line = p;
                    luaFrame.pc = pc;

                    boolean hasPaused = false;
                    boolean triggeredBurntBreakpoint = false;

                    if (isInstructionPausable(pc, luaClosure.p) && breakpoints.containsKey(sourceIndex)) {
                        for (BreakpointState breakpointState : breakpoints.get(sourceIndex)) {
                            Breakpoint breakpoint = breakpointState.breakpoint;
                            if (breakpoint.getLine() == p) {
                                if (burntBreakpoint == breakpoint.getId()) {
                                    triggeredBurntBreakpoint = true;
                                } else if (!hasPaused) {
                                    triggeredBurntBreakpoint = true;
                                    burntBreakpoint = breakpoint.getId();
                                    isInBreakpoint = true;
                                    if (breakpointState.process()) {
                                        isInBreakpoint = false;
                                        doPause(arg -> {
                                            arg.setReason(StoppedEventArgumentsReason.BREAKPOINT);
                                            arg.setHitBreakpointIds(new Integer[]{breakpoint.getId()});
                                        });
                                        hasPaused = true;
                                    }
                                    isInBreakpoint = false;
                                }
                            }
                        }
                    }
                    if (!triggeredBurntBreakpoint) {
                        burntBreakpoint = -1;
                    }
                }

                @Override
                public void intoJavaFunction(Varargs args, Method val$method, LuaDuck.CallType type) {
                    StackTraceTracker.JavaFrame javaFrame = new StackTraceTracker.JavaFrame();
                    javaFrame.javaFrame = val$method.getName();
                    javaFrame.arguments = args;
                    stackTrace.frameList.add(Either.left(javaFrame));
                }

                @Override
                public void outOfJavaFunction(Varargs args, Method val$method, Object result, LuaDuck.ReturnType type) {
                    stackTrace.frameList.remove(stackTrace.frameList.size() - 1);
                }

                @Override
                public void startEvent(String runReason, Object toRun, Varargs val) {
                    if (stackTrace.isActive) {
                        // onPlaySound event causes Avatar.run to be reentrant
                        // we will discard it under a compromise
                        reentrantCount += 1;
                        return;
                    }
                    stackTrace.frameList.clear();
                    stackTrace.isActive = true;
                    stackTrace.kickstarter = runReason;
                }

                @Override
                public void startInit(String name) {
                    stackTrace.frameList.clear();
                    stackTrace.isActive = true;
                    stackTrace.kickstarter = "Initialization";
                }

                @Override
                public void end() {
                    if (reentrantCount > 0) {
                        reentrantCount--;
                        return;
                    }
                    if (!stackTrace.frameList.isEmpty()) {
                        logger.warn("frame list is not empty");
                    }
                    stackTrace.isActive = false;
                }
            });
            destroyers.subscribe(runnable);
        }
    }
}
