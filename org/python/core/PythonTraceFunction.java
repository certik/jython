// Copyright � Corporation for National Research Initiatives
package org.python.core;

class PythonTraceFunction extends TraceFunction
{
    PyObject tracefunc;

    PythonTraceFunction(PyObject tracefunc) {
        this.tracefunc = tracefunc;
    }

    private synchronized TraceFunction safeCall(PyFrame frame, String label,
                                                PyObject arg)
    {
        ThreadState ts = Py.getThreadState();
        if (ts.tracing)
            return null;

        if (tracefunc == null)
            return null;

        ts.tracing = true;
        PyObject ret = tracefunc.__call__(frame, new PyString(label), arg);
        ts.tracing = false;
        if (ret == tracefunc)
            return this;
        if (ret == Py.None)
            return null;
        return new PythonTraceFunction(ret);
    }

    public TraceFunction traceCall(PyFrame frame) {
        return safeCall(frame, "call", Py.None);
    }

    public TraceFunction traceReturn(PyFrame frame, PyObject ret) {
        return safeCall(frame, "return", ret);
    }

    public TraceFunction traceLine(PyFrame frame, int line) {
        return safeCall(frame, "line", Py.None);
    }

    public TraceFunction traceException(PyFrame frame, PyException exc) {
        return safeCall(frame, "exception",
                        new PyTuple(new PyObject[] {exc.type, exc.value,
                                                    exc.traceback
                        }));
    }
}
