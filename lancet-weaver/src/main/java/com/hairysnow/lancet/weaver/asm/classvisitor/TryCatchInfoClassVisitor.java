package com.hairysnow.lancet.weaver.asm.classvisitor;

import com.hairysnow.lancet.weaver.asm.LinkedClassVisitor;
import com.hairysnow.lancet.weaver.asm.classvisitor.methodvisitor.TryCatchMethodVisitor;
import com.hairysnow.lancet.weaver.entity.TryCatchInfo;
import com.hairysnow.lancet.weaver.log.Log;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.util.List;
import java.util.stream.Collectors;



/**
 * Created by gengwanpeng on 17/3/27.
 */
public class TryCatchInfoClassVisitor extends LinkedClassVisitor {

    private String className;
    private List<TryCatchInfo> infos;
    private List<TryCatchInfo> matches = null;


    public TryCatchInfoClassVisitor(List<TryCatchInfo> infos) {
        this.infos = infos;
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        className = name;
        if (infos != null){
            matches = infos.stream().filter(t -> t.match(name)).collect(Collectors.toList());
        }
        super.visit(version, access, name, signature, superName, interfaces);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
        if (matches != null && matches.size() > 0) {
            Log.tag("transform").i("visit TryCatch method: "+className+"."+name+" "+desc);
            mv = new TryCatchMethodVisitor(Opcodes.ASM7, mv, matches);
        }
        return mv;
    }
}
