package com.github.applejuiceyy.luabridge.asm;

import org.objectweb.asm.MethodVisitor;

import java.util.Arrays;

import static org.objectweb.asm.Opcodes.*;

public abstract class TypeInfo {
    public static final int[] ARRAY_ATYPE = new int[]{
            T_INT,
            0,
            T_BOOLEAN,
            T_BYTE,
            T_CHAR,
            T_SHORT,
            T_DOUBLE,
            T_FLOAT,
            T_LONG
    };
    public static final Class<?>[] PRIMITIVE_BOXES = new Class[]{
            Integer.class,
            Void.class,
            Boolean.class,
            Byte.class,
            Character.class,
            Short.class,
            Double.class,
            Float.class,
            Long.class
    };
    public static final String[] PRIMITIVES_BOXES_NAME = Arrays.stream(PRIMITIVE_BOXES).map(Class::getName).toArray(String[]::new);
    public static final String[] PRIMITIVES_BOXES_DESCRIPTOR_NAME = Arrays.stream(PRIMITIVES_BOXES_NAME).map(s -> "L" + s.replace('.', '/') + ";").toArray(String[]::new);
    public static final Class<?>[] PRIMITIVES = new Class[]{
            Integer.TYPE,
            Void.TYPE,
            Boolean.TYPE,
            Byte.TYPE,
            Character.TYPE,
            Short.TYPE,
            Double.TYPE,
            Float.TYPE,
            Long.TYPE
    };
    public static final String[] PRIMITIVES_NAME = Arrays.stream(PRIMITIVES).map(Class::getName).toArray(String[]::new);
    public static final char[] PRIMITIVES_INTERNAL_NAME = new char[]{
            'I',
            'V',
            'Z',
            'B',
            'C',
            'S',
            'D',
            'F',
            'J'
    };

    // primitive  class          class            array       array
    // int.class, Integer.class, ArrayList.class, int[].class ArrayList[].class
    static TypeInfo fromClass(Class<?> cls) {
        return fromRegularName(cls.getName());
    }

    // prm  class              class                 ar  array
    // int, java.Lang.Integer, java.utils.ArrayList, [I, [Ljava.utils.ArrayList;
    static TypeInfo fromRegularName(String name) {
        if (name.charAt(0) == '[') {
            // arrays are already stack names except for the dots
            return new ReferenceTypeInfo(name.replace('.', '/'));
        }
        int i = find(PRIMITIVES_NAME, name);
        if (i != -1) {
            return new PrimitiveTypeInfo(PRIMITIVES_INTERNAL_NAME[i]);
        }
        return new ReferenceTypeInfo("L" + name.replace('.', '/') + ";");
    }

    public static <T> int find(T[] arr, T subject) {
        for (int i = 0; i < arr.length; i++) {
            if (arr[i].equals(subject)) {
                return i;
            }
        }
        return -1;
    }

    // p  class              class                 ar array
    // I, java/lang/Integer, java/utils/ArrayList, [I [Ljava/utils/ArrayList;
    static TypeInfo fromInternalName(String internalName) {
        if (internalName.charAt(0) == '[') {
            // arrays are already stack names except for the dots
            return new ReferenceTypeInfo(internalName);
        }
        if (internalName.length() == 1) {
            int i = findC(PRIMITIVES_INTERNAL_NAME, internalName.charAt(0));
            if (i != -1) {
                return new PrimitiveTypeInfo(internalName.charAt(0));
            }
        }
        return new ReferenceTypeInfo("L" + internalName + ";");
    }

    public static int findC(char[] arr, char subject) {
        for (int i = 0; i < arr.length; i++) {
            if (arr[i] == subject) {
                return i;
            }
        }
        return -1;
    }

    // p  class                class                   ar array
    // I, Ljava/lang/Integer;, Ljava/utils/ArrayList;, [I [Ljava/utils/ArrayList;
    static TypeInfo fromDescriptorName(String stackName) {
        char subject = stackName.charAt(0);
        if (subject == '[') {
            return new ReferenceTypeInfo(stackName);
        }
        if (stackName.length() == 1) {
            int i = findC(PRIMITIVES_INTERNAL_NAME, subject);
            if (i != -1) {
                return new PrimitiveTypeInfo(stackName.charAt(0));
            }
        }
        return new ReferenceTypeInfo(stackName);
    }

    public abstract Class<?> toClass();

    public abstract String toNormal();

    public abstract String toInternal();

    public abstract ReferenceTypeInfo toBox(MethodVisitor visitor);

    public abstract PrimitiveTypeInfo toPrimitive(MethodVisitor visitor);

    public String andArguments(TypeInfo[] arguments) {
        return returnAndArguments(this, arguments);
    }

    static String returnAndArguments(TypeInfo returnValue, TypeInfo[] arguments) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("(");
        for (TypeInfo argument : arguments) {
            stringBuilder.append(argument.toDescriptor());
        }
        stringBuilder.append(")");
        stringBuilder.append(returnValue.toDescriptor());
        return stringBuilder.toString();
    }

    public abstract String toDescriptor();

    public static class ReferenceTypeInfo extends TypeInfo {
        private final String descriptorName;

        public ReferenceTypeInfo(String descriptorName) {
            this.descriptorName = descriptorName;
        }

        @Override
        public Class<?> toClass() {
            try {
                return Class.forName(toNormal(), false, TypeInfo.class.getClassLoader());
            } catch (ClassNotFoundException e) {
                return null;
            }
        }

        int getArrayDepth() {
            int i = 0;
            while (descriptorName.charAt(i) == '[') i++;
            return i;
        }

        @Override
        public String toNormal() {
            return toInternal().replace('/', '.');
        }

        @Override
        public String toInternal() {
            if (getArrayDepth() > 0) {
                return descriptorName;
            }
            return descriptorName.substring(1, descriptorName.length() - 1);
        }

        @Override
        public ReferenceTypeInfo toBox(MethodVisitor visitor) {
            if (isBox()) {
                return this;
            }
            return null;
        }

        boolean isBox() {
            return find(PRIMITIVES_BOXES_DESCRIPTOR_NAME, descriptorName) != -1;
        }

        // Box -> primitive
        @Override
        public PrimitiveTypeInfo toPrimitive(MethodVisitor visitor) {
            if (isBox()) {
                String name = PRIMITIVES_NAME[TypeInfo.find(PRIMITIVES_BOXES_NAME, descriptorName)];
                PrimitiveTypeInfo p = (PrimitiveTypeInfo) fromDescriptorName(name);
                visitor.visitMethodInsn(
                        INVOKESTATIC,
                        toInternal(),
                        name + "Value",
                        "()" + p.toDescriptor(),
                        false
                );
                return p;
            }
            return null;
        }

        @Override
        public String toDescriptor() {
            return descriptorName;
        }

        @Override
        public String toString() {
            return "Reference{" + toNormal() + '}';
        }
    }

    public static class PrimitiveTypeInfo extends TypeInfo {
        private final char descriptorName;

        public PrimitiveTypeInfo(char descriptorName) {
            this.descriptorName = descriptorName;
        }

        @Override
        public Class<?> toClass() {
            return PRIMITIVES[findC(PRIMITIVES_INTERNAL_NAME, descriptorName)];
        }

        @Override
        public String toNormal() {
            return PRIMITIVES_NAME[findC(PRIMITIVES_INTERNAL_NAME, descriptorName)];
        }

        @Override
        public String toInternal() {
            return String.valueOf(descriptorName);
        }

        @Override
        public ReferenceTypeInfo toBox(MethodVisitor visitor) {
            String name = PRIMITIVES_BOXES_DESCRIPTOR_NAME[TypeInfo.findC(PRIMITIVES_INTERNAL_NAME, descriptorName)];
            ReferenceTypeInfo p = (ReferenceTypeInfo) fromDescriptorName(name);
            visitor.visitMethodInsn(
                    INVOKESTATIC,
                    p.toInternal(),
                    "valueOf",
                    "(" + descriptorName + ")" + p.toDescriptor(),
                    false
            );
            return p;
        }

        @Override
        public PrimitiveTypeInfo toPrimitive(MethodVisitor visitor) {
            return this;
        }

        @Override
        public String toDescriptor() {
            return String.valueOf(descriptorName);
        }

        @Override
        public String toString() {
            return "PrimitiveTypeInfo{" + toNormal() + '}';
        }
    }
}
