// Copyright (c) Corporation for National Research Initiatives
package org.python.util;

import org.python.core.*;
import org.gnu.readline.*;

// Based on CPython-1.5.2's code module

public class ReadlineConsole extends InteractiveConsole {
    public String filename;

    public ReadlineConsole() {
        this(null, "<console>");
    }
    public ReadlineConsole(PyObject locals) {
        this(locals, "<console>");
    }
    public ReadlineConsole(PyObject locals, String filename) {
        super(locals,filename);
        String backingLib = PySystemState.registry.getProperty(
                                 "python.console.readlinelib", "Editline");
        try {
            Readline.load(ReadlineLibrary.byName(backingLib));
        } catch (RuntimeException e) {
            // Silently ignore errors during load of the native library.
            // Will use a pure java fallback.
        }
        Readline.initReadline("jpython");
    }


    /**
     * Write a prompt and read a line.
     *
     * The returned line does not include the trailing newline.  When the
     * user enters the EOF key sequence, EOFError is raised.
     *
     * This subclass implements the functionality using JavaReadline.
     **/
    public String raw_input(PyObject prompt) {
        try {
            String line = Readline.readline(
                            prompt==null ? "" : prompt.toString());
            return (line == null ? "" : line);
        } catch (java.io.EOFException eofe) {
           throw new PyException(Py.EOFError);
        } catch (java.io.IOException e) {
           throw new PyException(Py.IOError);
        }
    }
}
