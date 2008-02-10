package org.python.core;

import org.python.expose.ExposedMethod;
import org.python.expose.ExposedNew;
import org.python.expose.ExposedType;

@ExposedType(name = "staticmethod")
public class PyStaticMethod extends PyObject {

    public static final PyType TYPE = PyType.fromClass(PyStaticMethod.class);

    @ExposedNew
    final static PyObject staticmethod_new(PyNewWrapper new_,
                                           boolean init,
                                           PyType subtype,
                                           PyObject[] args,
                                           String[] keywords) {
        if (keywords.length != 0) {
            throw Py.TypeError("staticmethod does not accept keyword arguments");
        }
        if (args.length != 1) {
            throw Py.TypeError("staticmethod expected 1 argument, got " + args.length);
        }
        return new PyStaticMethod(args[0]);
    }

    @ExposedMethod(defaults = "null")
    final PyObject staticmethod___get__(PyObject obj, PyObject type) {
        return callable;
    }

    protected PyObject callable;

    public PyStaticMethod(PyObject callable) {
        this.callable = callable;
    }

    public PyObject __get__(PyObject obj, PyObject type) {
        return staticmethod___get__(obj, type);
    }
}
