/* Copyright (c) Jython Developers */
package org.python.modules._weakref;

import org.python.core.PyObject;
import org.python.core.PyType;
import org.python.expose.ExposedMethod;
import org.python.expose.ExposedType;

/**
 * ProxyType with __call__.
 */
// XXX: not subclassable
@ExposedType(name = "weakcallableproxy")
public class CallableProxyType extends ProxyType {

    public static final PyType TYPE = PyType.fromClass(CallableProxyType.class);

    public CallableProxyType(GlobalRef ref, PyObject callback) {
        super(ref, callback);
    }

    public PyObject __call__(PyObject[] args, String[] kws) {
        return weakcallableproxy___call__(args, kws);
    }

    @ExposedMethod
    final PyObject weakcallableproxy___call__(PyObject[] args, String[] kws) {
        return py().__call__(args, kws);
    }
}
