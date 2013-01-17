package com.leozc.lantern;
import  org.objectweb.asm.Label;
import org.apache.commons.lang3.StringUtils;
import org.objectweb.asm.ClassAdapter;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.MethodAdapter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.commons.*;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class MethodcallerMatcher {
    private String targetClass;
    private Method targetMethod;

    private AppClassVisitor cv;

    private ArrayList<Callee> callees = new ArrayList<Callee>();
    public List<Callee> getCallees(){
        return callees;
    }

    public String getTargetClass() {
        return targetClass;
    }

    public Method getTargetMethod() {
        return targetMethod;
    }

    protected  class Callee {
        String className;
        String methodName;
        String methodDesc;
        String source;
        int line;

        public Callee(String cName, String mName, String mDesc, String src, int ln) {
            className = cName; methodName = mName; methodDesc = mDesc; source = src; line = ln;
        }
    }

    private class AppMethodVisitor extends MethodAdapter {

        boolean callsTarget;
        int line;

        public AppMethodVisitor() { super(new EmptyVisitor()); }

        public void visitMethodInsn(int opcode, String owner, String name, String desc) {
            if (owner.equals(targetClass)
                    && name.equals(targetMethod.getName())
                    && desc.equals(targetMethod.getDescriptor())
                    )
            {
                callsTarget = true;
            }
        }

        public void visitCode() {
            callsTarget = false;
        }

        public void visitLineNumber(int line, Label start) {
            this.line = line;
        }

        public void visitEnd() {
            if (callsTarget)
                callees.add(new Callee(cv.className, cv.methodName, cv.methodDesc,
                        cv.source, line));
        }
    }

    private class AppClassVisitor extends ClassAdapter {

        private AppMethodVisitor mv = new AppMethodVisitor();

        public String source;
        public String className;
        public String methodName;
        public String methodDesc;

        public AppClassVisitor() { super(new EmptyVisitor()); }

        public void visit(int version, int access, String name,
                          String signature, String superName, String[] interfaces) {
            className = name;
        }

        public void visitSource(String source, String debug) {
            this.source = source;
        }

        public MethodVisitor visitMethod(int access, String name,
                                         String desc, String signature,
                                         String[] exceptions) {
            methodName = name;
            methodDesc = desc;

            return mv;
        }
    }


    public void findCallingMethodsInJar(String jarPath, String targetClass,
                                        String targetMethodDeclaration) throws Exception {

        this.targetClass = targetClass;
        this.targetMethod = Method.getMethod(targetMethodDeclaration);

        this.cv = new AppClassVisitor();

        JarFile jarFile = new JarFile(jarPath);
        Enumeration<JarEntry> entries = jarFile.entries();

        while (entries.hasMoreElements()) {
            JarEntry entry = entries.nextElement();

            if (entry.getName().endsWith(".class")) {
                InputStream stream = new BufferedInputStream(jarFile.getInputStream(entry), 1024);
                ClassReader reader = new ClassReader(stream);

                reader.accept(cv, 0);

                stream.close();
            }
        }
    }


    public static void main( String[] args ) {

        if(args.length<3){
            System.err.println("It takes three parameters $LIB $CLASS $MethodDesc");
            System.err.println("See run.sh for example");
            System.exit(-1);

        }
        try {
            MethodcallerMatcher app = new MethodcallerMatcher();

            String classname = args[0];
            String methoddesc = args[1];

            System.out.println("CLASS: "+classname);
            System.out.println("MethodDesc: "+methoddesc);
            System.out.println("LIBs: "+ StringUtils.join(Arrays.asList(args).subList(2,args.length-1), ";"));

            for(int i=2;i<args.length;i++){   //for each libs
                app.findCallingMethodsInJar(args[i], classname, methoddesc);

                for (Callee c : app.callees) {
                    System.out.println("== "+c.source+":"+c.line+" "+c.className+" "+c.methodName+" "+c.methodDesc);
                }

                System.out.println("--\n"+app.callees.size()+" methods invoke "+
                        app.targetClass+" "+
                        app.targetMethod.getName()+" "+app.targetMethod.getDescriptor());
            }


        } catch(Exception x) {
            x.printStackTrace();
        }
    }

}
