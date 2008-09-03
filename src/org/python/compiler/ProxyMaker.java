// Copyright (c) Corporation for National Research Initiatives
package org.python.compiler;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.python.core.Py;
import org.python.objectweb.asm.Label;
import org.python.objectweb.asm.Opcodes;

public class ProxyMaker implements ClassConstants, Opcodes
{
    public static final int tBoolean=0;
    public static final int tByte=1;
    public static final int tShort=2;
    public static final int tInteger=3;
    public static final int tLong=4;
    public static final int tFloat=5;
    public static final int tDouble=6;
    public static final int tCharacter=7;
    public static final int tVoid=8;
    public static final int tOther=9;
    public static final int tNone=10;

    public static Map<Class<?>, Integer> types=fillTypes();

    public static Map<Class<?>, Integer> fillTypes() {
        Map<Class<?>, Integer> typeMap = new HashMap<Class<?>, Integer>();
        typeMap.put(Boolean.TYPE, tBoolean);
        typeMap.put(Byte.TYPE, tByte);
        typeMap.put(Short.TYPE, tShort);
        typeMap.put(Integer.TYPE, tInteger);
        typeMap.put(Long.TYPE, tLong);
        typeMap.put(Float.TYPE, tFloat);
        typeMap.put(Double.TYPE, tDouble);
        typeMap.put(Character.TYPE, tCharacter);
        typeMap.put(Void.TYPE, tVoid);
        return typeMap;
    }

    public static int getType(Class<?> c) {
        if (c == null) return tNone;
        Object i = types.get(c);
        if (i == null) return tOther;
        else return ((Integer)i).intValue();
    }

    Class<?> superclass;
    Class<?>[] interfaces;
    Set<String> names;
    Set<String> supernames = new HashSet<String>();
    public ClassFile classfile;
    public String myClass;
    public boolean isAdapter=false;

    public ProxyMaker(String classname, Class<?> superclass) {
        this.myClass = "org.python.proxies."+classname;
        if (superclass.isInterface()) {
            this.superclass = Object.class;
            this.interfaces = new Class[] {superclass};
        } else {
            this.superclass = superclass;
            this.interfaces = new Class[0];
        }
    }

    public ProxyMaker(String myClass, Class<?> superclass, Class<?>[] interfaces) {
        this.myClass = myClass;
        if (superclass == null)
            superclass = Object.class;
        this.superclass = superclass;
        if (interfaces == null)
            interfaces = new Class[0];
        this.interfaces = interfaces;
    }

    public static String mapClass(Class<?> c) {
        String name = c.getName();
        int index = name.indexOf(".");
        if (index == -1)
            return name;

        StringBuffer buf = new StringBuffer(name.length());
        int last_index = 0;
        while (index != -1) {
            buf.append(name.substring(last_index, index));
            buf.append("/");
            last_index = index+1;
            index = name.indexOf(".", last_index);
        }
        buf.append(name.substring(last_index, name.length()));
        return buf.toString();
    }

    public static String mapType(Class<?> type) {
        if (type.isArray())
            return "["+mapType(type.getComponentType());

        switch (getType(type)) {
        case tByte: return "B";
        case tCharacter:  return "C";
        case tDouble:  return "D";
        case tFloat:  return "F";
        case tInteger:  return "I";
        case tLong:  return "J";
        case tShort:  return "S";
        case tBoolean:  return "Z";
        case tVoid:  return "V";
        default:
            return "L"+mapClass(type)+";";
        }
    }

    public static String makeSignature(Class<?>[] sig, Class<?> ret) {
        StringBuffer buf=new StringBuffer();
        buf.append("(");
        for (int i=0; i<sig.length; i++) {
            buf.append(mapType(sig[i]));
        }
        buf.append(")");
        buf.append(mapType(ret));
        return buf.toString();
    }


    public void doConstants() throws Exception {
        Code code = classfile.addMethod("<clinit>", "()V", Modifier.STATIC);
        code.return_();
    }

    public static void doReturn(Code code, Class<?> type) throws Exception {
        switch (getType(type)) {
        case tNone:
            break;
        case tCharacter:
        case tBoolean:
        case tByte:
        case tShort:
        case tInteger:
            code.ireturn();
            break;
        case tLong:
            code.lreturn();
            break;
        case tFloat:
            code.freturn();
            break;
        case tDouble:
            code.dreturn();
            break;
        case tVoid:
            code.return_();
            break;
        default:
            code.areturn();
            break;
        }
    }

    public static void doNullReturn(Code code, Class<?> type) throws Exception {
        switch (getType(type)) {
        case tNone:
            break;
        case tCharacter:
        case tBoolean:
        case tByte:
        case tShort:
        case tInteger:
            code.iconst_0();
            code.ireturn();
            break;
        case tLong:
            code.lconst_0();
            code.lreturn();
            break;
        case tFloat:
            code.fconst_0();
            code.freturn();
            break;
        case tDouble:
            code.dconst_0();
            code.dreturn();
            break;
        case tVoid:
            code.return_();
            break;
        default:
            code.aconst_null();
            code.areturn();
            break;
        }
    }

    public void callSuper(Code code,
                          String name,
                          String superclass,
                          Class<?>[] parameters,
                          Class<?> ret,
                          String sig) throws Exception {

        code.aload(0);
        int local_index;
        int i;
        for (i=0, local_index=1; i<parameters.length; i++) {
            switch(getType(parameters[i])) {
            case tCharacter:
            case tBoolean:
            case tByte:
            case tShort:
            case tInteger:
                code.iload(local_index);
                local_index += 1;
                break;
            case tLong:
                code.lload(local_index);
                local_index += 2;
                break;
            case tFloat:
                code.fload(local_index);
                local_index += 1;
                break;
            case tDouble:
                code.dload(local_index);
                local_index += 2;
                break;
            default:
                code.aload(local_index);
                local_index += 1;
                break;
            }
        }
        code.invokespecial(superclass, name, sig);

        doReturn(code, ret);
    }

    public void doJavaCall(Code code, String name, String type,
                          String jcallName)
        throws Exception
    {
        code.invokevirtual("org/python/core/PyObject", jcallName, "(" + $objArr + ")" + $pyObj);
        code.invokestatic("org/python/core/Py", "py2"+name, "(" + $pyObj + ")"+type);
    }


    public void getArgs(Code code, Class<?>[] parameters) throws Exception {
        if (parameters.length == 0) {
            code.getstatic("org/python/core/Py", "EmptyObjects", $pyObjArr);
        } else {
            code.iconst(parameters.length);
            code.anewarray("java/lang/Object");

            int array = code.getLocal("[org/python/core/PyObject");
            code.astore(array);

            int local_index;
            int i;
            for (i=0, local_index=1; i<parameters.length; i++) {
                code.aload(array);
                code.iconst(i);

                switch (getType(parameters[i])) {
                case tBoolean:
                case tByte:
                case tShort:
                case tInteger:
                    code.iload(local_index);
                    local_index += 1;
                    code.invokestatic("org/python/core/Py", "newInteger", "(I)" + $pyInteger);
                    break;
                case tLong:
                    code.lload(local_index);
                    local_index += 2;
                    code.invokestatic("org/python/core/Py", "newInteger", "(J)" + $pyObj);
                    break;
                case tFloat:
                    code.fload(local_index);
                    local_index += 1;
                    code.invokestatic("org/python/core/Py", "newFloat", "(F)" + $pyFloat);
                    break;
                case tDouble:
                    code.dload(local_index);
                    local_index += 2;
                    code.invokestatic("org/python/core/Py", "newFloat", "(D)" + $pyFloat);
                    break;
                case tCharacter:
                    code.iload(local_index);
                    local_index += 1;
                    code.invokestatic("org/python/core/Py", "newString", "(C)" + $pyStr);
                    break;
                default:
                    code.aload(local_index);
                    local_index += 1;
                    break;
                }
                code.aastore();
            }
            code.aload(array);
        }
    }

    public void callMethod(Code code,
                           String name,
                           Class<?>[] parameters,
                           Class<?> ret,
                           Class<?>[] exceptions) throws Exception {
        Label start = null;
        Label end = null;
        
        String jcallName = "_jcall";
        int instLocal = 0;

        if (exceptions.length > 0) {
            start = new Label();
            end = new Label();
            jcallName = "_jcallexc";
            instLocal = code.getLocal("org/python/core/PyObject");
            code.astore(instLocal);
            code.label(start);
            code.aload(instLocal);
        }

        getArgs(code, parameters);

        switch (getType(ret)) {
        case tCharacter:
            doJavaCall(code, "char", "C", jcallName);
            break;
        case tBoolean:
            doJavaCall(code, "boolean", "Z", jcallName);
            break;
        case tByte:
        case tShort:
        case tInteger:
            doJavaCall(code, "int", "I", jcallName);
            break;
        case tLong:
            doJavaCall(code, "long", "J", jcallName);
            break;
        case tFloat:
            doJavaCall(code, "float", "F", jcallName);
            break;
        case tDouble:
            doJavaCall(code, "double", "D", jcallName);
            break;
        case tVoid:
            doJavaCall(code, "void", "V", jcallName);
            break;
        default:
            code.invokevirtual("org/python/core/PyObject", jcallName, "(" + $objArr + ")" + $pyObj);
            code.ldc(ret.getName());
            code.invokestatic("java/lang/Class","forName", "(" + $str + ")" + $clss);
            code.invokestatic("org/python/core/Py", "tojava", "(" + $pyObj + $clss + ")" + $obj);
            // I guess I need this checkcast to keep the verifier happy
            code.checkcast(mapClass(ret));
            break;
        }
        if (end != null) {
            code.label(end);
        }

        doReturn(code, ret);

        if (exceptions.length > 0) {
            boolean throwableFound = false;

            Label handlerStart = null;
            for (int i = 0; i < exceptions.length; i++) {
                handlerStart = new Label();
                code.label(handlerStart);
                int excLocal = code.getLocal("java/lang/Throwable");
                code.astore(excLocal);

                code.aload(excLocal);
                code.athrow();

                code.visitTryCatchBlock(start, end, handlerStart, mapClass(exceptions[i]));
                doNullReturn(code, ret);

                code.freeLocal(excLocal);
                if (exceptions[i] == Throwable.class)
                    throwableFound = true;
            }

            if (!throwableFound) {
                // The final catch (Throwable)
                handlerStart = new Label();
                code.label(handlerStart);
                int excLocal = code.getLocal("java/lang/Throwable");
                code.astore(excLocal);
                code.aload(instLocal);
                code.aload(excLocal);

                code.invokevirtual("org/python/core/PyObject", "_jthrow", "(" + $throwable + ")V");
                code.visitTryCatchBlock(start, end, handlerStart, "java/lang/Throwable");

                code.freeLocal(excLocal);
                doNullReturn(code, ret);
            }
            code.freeLocal(instLocal);
        }
    }


    public void addMethod(Method method, int access) throws Exception {
        boolean isAbstract = false;

        if (Modifier.isAbstract(access)) {
            access = access & ~Modifier.ABSTRACT;
            isAbstract = true;
        }

        Class<?>[] parameters = method.getParameterTypes();
        Class<?> ret = method.getReturnType();
        String sig = makeSignature(parameters, ret);

        String name = method.getName();
        names.add(name);

        Code code = classfile.addMethod(name, sig, access);

        code.aload(0);
        code.ldc(name);

        if (!isAbstract) {
            int tmp = code.getLocal("org/python/core/PyObject");
            code.invokestatic("org/python/core/Py", "jfindattr", "(" + $pyProxy + $str + ")" + $pyObj);
            code.astore(tmp);
            code.aload(tmp);

            Label callPython = new Label();
            code.ifnonnull(callPython);

            String superClass = mapClass(method.getDeclaringClass());

            callSuper(code, name, superClass, parameters, ret, sig);
            code.label(callPython);
            code.aload(tmp);
            callMethod(code, name, parameters, ret, method.getExceptionTypes());

            addSuperMethod("super__"+name, name, superClass, parameters,
                           ret, sig, access);
        } else {
            if (!isAdapter) {
                code.invokestatic("org/python/core/Py", "jgetattr", "(" + $pyProxy + $str + ")" + $pyObj);
                callMethod(code, name, parameters, ret, method.getExceptionTypes());
            } else {
                code.invokestatic("org/python/core/Py", "jfindattr", "(" + $pyProxy + $str + ")" + $pyObj);
                code.dup();
                Label returnNull = new Label();
                code.ifnull(returnNull);

                callMethod(code, name, parameters, ret, method.getExceptionTypes());
                code.label(returnNull);
                code.pop();
                doNullReturn(code, ret);
            }
        }
    }

    private String methodString(Method m) {
        StringBuffer buf = new StringBuffer(m.getName());
        buf.append(":");
        Class<?>[] params = m.getParameterTypes();
        for (int i = 0; i < params.length; i++) {
            buf.append(params[i].getName());
            buf.append(",");
        }
        return buf.toString();
    }

    protected void addMethods(Class<?> c, Set<String> t) throws Exception {
        Method[] methods = c.getDeclaredMethods();
        for (int i=0; i<methods.length; i++) {
            Method method = methods[i];
            String s = methodString(method);
            if (t.contains(s))
                continue;
            t.add(s);

            int access = method.getModifiers();
            if (Modifier.isStatic(access) || Modifier.isPrivate(access)) {
                continue;
            }

            if (Modifier.isNative(access)) {
                access = access & ~Modifier.NATIVE;
            }

            if (Modifier.isProtected(access)) {
                access = (access & ~Modifier.PROTECTED) | Modifier.PUBLIC;
                if (Modifier.isFinal(access)) {
                    addSuperMethod(methods[i], access);
                    continue;
                }
            } else if (Modifier.isFinal(access)) {
                continue;
            }
            addMethod(methods[i], access);
        }

        Class<?> sc = c.getSuperclass();
        if (sc != null)
            addMethods(sc, t);

        Class<?>[] ifaces = c.getInterfaces();
        for (int j=0; j<ifaces.length; j++) {
            addMethods(ifaces[j], t);
        }
    }

    public void addConstructor(String name,
                               Class<?>[] parameters,
                               Class<?> ret,
                               String sig,
                               int access) throws Exception {
        Code code = classfile.addMethod("<init>", sig, access);
        callSuper(code, "<init>", name, parameters, Void.TYPE, sig);
    }

    public void addConstructors(Class<?> c) throws Exception {
        Constructor<?>[] constructors = c.getDeclaredConstructors();
        String name = mapClass(c);
        for (int i = 0; i < constructors.length; i++) {
            int access = constructors[i].getModifiers();
            if (Modifier.isPrivate(access))
                continue;
            if (Modifier.isNative(access))
                access = access & ~Modifier.NATIVE;
            if (Modifier.isProtected(access))
                access = access & ~Modifier.PROTECTED | Modifier.PUBLIC;
            Class<?>[] parameters = constructors[i].getParameterTypes();
            String sig = makeSignature(parameters, Void.TYPE);
            addConstructor(name, parameters, Void.TYPE, sig, access);
        }
    }

    // Super methods are added for the following three reasons:
    //
    //   1) for a protected non-final method add a public method with no
    //   super__ prefix.  This gives needed access to this method for
    //   subclasses
    //
    //   2) for protected final methods, add a public method with the
    //   super__ prefix.  This avoids the danger of trying to override a
    //   final method
    //
    //   3) For any other method that is overridden, add a method with the
    //   super__ prefix.  This gives access to super. version or the
    //   method.
    //
    public void addSuperMethod(Method method, int access) throws Exception {
        Class<?>[] parameters = method.getParameterTypes();
        Class<?> ret = method.getReturnType();
        String sig = makeSignature(parameters, ret);
        String superClass = mapClass(method.getDeclaringClass());
        String superName = method.getName();
        String methodName = superName;
        if (Modifier.isFinal(access)) {
            methodName = "super__" + superName;
            access &= ~Modifier.FINAL;
        }
        addSuperMethod(methodName, superName, superClass, parameters,
                       ret, sig, access);
    }

    public void addSuperMethod(String methodName,
                               String superName,
                               String declClass,
                               Class<?>[] parameters,
                               Class<?> ret,
                               String sig,
                               int access) throws Exception {
        if (methodName.startsWith("super__")) {
            /* rationale: JC java-class, P proxy-class subclassing JC
               in order to avoid infinite recursion P should define super__foo
               only if no class between P and JC in the hierarchy defines
               it yet; this means that the python class needing P is the
               first that redefines the JC method foo.
            */
            try {
                superclass.getMethod(methodName,parameters);
                return;
            } catch(NoSuchMethodException e) {
            } catch(SecurityException e) {
                return;
            }
        }
        supernames.add(methodName);
        Code code = classfile.addMethod(methodName, sig, access);
        callSuper(code, superName, declClass, parameters, ret, sig);
    }

    public void addProxy() throws Exception {
        // implement PyProxy interface
        classfile.addField("__proxy", "Lorg/python/core/PyInstance;",
                           Modifier.PROTECTED);
        // setProxy methods
        Code code = classfile.addMethod("_setPyInstance",
                                        "(Lorg/python/core/PyInstance;)V",
                                        Modifier.PUBLIC);
        code.aload(0);
        code.aload(1);
        code.putfield(classfile.name, "__proxy", "Lorg/python/core/PyInstance;");
        code.return_();

        // getProxy method
        code = classfile.addMethod("_getPyInstance",
                                   "()Lorg/python/core/PyInstance;",
                                   Modifier.PUBLIC);
        code.aload(0);
        code.getfield(classfile.name, "__proxy", "Lorg/python/core/PyInstance;");
        code.areturn();

        // implement PyProxy interface
        classfile.addField("__systemState",
                           "Lorg/python/core/PySystemState;",
                           Modifier.PROTECTED | Modifier.TRANSIENT);

        // setProxy method
        code = classfile.addMethod("_setPySystemState",
                                   "(Lorg/python/core/PySystemState;)V",
                                   Modifier.PUBLIC);

        code.aload(0);
        code.aload(1);
        code.putfield(classfile.name, "__systemState", "Lorg/python/core/PySystemState;");
        code.return_();

        // getProxy method
        code = classfile.addMethod("_getPySystemState",
                                   "()Lorg/python/core/PySystemState;",
                                   Modifier.PUBLIC);
        code.aload(0);
        code.getfield(classfile.name, "__systemState", "Lorg/python/core/PySystemState;");
        code.areturn();
    }

    public void addClassDictInit() throws Exception {
        int n = supernames.size();

        // classDictInit method
        classfile.addInterface(mapClass(org.python.core.ClassDictInit.class));
        Code code = classfile.addMethod("classDictInit",
                                   "(" + $pyObj + ")V",
                                   Modifier.PUBLIC | Modifier.STATIC);
        code.aload(0);
        code.ldc("__supernames__");

        String[] nameArray = supernames.toArray(new String[n]);
        CodeCompiler.makeStrings(code, nameArray, n);
        code.invokestatic("org/python/core/Py", "java2py", "(" + $obj + ")" + $pyObj);
        code.invokevirtual("org/python/core/PyObject", "__setitem__", "(" + $str + $pyObj + ")V");
        code.return_();
    }

    public void build() throws Exception {
        names = new HashSet<String>();
        int access = superclass.getModifiers();
        if ((access & Modifier.FINAL) != 0) {
            throw new InstantiationException("can't subclass final class");
        }
        access = Modifier.PUBLIC | Modifier.SYNCHRONIZED;

        classfile = new ClassFile(myClass, mapClass(superclass), access);
        addProxy();
        addConstructors(superclass);
        classfile.addInterface("org/python/core/PyProxy");

        Set<String> seenmethods = new HashSet<String>();
        addMethods(superclass, seenmethods);
        for (int i=0; i<interfaces.length; i++) {
            if (interfaces[i].isAssignableFrom(superclass)) {
                Py.writeWarning("compiler",
                                "discarding redundant interface: "+
                                interfaces[i].getName());
                continue;
            }
            classfile.addInterface(mapClass(interfaces[i]));
            addMethods(interfaces[i], seenmethods);
        }
        doConstants();
        addClassDictInit();
    }

    public static File makeFilename(String name, File dir) {
        int index = name.indexOf(".");
        if (index == -1)
            return new File(dir, name+".class");

        return makeFilename(name.substring(index+1, name.length()),
                            new File(dir, name.substring(0, index)));
    }

    // This is not general enough
    public static OutputStream getFile(String d, String name)
        throws IOException
    {
        File dir = new File(d);
        File file = makeFilename(name, dir);
        file.getParentFile().mkdirs();
        return new FileOutputStream(file);
    }
}
