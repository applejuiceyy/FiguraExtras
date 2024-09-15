package com.github.applejuiceyy.luabridge.asm;

import org.apache.commons.lang3.tuple.Pair;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

import java.lang.invoke.StringConcatFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static org.objectweb.asm.Opcodes.*;

public class ASMUtil {
    public static void emitThrow(MethodVisitor visitor, Type type, String arg) {
        emitThrow(visitor, type, () -> {
            visitor.visitLdcInsn(arg);
            return new Type[]{Type.getType(String.class)};
        });
    }

    public static void emitThrow(MethodVisitor visitor, Type type, Supplier<Type[]> executor) {
        String internalName = type.getInternalName();
        // -> Class
        visitor.visitTypeInsn(NEW, internalName);
        // Class -> Class Class
        visitor.visitInsn(DUP);
        // Class Class -> Class Class ...
        Type[] args = executor.get();
        // Class Class ... -> Throwable
        visitor.visitMethodInsn(
                INVOKESPECIAL,
                internalName,
                "<init>",
                Type.getMethodDescriptor(Type.VOID_TYPE, args),
                false
        );

        // IAE ->
        visitor.visitInsn(ATHROW);
    }

    public static void emitIf(MethodVisitor visitor, Consumer<Label> conditionalJumper, Runnable ifTrue) {
        Label falseLabel = new Label();

        conditionalJumper.accept(falseLabel);
        ifTrue.run();
        visitor.visitLabel(falseLabel);
    }

    public static void emitIfElse(MethodVisitor visitor, Consumer<Label> conditionalJumper, Runnable ifTrue, Runnable ifFalse) {
        Label falseLabel = new Label();
        Label after = new Label();

        conditionalJumper.accept(falseLabel);
        ifTrue.run();
        visitor.visitJumpInsn(GOTO, after);
        visitor.visitLabel(falseLabel);
        ifFalse.run();
        visitor.visitLabel(after);
    }

    public static void emitWhile(MethodVisitor visitor, Consumer<Label> conditionalJumper, Runnable loopBlock) {
        Label start = new Label();
        Label after = new Label();

        visitor.visitLabel(start);
        conditionalJumper.accept(after);
        loopBlock.run();
        visitor.visitJumpInsn(GOTO, start);
        visitor.visitLabel(after);
    }

    public static TryCatchBuilder<Void> emitTryCatch(MethodVisitor visitor, Runnable trialBlock) {
        return emitTryCatch(visitor, () -> {
            trialBlock.run();
            return null;
        });
    }

    public static <R> TryCatchBuilder<R> emitTryCatch(MethodVisitor visitor, Supplier<R> trialBlock) {
        return new TryCatchBuilder<>(visitor, trialBlock);
    }

    public static void emitStringConcat(MethodVisitor visitor, Type[] dynamics, boolean startDynamic, String... statics) {
        String SCFInternal = Type.getInternalName(StringConcatFactory.class);
        String arguments;
        if (dynamics.length >= statics.length - 1) {
            arguments = String.join("\u0001", statics);
        } else {
            throw new IllegalStateException("dynamics should have a length of statics that is one lower or greater than that");
        }
        if (dynamics.length == statics.length) {
            if (startDynamic) {
                arguments = "\u0001" + arguments;
            } else {
                arguments = arguments + "\u0001";
            }
        } else if (dynamics.length == statics.length + 1) {
            arguments = "\u0001" + arguments + "\u0001";
        } else if (dynamics.length > statics.length + 1) {
            throw new IllegalStateException("dynamics should have a length of statics that is one higher or lesser than that");
        }

        visitor.visitInvokeDynamicInsn(
                "makeConcatWithConstants",
                Type.getMethodDescriptor(Type.getType(String.class), dynamics),
                new Handle(
                        H_INVOKESTATIC,
                        SCFInternal,
                        "makeConcatWithConstants",
                        "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite;",
                        false
                ),
                arguments
        );
    }

    public static class TryCatchBuilder<R> {
        private final MethodVisitor visitor;
        private final Supplier<R> trialBlock;
        private final List<Pair<Type, Runnable>> catchBlocks = new ArrayList<>();
        private Runnable finallyBlock;

        private TryCatchBuilder(MethodVisitor visitor, Supplier<R> trialBlock) {
            this.visitor = visitor;
            this.trialBlock = trialBlock;
        }

        public TryCatchBuilder<R> emitEmptyCatch(Type type) {
            return emitCatch(type, () -> visitor.visitInsn(POP));
        }

        public TryCatchBuilder<R> emitCatch(Type type, Runnable catchBlock) {
            catchBlocks.add(Pair.of(type, catchBlock));
            return this;
        }

        public R emitFinally(Runnable finallyBlock) {
            this.finallyBlock = finallyBlock;
            return emit();
        }

        public R emit() {
            Label startTryBlock = new Label();
            Label endTryBlock = new Label();
            Label endCatchesBlock = new Label();
            Label finallyRethrowBlock = new Label();
            Label finallyBlock = new Label();

            List<Label> labels = new ArrayList<>();
            for (Pair<Type, Runnable> catchBlock : catchBlocks) {
                Label handlerBlock = new Label();
                visitor.visitTryCatchBlock(startTryBlock, endTryBlock, handlerBlock, catchBlock.getLeft().getInternalName());
                labels.add(handlerBlock);
            }

            if (this.finallyBlock != null) {
                visitor.visitTryCatchBlock(startTryBlock, endCatchesBlock, finallyRethrowBlock, null);
            }

            visitor.visitLabel(startTryBlock);
            R r = trialBlock.get();
            visitor.visitLabel(endTryBlock);

            visitor.visitJumpInsn(GOTO, finallyBlock);

            for (int i = 0, labelsSize = labels.size(); i < labelsSize; i++) {
                Label label = labels.get(i);

                visitor.visitLabel(label);
                catchBlocks.get(i).getRight().run();
                visitor.visitJumpInsn(GOTO, finallyBlock);
            }

            visitor.visitLabel(endCatchesBlock);

            if (this.finallyBlock == null) {
                visitor.visitLabel(finallyBlock);
                return r;
            }
            visitor.visitLabel(finallyRethrowBlock);
            this.finallyBlock.run();
            visitor.visitInsn(ATHROW);
            visitor.visitLabel(finallyBlock);
            this.finallyBlock.run();

            return r;
        }
    }
}
