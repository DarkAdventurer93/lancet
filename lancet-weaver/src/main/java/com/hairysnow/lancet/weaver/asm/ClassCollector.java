package com.hairysnow.lancet.weaver.asm;


import com.hairysnow.lancet.weaver.ClassData;
import com.hairysnow.lancet.weaver.graph.Graph;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.util.HashMap;
import java.util.Map;


/**
 * Created by Jude on 2017/4/25.
 */

public class ClassCollector {

    // canonical name
    String originClassName;
    ClassWriter originClassWriter;

    ClassReader mClassReader;
    Graph graph;

    // simple name of innerClass
    Map<String, ClassWriter> mClassWriters = new HashMap<>();

    public ClassCollector(ClassReader mClassReader, Graph graph) {
        this.mClassReader = mClassReader;
        this.graph = graph;
    }

    void setOriginClassName(String originClassName) {
        this.originClassName = originClassName;
    }

    public ClassVisitor getOriginClassVisitor() {
        if(originClassWriter ==null){
            originClassWriter = new ClassWriter(mClassReader, 0);
        }
        return originClassWriter;
    }

    public ClassVisitor getInnerClassVisitor(String classSimpleName) {
        ClassWriter writer = mClassWriters.get(classSimpleName);
        if (writer == null) {
            writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
            initForWriter(writer, classSimpleName);
            mClassWriters.put(classSimpleName, writer);
        }
        return writer;
    }

    private void initForWriter(ClassVisitor visitor, String classSimpleName) {
        visitor.visit(Opcodes.V1_7, Opcodes.ACC_SUPER, getCanonicalName(classSimpleName), null, "java/lang/Object", null);
        MethodVisitor mv = visitor.visitMethod(Opcodes.ACC_PRIVATE, "<init>", "()V", null, null);
        mv.visitCode();
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(1, 1);
        mv.visitEnd();
    }

    public ClassData[] generateClassBytes() {
        for (String className : mClassWriters.keySet()) {
            originClassWriter.visitInnerClass(getCanonicalName(className), originClassName, className, Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC);
        }
        ClassData[] classDataArray = new ClassData[mClassWriters.size() + 1];
        int index = 0;
        for (Map.Entry<String, ClassWriter> entry : mClassWriters.entrySet()) {
            classDataArray[index] = new ClassData(entry.getValue().toByteArray(), getCanonicalName(entry.getKey()));
            index++;
        }
        classDataArray[index] = new ClassData(originClassWriter.toByteArray(), originClassName);
        return classDataArray;
    }

    public String getCanonicalName(String simpleName) {
        return originClassName + "$" + simpleName;
    }
}
