// Copyright � Corporation for National Research Initiatives
package org.python.core;

public abstract class PyFunctionTable {
    abstract public PyObject call_function(int index, PyFrame frame);
}
