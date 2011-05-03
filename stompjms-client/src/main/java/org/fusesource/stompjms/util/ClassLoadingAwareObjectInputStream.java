/**
 * Copyright (C) 2010-2011, FuseSource Corp.  All rights reserved.
 *
 *     http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */

package org.fusesource.stompjms.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;
import java.lang.reflect.Proxy;
import java.util.HashMap;

public class ClassLoadingAwareObjectInputStream extends ObjectInputStream {

    private static final ClassLoader FALLBACK_CLASS_LOADER = ClassLoadingAwareObjectInputStream.class.getClassLoader();
    /**
     * <p>Maps primitive type names to corresponding class objects.</p>
     */
    private static final HashMap<String, Class> primClasses = new HashMap<String, Class>(8, 1.0F);

    public ClassLoadingAwareObjectInputStream(InputStream in) throws IOException {
        super(in);
    }

    protected Class resolveClass(ObjectStreamClass classDesc) throws IOException, ClassNotFoundException {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        return load(classDesc.getName(), cl);
    }

    protected Class resolveProxyClass(String[] interfaces) throws IOException, ClassNotFoundException {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        Class[] cinterfaces = new Class[interfaces.length];
        for (int i = 0; i < interfaces.length; i++) {
            cinterfaces[i] = load(interfaces[i], cl);
        }

        try {
            return Proxy.getProxyClass(cinterfaces[0].getClassLoader(), cinterfaces);
        } catch (IllegalArgumentException e) {
            throw new ClassNotFoundException(null, e);
        }
    }

    private Class load(String className, ClassLoader cl)
            throws ClassNotFoundException {
        try {
            return Class.forName(className, false, cl);
        } catch (ClassNotFoundException e) {
            final Class clazz = (Class) primClasses.get(className);
            if (clazz != null) {
                return clazz;
            } else {
                return Class.forName(className, false, FALLBACK_CLASS_LOADER);
            }
        }
    }


    static {
        primClasses.put("boolean", boolean.class);
        primClasses.put("byte", byte.class);
        primClasses.put("char", char.class);
        primClasses.put("short", short.class);
        primClasses.put("int", int.class);
        primClasses.put("long", long.class);
        primClasses.put("float", float.class);
        primClasses.put("double", double.class);
        primClasses.put("void", void.class);
    }

}
