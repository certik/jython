// Copyright (c) Corporation for National Research Initiatives
package org.python.modules;

import org.python.core.*;
import java.io.File;

public class py_compile {
    public static PyList __all__ = new PyList(new PyString[] {
        new PyString("compile")
    });


    public static void compile(String filename, String cfile) {
        compile(filename, cfile, null);
    }

    public static void compile(String filename) {
        compile(filename, null, null);
    }

    public static void compile(String filename, String cfile, String dfile) {
        File file = new File(filename);
        String name = file.getName();
        int dot = name.lastIndexOf('.');
        if (dot != -1) {
            name = name.substring(0, dot);
        }
        org.python.core.imp.compileSource(name, file, dfile, cfile);
    }
}
