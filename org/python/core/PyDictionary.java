// Copyright � Corporation for National Research Initiatives
package org.python.core;

import java.util.Hashtable;



class DictFuncs extends PyBuiltinFunctionSet
{
    DictFuncs(String name, int index, int argcount) {
        super(name, index, argcount, argcount, true, null);
    }

    DictFuncs(String name, int index, int minargs, int maxargs) {
        super(name, index, minargs, maxargs, true, null);
    }

    public PyObject __call__() {
        PyDictionary dict = (PyDictionary)__self__;
        switch (index) {
        case 1:
            return new PyInteger(dict.__len__());
        case 2:
            return new PyInteger(dict.__nonzero__() ? 1 : 0);
        case 3:
            return dict.copy();
        case 4:
            dict.clear();
            return Py.None;
        case 5:
            return dict.items();
        case 6:
            return dict.keys();
        case 7:
            return dict.values();
        case 8:
            return dict.popitem();
        default:
            throw argCountError(0);
        }
    }

    public PyObject __call__(PyObject arg) {
        PyDictionary dict = (PyDictionary)__self__;
        switch (index) {
        case 11:
            return new PyInteger(dict.__cmp__(arg));
        case 12:
            return new PyInteger(dict.has_key(arg) ? 1 : 0);
        case 13:
            return dict.get(arg);
        case 14:
            if (arg instanceof PyDictionary) {
                dict.update((PyDictionary)arg);
                return Py.None;
            }
            else
                throw Py.TypeError("dictionary expected, got " +
                                   arg.safeRepr());
        default:
            throw argCountError(1);
        }
    }

    public PyObject __call__(PyObject arg1, PyObject arg2) {
        PyDictionary dict = (PyDictionary)__self__;
        switch (index) {
        case 13:
            return dict.get(arg1, arg2);
        default:
            throw argCountError(2);
        }
    }
}




public class PyDictionary extends PyObject implements ClassDictInit
{

    protected Hashtable table;
    protected static PyObject __methods__;

    static {
        PyList list = new PyList();
        String[] methods = {
            "clear", "copy", "get", "has_key", "items",
            "keys", "update", "values", "setdefault" };
        for (int i = 0; i < methods.length; i++)
            list.append(new PyString(methods[i]));
        __methods__ = list;
    }

    // Used by PyJavaClass.init()
    public PyDictionary() {
        this(new Hashtable());
    }

    public PyDictionary(Hashtable t) {
        table = t;
    }

    public PyDictionary(PyObject elements[]) {
        this();
        for (int i = 0; i < elements.length; i+=2) {
            table.put(elements[i], elements[i+1]);
        }
    }

    public static void classDictInit(PyObject dict) {
        dict.__setitem__("__len__", new DictFuncs("__len__", 1, 0));
        dict.__setitem__("__nonzero__", new DictFuncs("__nonzero__", 2, 0));
        dict.__setitem__("copy", new DictFuncs("copy", 3, 0));
        dict.__setitem__("clear", new DictFuncs("clear", 4, 0));
        dict.__setitem__("items", new DictFuncs("items", 5, 0));
        dict.__setitem__("keys", new DictFuncs("keys", 6, 0));
        dict.__setitem__("values", new DictFuncs("values", 7, 0));
        dict.__setitem__("popitem", new DictFuncs("popitem", 8, 0));
        dict.__setitem__("__cmp__", new DictFuncs("__cmp__", 11, 1));
        dict.__setitem__("has_key", new DictFuncs("has_key", 12, 1));
        dict.__setitem__("get", new DictFuncs("get", 13, 1, 2));
        dict.__setitem__("update", new DictFuncs("update", 14, 1));
        // Hide these from Python
        dict.__setitem__("__finditem__", null);
        dict.__setitem__("__setitem__", null);
        dict.__setitem__("__delitem__", null);
        dict.__setitem__("toString", null);
        dict.__setitem__("hashCode", null);
        dict.__setitem__("classDictInit", null);
    }

    protected String safeRepr() {
        return "'dict' object";
    }

    public PyObject __findattr__(String name) {
        if (name.equals("__methods__")) {
            PyList mlist = (PyList)__methods__;
            PyString methods[] = new PyString[mlist.length];
            for (int i = 0; i < mlist.length; i++)
                methods[i] = (PyString)mlist.list[i];
            return new PyList(methods);
        }
        return super.__findattr__(name);
    }

    public int __len__() {
        return table.size();
    }

    public boolean __nonzero__() {
        return table.size() != 0;
    }

    public PyObject __finditem__(int index) {
        throw Py.TypeError("loop over non-sequence");
    }

    public PyObject __finditem__(PyObject key) {
        return (PyObject)table.get(key);
    }

    public void __setitem__(PyObject key, PyObject value)  {
        table.put(key, value);
    }

    public void __delitem__(PyObject key) {
        Object ret = table.remove(key);
        if (ret == null)
            throw Py.KeyError(key.toString());
    }

    public String toString() {
        ThreadState ts = Py.getThreadState();
        if (!ts.enterRepr(this)) {
            return "{...}";
        }

        java.util.Enumeration ek = table.keys();
        java.util.Enumeration ev = table.elements();
        int n = table.size();
        StringBuffer buf = new StringBuffer("{");

        for(int i=0; i<n; i++) {
            buf.append(((PyObject)ek.nextElement()).__repr__().toString());
            buf.append(": ");
            buf.append(((PyObject)ev.nextElement()).__repr__().toString());
            if (i < n-1)
                buf.append(", ");
        }
        buf.append("}");

        ts.exitRepr(this);
        return buf.toString();
    }

    public int __cmp__(PyObject ob_other) {
        if (ob_other.__class__ != __class__)
            return -2;

        PyDictionary other = (PyDictionary)ob_other;
        int an = table.size();
        int bn = other.table.size();
        if (an < bn) return -1;
        if (an > bn) return 1;

        PyList akeys = keys();
        PyList bkeys = other.keys();

        akeys.sort();
        bkeys.sort();

        for (int i=0; i<bn; i++) {
            PyObject akey = akeys.get(i);
            PyObject bkey = bkeys.get(i);
            int c = akey._cmp(bkey);
            if (c != 0)
                return c;

            PyObject avalue = __finditem__(akey);
            PyObject bvalue = other.__finditem__(bkey);
            c = avalue._cmp(bvalue);
            if (c != 0)
                return c;
        }
        return 0;
    }

    public boolean has_key(PyObject key) {
        return table.containsKey(key);
    }

    public PyObject get(PyObject key, PyObject default_object) {
        PyObject o = __finditem__(key);
        if (o == null)
            return default_object;
        else
            return o;
    }

    public PyObject get(PyObject key) {
        return get(key, Py.None);
    }

    public PyDictionary copy() {
        return new PyDictionary((Hashtable)table.clone());
    }

    public void clear() {
        table.clear();
    }

    public void update(PyDictionary d) {
        Hashtable otable = d.table;

        java.util.Enumeration ek = otable.keys();
        java.util.Enumeration ev = otable.elements();
        int n = otable.size();

        for (int i=0; i<n; i++)
            table.put(ek.nextElement(), ev.nextElement());
    }

    public void update(PyStringMap d) {
        PyObject keys = d.keys();
        PyObject key;
        for (int i = 0; (key = keys.__finditem__(i)) != null; i++)
            __setitem__(key, d.__getitem__(key));
    }

    public PyObject setdefault(PyObject key) {
        return setdefault(key, Py.None);
    }

    public PyObject setdefault(PyObject key, PyObject failobj) {
        PyObject o = __finditem__(key);
        if (o == null)
            __setitem__(key, o = failobj);
        return o;
    }

    public PyObject popitem() {
        java.util.Enumeration keys = table.keys();
        if (!keys.hasMoreElements())
            throw Py.KeyError("popitem(): dictionary is empty");
        PyObject key = (PyObject) keys.nextElement();
        PyObject val = (PyObject) table.get(key);
        table.remove(key);
        return new PyTuple(new PyObject[] { key, val });
    }

    public PyList items() {
        java.util.Enumeration ek = table.keys();
        java.util.Enumeration ev = table.elements();
        int n = table.size();
        java.util.Vector l = new java.util.Vector(n);

        for (int i=0; i<n; i++)
            l.addElement(new PyTuple(new PyObject[] {
                (PyObject)ek.nextElement(), (PyObject)ev.nextElement()
            }));
        return new PyList(l);
    }

    public PyList keys() {
        java.util.Enumeration e = table.keys();
        int n = table.size();
        java.util.Vector l = new java.util.Vector(n);

        for (int i=0; i<n; i++)
            l.addElement(e.nextElement());
        return new PyList(l);
    }

    public PyList values() {
        java.util.Enumeration e = table.elements();
        int n = table.size();
        java.util.Vector l = new java.util.Vector(n);

        for (int i=0; i<n; i++)
            l.addElement(e.nextElement());
        return new PyList(l);
    }

    public int hashCode() {
        throw Py.TypeError("unhashable type");
    }
}
