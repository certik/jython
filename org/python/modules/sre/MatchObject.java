
package org.python.modules.sre;

import org.python.core.*;


public class MatchObject extends PyObject {
    public String string; /* link to the target string */
    public PyObject regs; /* cached list of matching spans */
    PatternObject pattern; /* link to the regex (pattern) object */
    int pos, endpos; /* current target slice */
    int lastindex; /* last index marker seen by the engine (-1 if none) */
    int groups; /* number of groups (start/end marks) */
    int[] mark;


    public PyObject group(PyObject[] args) {
        switch (args.length) {
        case 0:
            return getslice(Py.Zero, Py.None);
        case 1:
            return getslice(args[0], Py.None);
        default:
            PyObject[] result = new PyObject[args.length];
            for (int i = 0; i < args.length; i++)
                result[i] = getslice(args[i], Py.None);
            return new PyTuple(result);
        }
    }


    public PyObject groups() {
        return groups(Py.None);
    }

    public PyObject groups(PyObject def) {
        PyObject[] result = new PyObject[groups-1];
 
        for (int i = 1; i < groups; i++) {
            result[i-1] = getslice_by_index(i, def);
        }
        return new PyTuple(result);
    }


    public PyObject groupdict() {
        return groupdict(Py.None);
    }

    public PyObject groupdict(PyObject def) {
        PyObject result = new PyDictionary();

        if (pattern.groupindex == null)
            return result;

        PyObject keys = pattern.groupindex.invoke("keys");

        PyObject key;
        for (int i = 0; (key = keys.__finditem__(i)) != null; i++) {
            PyObject item = getslice(key, def);
            result.__setitem__(key, item);
        }
        return result;
    }





    public PyObject start() {
        return start(Py.Zero);
    }


    public PyObject start(PyObject index_) {
        int index = getindex(index_);

        if (index < 0 || index >= groups)
            throw Py.IndexError("no such group");

        return Py.newInteger(mark[index*2]);
    }



    public PyObject end() {
        return end(Py.Zero);
    }


    public PyObject end(PyObject index_) {
        int index = getindex(index_);

        if (index < 0 || index >= groups)
            throw Py.IndexError("no such group");

        return Py.newInteger(mark[index*2+1]);
    }




    public PyTuple span() {
        return span(Py.Zero);
    }


    public PyTuple span(PyObject index_) {
        int index = getindex(index_);

        if (index < 0 || index >= groups) 
            throw Py.IndexError("no such group");

        int start = mark[index*2];
        int end = mark[index*2+1];

        return _pair(start, end);
    }




    public PyObject regs() {

        PyObject[] regs = new PyObject[groups];

        for (int index = 0; index < groups; index++) {
            regs[index] = _pair(mark[index*2], mark[index*2+1]);
        }

        return new PyTuple(regs);
    }






    PyTuple _pair(int i1, int i2) {
        return new PyTuple(new PyObject[] { Py.newInteger(i1), Py.newInteger(i2) });
    }
       



    private PyObject getslice(PyObject index, PyObject def) {
        return getslice_by_index(getindex(index), def);
    }



    private int getindex(PyObject index) {
        if (index instanceof PyInteger)
            return ((PyInteger) index).getValue();

        int i = -1;

        if (pattern.groupindex != null) {
            index = pattern.groupindex.__finditem__(index);
            if (index != null)
                if (index instanceof PyInteger)
                    return ((PyInteger) index).getValue();
        }
        return i;
    }



    private PyObject getslice_by_index(int index, PyObject def) {
        if (index < 0 || index >= groups) 
            throw Py.IndexError("no such group");
        
        index *= 2;
        int start = mark[index];
        int end = mark[index+1];

        //System.out.println("group:" + index + " " + start + " " + end + " l:" + string.length());

        if (string == null || start < 0)
            return def;

        return new PyString(string.substring(start, end));
    }






    public PyObject __findattr__(String key) {
        //System.out.println("__findattr__:" + key);
        if (key == "flags")
            return Py.newInteger(pattern.flags);
        if (key == "groupindex")
            return pattern.groupindex;
        if (key == "re")
            return pattern;
        return super.__findattr__(key);
    }
}