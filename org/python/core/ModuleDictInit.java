// Copyright 2000 Finn Bock

package org.python.core;

/**
 * An empty tagging interface. If a java class implements this 
 * interface, it must also have a method like:
 * <pre>
 *       public static void moduleDictInit(PyObject dict) { .. }
 * </pre>
 * The method will be called when the class is imported. The
 * method can then make changes to the class's __dict__ instance,
 * f.example be removing method that should not be avaiable in python
 * or by replacing some method with high performance versions.
 */

public interface ModuleDictInit
{
    // An empty tagging interface.
}
