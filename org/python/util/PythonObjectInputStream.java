
package org.python.util;


import java.io.*;
import org.python.core.*;

public class PythonObjectInputStream extends ObjectInputStream {
    public PythonObjectInputStream(InputStream istr) throws IOException {
        super(istr);
    }

    protected Class resolveClass(ObjectStreamClass v)
                      throws IOException, ClassNotFoundException {
        String clsName = v.getName();
        //System.out.println(clsName);
        if (clsName.startsWith("org.python.proxies")) {
            int idx = clsName.lastIndexOf('$');
            if (idx > 19)
               clsName = clsName.substring(19, idx);
            //System.out.println("new:" + clsName);
            Class cls = (Class) PyClass.serializableProxies.get(clsName);
            if (cls != null)
                return cls;
        }
        return super.resolveClass(v);
    }
}


        