// Copyright (c) Corporation for National Research Initiatives
package org.python.core;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.util.ListIterator;

/**
 * A builtin python tuple.
 */

public class PyTuple extends PySequenceList implements ClassDictInit
{

    /** <i>Internal use only. Do not call this method explicit.</i> */
    public static void classDictInit(PyObject dict)throws PyIgnoreMethodTag {}
    //~ BEGIN GENERATED REGION -- DO NOT EDIT SEE gexpose.py
    /* type info */

    public static final String exposed_name="tuple";

    public static void typeSetup(PyObject dict,PyType.Newstyle marker) {
        class exposed___ne__ extends PyBuiltinFunctionNarrow {

            private PyTuple self;

            public PyObject getSelf() {
                return self;
            }

            exposed___ne__(PyTuple self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed___ne__((PyTuple)self,info);
            }

            public PyObject __call__(PyObject arg0) {
                PyObject ret=self.tuple___ne__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

            public PyObject inst_call(PyObject gself,PyObject arg0) {
                PyTuple self=(PyTuple)gself;
                PyObject ret=self.tuple___ne__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

        }
        dict.__setitem__("__ne__",new PyMethodDescr("__ne__",PyTuple.class,1,1,new exposed___ne__(null,null)));
        class exposed___eq__ extends PyBuiltinFunctionNarrow {

            private PyTuple self;

            public PyObject getSelf() {
                return self;
            }

            exposed___eq__(PyTuple self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed___eq__((PyTuple)self,info);
            }

            public PyObject __call__(PyObject arg0) {
                PyObject ret=self.tuple___eq__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

            public PyObject inst_call(PyObject gself,PyObject arg0) {
                PyTuple self=(PyTuple)gself;
                PyObject ret=self.tuple___eq__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

        }
        dict.__setitem__("__eq__",new PyMethodDescr("__eq__",PyTuple.class,1,1,new exposed___eq__(null,null)));
        class exposed___contains__ extends PyBuiltinFunctionNarrow {

            private PyTuple self;

            public PyObject getSelf() {
                return self;
            }

            exposed___contains__(PyTuple self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed___contains__((PyTuple)self,info);
            }

            public PyObject __call__(PyObject arg0) {
                return Py.newBoolean(self.tuple___contains__(arg0));
            }

            public PyObject inst_call(PyObject gself,PyObject arg0) {
                PyTuple self=(PyTuple)gself;
                return Py.newBoolean(self.tuple___contains__(arg0));
            }

        }
        dict.__setitem__("__contains__",new PyMethodDescr("__contains__",PyTuple.class,1,1,new exposed___contains__(null,null)));
        class exposed___len__ extends PyBuiltinFunctionNarrow {

            private PyTuple self;

            public PyObject getSelf() {
                return self;
            }

            exposed___len__(PyTuple self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed___len__((PyTuple)self,info);
            }

            public PyObject __call__() {
                return Py.newInteger(self.tuple___len__());
            }

            public PyObject inst_call(PyObject gself) {
                PyTuple self=(PyTuple)gself;
                return Py.newInteger(self.tuple___len__());
            }

        }
        dict.__setitem__("__len__",new PyMethodDescr("__len__",PyTuple.class,0,0,new exposed___len__(null,null)));
        class exposed___add__ extends PyBuiltinFunctionNarrow {

            private PyTuple self;

            public PyObject getSelf() {
                return self;
            }

            exposed___add__(PyTuple self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed___add__((PyTuple)self,info);
            }

            public PyObject __call__(PyObject arg0) {
                return self.tuple___add__(arg0);
            }

            public PyObject inst_call(PyObject gself,PyObject arg0) {
                PyTuple self=(PyTuple)gself;
                return self.tuple___add__(arg0);
            }

        }
        dict.__setitem__("__add__",new PyMethodDescr("__add__",PyTuple.class,1,1,new exposed___add__(null,null)));
        class exposed___reduce__ extends PyBuiltinFunctionNarrow {

            private PyTuple self;

            public PyObject getSelf() {
                return self;
            }

            exposed___reduce__(PyTuple self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed___reduce__((PyTuple)self,info);
            }

            public PyObject __call__() {
                return self.tuple___reduce__();
            }

            public PyObject inst_call(PyObject gself) {
                PyTuple self=(PyTuple)gself;
                return self.tuple___reduce__();
            }

        }
        dict.__setitem__("__reduce__",new PyMethodDescr("__reduce__",PyTuple.class,0,0,new exposed___reduce__(null,null)));
        class exposed___mul__ extends PyBuiltinFunctionNarrow {

            private PyTuple self;

            public PyObject getSelf() {
                return self;
            }

            exposed___mul__(PyTuple self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed___mul__((PyTuple)self,info);
            }

            public PyObject __call__(PyObject arg0) {
                return self.tuple___mul__(arg0);
            }

            public PyObject inst_call(PyObject gself,PyObject arg0) {
                PyTuple self=(PyTuple)gself;
                return self.tuple___mul__(arg0);
            }

        }
        dict.__setitem__("__mul__",new PyMethodDescr("__mul__",PyTuple.class,1,1,new exposed___mul__(null,null)));
        class exposed___rmul__ extends PyBuiltinFunctionNarrow {

            private PyTuple self;

            public PyObject getSelf() {
                return self;
            }

            exposed___rmul__(PyTuple self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed___rmul__((PyTuple)self,info);
            }

            public PyObject __call__(PyObject arg0) {
                return self.tuple___rmul__(arg0);
            }

            public PyObject inst_call(PyObject gself,PyObject arg0) {
                PyTuple self=(PyTuple)gself;
                return self.tuple___rmul__(arg0);
            }

        }
        dict.__setitem__("__rmul__",new PyMethodDescr("__rmul__",PyTuple.class,1,1,new exposed___rmul__(null,null)));
        class exposed___getslice__ extends PyBuiltinFunctionNarrow {

            private PyTuple self;

            public PyObject getSelf() {
                return self;
            }

            exposed___getslice__(PyTuple self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed___getslice__((PyTuple)self,info);
            }

            public PyObject __call__(PyObject arg0,PyObject arg1,PyObject arg2) {
                return self.tuple___getslice__(arg0,arg1,arg2);
            }

            public PyObject inst_call(PyObject gself,PyObject arg0,PyObject arg1,PyObject arg2) {
                PyTuple self=(PyTuple)gself;
                return self.tuple___getslice__(arg0,arg1,arg2);
            }

            public PyObject __call__(PyObject arg0,PyObject arg1) {
                return self.tuple___getslice__(arg0,arg1);
            }

            public PyObject inst_call(PyObject gself,PyObject arg0,PyObject arg1) {
                PyTuple self=(PyTuple)gself;
                return self.tuple___getslice__(arg0,arg1);
            }

        }
        dict.__setitem__("__getslice__",new PyMethodDescr("__getslice__",PyTuple.class,2,3,new exposed___getslice__(null,null)));
        class exposed___getitem__ extends PyBuiltinFunctionNarrow {

            private PyTuple self;

            public PyObject getSelf() {
                return self;
            }

            exposed___getitem__(PyTuple self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed___getitem__((PyTuple)self,info);
            }

            public PyObject __call__(PyObject arg0) {
                return self.tuple___getitem__(arg0);
            }

            public PyObject inst_call(PyObject gself,PyObject arg0) {
                PyTuple self=(PyTuple)gself;
                return self.tuple___getitem__(arg0);
            }

        }
        dict.__setitem__("__getitem__",new PyMethodDescr("__getitem__",PyTuple.class,1,1,new exposed___getitem__(null,null)));
        class exposed___hash__ extends PyBuiltinFunctionNarrow {

            private PyTuple self;

            public PyObject getSelf() {
                return self;
            }

            exposed___hash__(PyTuple self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed___hash__((PyTuple)self,info);
            }

            public PyObject __call__() {
                return Py.newInteger(self.tuple_hashCode());
            }

            public PyObject inst_call(PyObject gself) {
                PyTuple self=(PyTuple)gself;
                return Py.newInteger(self.tuple_hashCode());
            }

        }
        dict.__setitem__("__hash__",new PyMethodDescr("__hash__",PyTuple.class,0,0,new exposed___hash__(null,null)));
        class exposed___repr__ extends PyBuiltinFunctionNarrow {

            private PyTuple self;

            public PyObject getSelf() {
                return self;
            }

            exposed___repr__(PyTuple self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed___repr__((PyTuple)self,info);
            }

            public PyObject __call__() {
                return new PyString(self.tuple_toString());
            }

            public PyObject inst_call(PyObject gself) {
                PyTuple self=(PyTuple)gself;
                return new PyString(self.tuple_toString());
            }

        }
        dict.__setitem__("__repr__",new PyMethodDescr("__repr__",PyTuple.class,0,0,new exposed___repr__(null,null)));
        dict.__setitem__("__new__",new PyNewWrapper(PyTuple.class,"__new__",-1,-1) {

                                                                                       public PyObject new_impl(boolean init,PyType subtype,PyObject[]args,String[]keywords) {
                                                                                           return tuple_new(this,init,subtype,args,keywords);
                                                                                       }

                                                                                   });
    }
    //~ END GENERATED REGION -- DO NOT EDIT SEE gexpose.py

    private static final PyType TUPLETYPE = PyType.fromClass(PyTuple.class);

    public PyTuple() {
        this(TUPLETYPE, Py.EmptyObjects);
    }

    public PyTuple(PyObject[] elements) {
        this(TUPLETYPE, elements);
    }

    public PyTuple(PyType subtype, PyObject[] elements) {
        super(subtype, elements);
    }

    final static PyObject tuple_new(PyNewWrapper new_, boolean init, PyType subtype,
            PyObject[] args, String[] keywords) {
        ArgParser ap = new ArgParser("tuple", args, keywords, new String[] { "S" }, 0);
        PyObject S = ap.getPyObject(0, null);
        if (new_.for_type == subtype) {
            if (S == null) {
                return new PyTuple();
            }
            if (S instanceof PyTupleDerived) {
                return new PyTuple(((PyTuple)S).getArray());
            }
            if (S instanceof PyTuple) {
                return S;
            }
            PyObject iter = S.__iter__();
            // it's not always possible to know the length of the iterable
            ArrayList a = new ArrayList(10);
            for (PyObject item = null; (item = iter.__iternext__()) != null; ) {
                a.add(item);
            }
            return new PyTuple((PyObject[])a.toArray(new PyObject[a.size()]));
        } else {
            if (S == null) {
                return new PyTupleDerived(subtype, Py.EmptyObjects);
            }
            PyObject iter = S.__iter__();
            // it's not always possible to know the length of the iterable
            ArrayList a = new ArrayList(10);
            for (PyObject item = null; (item = iter.__iternext__()) != null; ) {
                a.add(item);
            }
            return new PyTupleDerived(subtype, (PyObject[])a.toArray(new PyObject[a.size()]));
        }
    }

    public String safeRepr() throws PyIgnoreMethodTag {
        return "'tuple' object";
    }

    protected PyObject getslice(int start, int stop, int step) {
        if (step > 0 && stop < start)
            stop = start;
        int n = sliceLength(start, stop, step);
        PyObject[] newArray = new PyObject[n];
        PyObject[] array = getArray();

        if (step == 1) {
            System.arraycopy(array, start, newArray, 0, stop-start);
            return new PyTuple(newArray);
        }
        int j = 0;
        for (int i=start; j<n; i+=step) {
            newArray[j] = array[i];
            j++;
        }
        return new PyTuple(newArray);
    }

    protected PyObject repeat(int count) {

        PyObject[] array = getArray();
        int l = size();
        PyObject[] newArray = new PyObject[l*count];
        for (int i=0; i<count; i++) {
            System.arraycopy(array, 0, newArray, i*l, l);
        }
        return new PyTuple(newArray);
    }

    public int __len__() {
        return tuple___len__();
    }

    final int tuple___len__() {
        return size();
    }

    final boolean tuple___contains__(PyObject o) {
        return super.__contains__(o);
    }

    final PyObject tuple___ne__(PyObject o) {
        return super.__ne__(o);
    }

    final PyObject tuple___eq__(PyObject o) {
        return super.__eq__(o);
    }

    public PyObject __add__(PyObject generic_other) {
        return tuple___add__(generic_other);
    }

    final PyObject tuple___add__(PyObject generic_other) {
        PyTuple sum = null;
        if (generic_other instanceof PyTuple) {
            PyTuple otherTuple = (PyTuple)generic_other;
            PyObject[] array = getArray();
            PyObject[] otherArray = otherTuple.getArray();
            int thisLen = size();
            int otherLen = otherTuple.size();
            PyObject[] newArray = new PyObject[thisLen + otherLen];
            System.arraycopy(array, 0, newArray, 0, thisLen);
            System.arraycopy(otherArray, 0, newArray, thisLen, otherLen);
            sum = new PyTuple(newArray);
        }
        return sum;
    }

    final PyObject tuple___mul__(PyObject o) {
        if (!(o instanceof PyInteger || o instanceof PyLong))
            throw Py.TypeError("can't multiply sequence to non-int");
        int count = ((PyInteger)o.__int__()).getValue();
        return repeat(count);
    }

    final PyObject tuple___rmul__(PyObject o) {
        if (!(o instanceof PyInteger || o instanceof PyLong))
            throw Py.TypeError("can't multiply sequence to non-int");
        int count = ((PyInteger)o.__int__()).getValue();
        return repeat(count);
    }

    final PyObject tuple___getslice__(PyObject s_start, PyObject s_stop) {
        return seq___getslice__(s_start,s_stop,null);
    }

    final PyObject tuple___getslice__(PyObject s_start, PyObject s_stop, PyObject s_step) {
        return seq___getslice__(s_start,s_stop,s_step);
    }

    final PyObject tuple___getitem__(PyObject index) {
        return seq___getitem__(index);
    }


    /**
     * Used for pickling.
     *
     * @return a tuple of (class, tuple)
     */
    public PyObject __reduce__() {
        return tuple___reduce__();
    }

    final PyObject tuple___reduce__() {
        PyTuple newargs = __getnewargs__();
        return new PyTuple(new PyObject[]{
            getType(), newargs
        });
    }

    public PyTuple __getnewargs__() {
        return new PyTuple(new PyObject[]
            {new PyList(list.getArray())}
        );
    }

    public int hashCode() {
        return tuple_hashCode();
    }

    final int tuple_hashCode() {
        return super.hashCode();
    }

    private String subobjRepr(PyObject o) {
        if (o == null)
            return "null";
        return o.__repr__().toString();
    }

    public String toString() {
        return tuple_toString();
    }

    final String tuple_toString() {
        StringBuffer buf = new StringBuffer("(");
        PyObject[] array = getArray();
        int arrayLen = size();
        for (int i = 0; i < arrayLen-1; i++) {
            buf.append(subobjRepr(array[i]));
            buf.append(", ");
        }
        if (arrayLen > 0)
            buf.append(subobjRepr(array[arrayLen-1]));
        if (arrayLen == 1)
            buf.append(",");
        buf.append(")");
        return buf.toString();
    }

    public List subList(int fromIndex, int toIndex) {
        return Collections.unmodifiableList(list.subList(fromIndex, toIndex));
    }

    // Make PyTuple immutable from the collections interfaces by overriding
    // all the mutating methods to throw UnsupportedOperationException exception.
    // This is how Collections.unmodifiableList() does it.
    public Iterator iterator() {
        return new Iterator() {
            Iterator i = list.iterator();
            public void remove() {
                throw new UnsupportedOperationException();
            }
            public boolean hasNext() {
                return i.hasNext();
                }
            public Object next() {
                return i.next();
                }
        };
    }

    public boolean add(Object o){
        throw new UnsupportedOperationException();
    }

    public boolean remove(Object o) {
        throw new UnsupportedOperationException();
    }

    public boolean addAll(Collection coll) {
        throw new UnsupportedOperationException();
    }

    public boolean removeAll(Collection coll) {
        throw new UnsupportedOperationException();
    }

    public boolean retainAll(Collection coll) {
        throw new UnsupportedOperationException();
    }

    public void clear() {
        throw new UnsupportedOperationException();
    }

    public Object set(int index, Object element) {
        throw new UnsupportedOperationException();
    }

    public void add(int index, Object element) {
        throw new UnsupportedOperationException();
    }

    public Object remove(int index) {
        throw new UnsupportedOperationException();
    }

    public boolean addAll(int index, Collection c) {
        throw new UnsupportedOperationException();
    }

    public ListIterator listIterator() {
        return listIterator(0);
    }

    public ListIterator listIterator(final int index) {
        return new ListIterator() {
            ListIterator i = list.listIterator(index);

            public boolean hasNext()     {return i.hasNext();}
            public Object next()         {return i.next();}
            public boolean hasPrevious() {return i.hasPrevious();}
            public Object previous()     {return i.previous();}
            public int nextIndex()       {return i.nextIndex();}
            public int previousIndex()   {return i.previousIndex();}

            public void remove() {
                throw new UnsupportedOperationException();
            }

            public void set(Object o) {
                throw new UnsupportedOperationException();
            }

            public void add(Object o) {
                throw new UnsupportedOperationException();
            }
        };
    }
}
