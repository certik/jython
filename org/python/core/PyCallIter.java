package org.python.core;

public class PyCallIter extends PyIterator {
    private PyObject callable;
    private PyObject sentinel;
    private int idx;

    public PyCallIter(PyObject callable, PyObject sentinel) {
        this.callable = callable;
        this.sentinel = sentinel;
    }

    public PyObject __iternext__() {
        PyObject val = null;
        try {
            val = callable.__call__();
        } catch (PyException exc) {
            if (Py.matchException(exc, Py.StopIteration))
                return null;
            throw exc;
        }
        if (val._eq(sentinel).__nonzero__())
            return null;
        return val;
    }

    // __class__ boilerplate -- see PyObject for details
    public static PyClass __class__;
    protected PyClass getPyClass() { return __class__; }
}

