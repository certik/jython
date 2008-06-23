// Copyright (c) Corporation for National Research Initiatives
package org.python.core;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.python.expose.ExposedMethod;
import org.python.expose.ExposedNew;
import org.python.expose.ExposedType;
import org.python.expose.MethodType;

/**
 * A builtin python list.
 */
@ExposedType(name = "list", base = PyObject.class)
public class PyList extends PySequenceList {

    public static final PyType TYPE = PyType.fromClass(PyList.class);

    public PyList() {
        this(TYPE, Py.EmptyObjects);
    }

    public PyList(PyType type) {
        super(type);
    }

    public PyList(PyType type, PyObject[] elements) {
        super(type, elements);
    }

    public PyList(PyType type, Collection c) {
        super(type, c);
    }

    public PyList(PyObject[] elements) {
        this(TYPE, elements);
    }

    public PyList(Collection c) {
        super(TYPE, c);
    }

    public PyList(PyObject o) {
        this(TYPE);
        for (PyObject item : o.asIterable()) {
            append(item);
        }
    }
    
    private static List<PyObject> listify(Iterator<PyObject> iter) {
         final List<PyObject> list = new LinkedList<PyObject>();  
         while (iter.hasNext()) {
            list.add(iter.next());
         }
         return list;
    }
    public PyList(Iterator<PyObject> iter) {
        this(TYPE, listify(iter));
    }

    @ExposedNew
    @ExposedMethod
    final void list___init__(PyObject[] args, String[] kwds) {
        ArgParser ap = new ArgParser("list", args, kwds, new String[] {"sequence"}, 0);
        PyObject seq = ap.getPyObject(0, null);
        clear();
        if(seq == null) {
            return;
        }
        if(seq instanceof PySequenceList) {
            PySequenceList p = (PySequenceList)seq.__getslice__(Py.None, Py.None, Py.One);
            this.list = p.list;
        } else {
            for (PyObject item : seq.asIterable()) {
                append(item);
            }
        }
    }

    public int __len__() {
        return list___len__();
    }

    @ExposedMethod
    final int list___len__() {
        return size();
    }

    protected PyObject getslice(int start, int stop, int step) {
        if(step > 0 && stop < start) {
            stop = start;
        }
        int n = sliceLength(start, stop, step);
        PyObject[] newList = new PyObject[n];
        PyObject[] array = getArray();
        if(step == 1) {
            System.arraycopy(array, start, newList, 0, stop - start);
            return new PyList(newList);
        }
        int j = 0;
        for(int i = start; j < n; i += step) {
            newList[j] = array[i];
            j++;
        }
        return new PyList(newList);
    }

    protected void del(int i) {
        remove(i);
    }

    protected void delRange(int start, int stop, int step) {
        if(step == 1) {
            remove(start, stop);
        } else if(step > 1) {
            for(int i = start; i < stop; i += step) {
                remove(i);
                i--;
                stop--;
            }
        } else if(step < 0) {
            for(int i = start; i >= 0 && i >= stop; i += step) {
                remove(i);
            }
        }
    }

    protected void set(int i, PyObject value) {
        list.pyset(i, value);
    }

    protected void setslice(int start, int stop, int step, PyObject value) {
        if(stop < start) {
            stop = start;
        }
        if(value instanceof PySequence) {
            PySequence sequence = (PySequence) value;
            setslicePySequence(start, stop, step, sequence);
        } else if(value instanceof List) {
            List list = (List)value.__tojava__(List.class);
            if(list != null && list != Py.NoConversion) {
                setsliceList(start, stop, step, list);
            }
        } else {
            setsliceIterable(start, stop, step, value);
        }
    }

    protected void setslicePySequence(int start, int stop, int step, PySequence value) {
        if(step == 1) {
            PyObject[] otherArray;
            PyObject[] array = getArray();
            if(value instanceof PySequenceList) {
                PySequenceList seqList = (PySequenceList) value;
                otherArray = seqList.getArray();
                if(otherArray == array) {
                    otherArray = otherArray.clone();
                }
                list.replaceSubArray(start, stop, otherArray, 0, seqList.size());
            } else {
                int n = value.__len__();
                list.ensureCapacity(start + n);
                for(int i = 0; i < n; i++) {
                    list.add(i + start, value.pyget(i));
                }
            }
        } else if(step > 1) {
            int n = value.__len__();
            for(int i = 0, j = 0; i < n; i++, j += step) {
                list.pyset(j + start, value.pyget(i));
            }
        } else if(step < 0) {
            int n = value.__len__();
            if(value == this) {
                PyList newseq = new PyList();
                PyObject iter = value.__iter__();
                for(PyObject item = null; (item = iter.__iternext__()) != null;) {
                    newseq.append(item);
                }
                value = newseq;
            }
            for(int i = 0, j = list.size() - 1; i < n; i++, j += step) {
                list.pyset(j, value.pyget(i));
            }
        }
    }

    protected void setsliceList(int start, int stop, int step, List value) {
        if(step != 1) {
            throw Py.TypeError("setslice with java.util.List and step != 1 not " + "supported yet");
        }
        int n = value.size();
        list.ensureCapacity(start + n);
        for(int i = 0; i < n; i++) {
            list.add(i + start, value.get(i));
        }
    }

    protected void setsliceIterable(int start, int stop, int step, PyObject value) {
        PyObject iter;
        try {
            iter = value.__iter__();
        } catch(PyException pye) {
            if(Py.matchException(pye, Py.TypeError)) {
                throw Py.TypeError("can only assign an iterable");
            }
            throw pye;
        }
        PyObject next;
        for(int j = 0; (next = iter.__iternext__()) != null; j += step) {
            if(step < 0) {
                list.pyset(start + j, next);
            } else {
                list.add(start + j, next);
            }
        }
    }

    protected PyObject repeat(int count) {
        if(count < 0) {
            count = 0;
        }
        int l = size();
        PyObject[] newList = new PyObject[l * count];
        for(int i = 0; i < count; i++) {
            System.arraycopy(getArray(), 0, newList, i * l, l);
        }
        return new PyList(newList);
    }

    @ExposedMethod(type = MethodType.BINARY)
    final PyObject list___ne__(PyObject o) {
        return seq___ne__(o);
    }

    @ExposedMethod(type = MethodType.BINARY)
    final PyObject list___eq__(PyObject o) {
        return seq___eq__(o);
    }

    @ExposedMethod(type = MethodType.BINARY)
    final PyObject list___lt__(PyObject o) {
        return seq___lt__(o);
    }

    @ExposedMethod(type = MethodType.BINARY)
    final PyObject list___le__(PyObject o) {
        return seq___le__(o);
    }

    @ExposedMethod(type = MethodType.BINARY)
    final PyObject list___gt__(PyObject o) {
        return seq___gt__(o);
    }

    @ExposedMethod(type = MethodType.BINARY)
    final PyObject list___ge__(PyObject o) {
        return seq___ge__(o);
    }

    public PyObject __imul__(PyObject o) {
        PyObject result = list___imul__(o);
        if(result == null) {
            // We can't perform an in-place multiplication on o's
            // type, so let o try to rmul this list. A new list will
            // be created instead of modifying this one, but that's
            // preferable to just blowing up on this operation.
            result = o.__rmul__(this);
            if(result == null) {
                throw Py.TypeError(_unsupportedop("*", o));
            }
        }
        return result;
    }

    @ExposedMethod
    final PyObject list___imul__(PyObject o) {
        if(!(o instanceof PyInteger || o instanceof PyLong)) {
            return null;
        }
        int l = size();
        int count = ((PyInteger)o.__int__()).getValue();
        int newSize = l * count;
        list.setSize(newSize);
        PyObject[] array = getArray();
        for (int i = 1; i < count; i++) {
            System.arraycopy(array, 0, array, i * l, l);
        }
        gListAllocatedStatus = __len__();
        return this;
    }

    @ExposedMethod(type = MethodType.BINARY)
    final PyObject list___mul__(PyObject o) {
        if(!(o instanceof PyInteger || o instanceof PyLong)) {
            return null;
        }
        int count = ((PyInteger)o.__int__()).getValue();
        return repeat(count);
    }

    @ExposedMethod(type = MethodType.BINARY)
    final PyObject list___rmul__(PyObject o) {
        if(!(o instanceof PyInteger || o instanceof PyLong)) {
            return null;
        }
        int count = ((PyInteger)o.__int__()).getValue();
        return repeat(count);
    }

    public PyObject __add__(PyObject o) {
        return list___add__(o);
    }

    @ExposedMethod(type = MethodType.BINARY)
    final PyObject list___add__(PyObject o) {
        PyList sum = null;
        if(o instanceof PyList) {
            PyList other = (PyList)o;
            int thisLen = size();
            int otherLen = other.size();
            PyObject[] newList = new PyObject[thisLen + otherLen];
            System.arraycopy(getArray(), 0, newList, 0, thisLen);
            System.arraycopy(other.getArray(), 0, newList, thisLen, otherLen);
            sum = new PyList(newList);
        } else if(!(o instanceof PySequenceList)) {
            // also support adding java lists (but not PyTuple!)
            Object oList = o.__tojava__(List.class);
            if(oList != Py.NoConversion && oList != null) {
                List otherList = (List) oList;
                sum = new PyList();
                sum.list_extend(this);
                for(Iterator i = otherList.iterator(); i.hasNext();) {
                    sum.add(i.next());
                }
            }
        }
        return sum;
    }

    public PyObject __radd__(PyObject o) {
        return list___radd__(o);
    }

    @ExposedMethod(type = MethodType.BINARY)
    final PyObject list___radd__(PyObject o) {
        // Support adding java.util.List, but prevent adding PyTuple.
        // 'o' should never be a PyList since __add__ is defined.
        PyList sum = null;
        if(o instanceof PySequence) {
            return null;
        }
        Object oList = o.__tojava__(List.class);
        if (oList != Py.NoConversion && oList != null) {
            sum = new PyList();
            sum.addAll((List) oList);
            sum.extend(this);
        }
        return sum;
    }

    @ExposedMethod
    final boolean list___contains__(PyObject o) {
        return object___contains__(o);
    }

    @ExposedMethod
    final void list___delitem__(PyObject index) {
        seq___delitem__(index);
    }

    @ExposedMethod
    final void list___setitem__(PyObject o, PyObject def) {
        seq___setitem__(o, def);
    }

    @ExposedMethod
    final PyObject list___getitem__(PyObject o) {
        PyObject ret = seq___finditem__(o);
        if(ret == null) {
            throw Py.IndexError("index out of range: " + o);
        }
        return ret;
    }

    @ExposedMethod
    final boolean list___nonzero__() {
        return seq___nonzero__();
    }

    @ExposedMethod
    public PyObject list___iter__() {
        return seq___iter__();
    }

    @ExposedMethod(defaults = "null")
    final PyObject list___getslice__(PyObject start, PyObject stop, PyObject step) {
        return seq___getslice__(start, stop, step);
    }

    @ExposedMethod(defaults = "null")
    final void list___setslice__(PyObject start, PyObject stop, PyObject step, PyObject value) {
        if(value == null) {
            value = step;
            step = null;
        }
        seq___setslice__(start, stop, step, value);
    }

    @ExposedMethod(defaults = "null")
    final void list___delslice__(PyObject start, PyObject stop, PyObject step) {
        seq___delslice__(start, stop, step);
    }

    protected String unsupportedopMessage(String op, PyObject o2) {
        if(op.equals("+")) {
            return "can only concatenate list (not \"{2}\") to list";
        }
        return super.unsupportedopMessage(op, o2);
    }

    public String toString() {
        return list_toString();
    }

    @ExposedMethod(names = "__repr__")
    final String list_toString() {
        ThreadState ts = Py.getThreadState();
        if(!ts.enterRepr(this)) {
            return "[...]";
        }
        StringBuffer buf = new StringBuffer("[");
        int length = size();
        PyObject[] array = getArray();
        for(int i = 0; i < length - 1; i++) {
            buf.append((array[i]).__repr__().toString());
            buf.append(", ");
        }
        if(length > 0) {
            buf.append((array[length - 1]).__repr__().toString());
        }
        buf.append("]");
        ts.exitRepr(this);
        return buf.toString();
    }

    /**
     * Add a single element to the end of list.
     * 
     * @param o
     *            the element to add.
     */
    public void append(PyObject o) {
        list_append(o);
    }

    @ExposedMethod
    final void list_append(PyObject o) {
        pyadd(o);
        gListAllocatedStatus = __len__();
    }

    /**
     * Return the number elements in the list that equals the argument.
     * 
     * @param o
     *            the argument to test for. Testing is done with the <code>==</code> operator.
     */
    public int count(PyObject o) {
        return list_count(o);
    }

    @ExposedMethod
    final int list_count(PyObject o) {
        int count = 0;
        PyObject[] array = getArray();
        for(int i = 0, n = size(); i < n; i++) {
            if(array[i].equals(o)) {
                count++;
            }
        }
        return count;
    }

    /**
     * return smallest index where an element in the list equals the argument.
     * 
     * @param o
     *            the argument to test for. Testing is done with the <code>==</code> operator.
     */
    public int index(PyObject o) {
        return index(o, 0);
    }

    public int index(PyObject o, int start) {
        return list_index(o, start, null);
    }

    public int index(PyObject o, int start, int stop) {
        return list_index(o, start, Py.newInteger(stop));
    }

    @ExposedMethod(defaults = {"0", "null"})
    final int list_index(PyObject o, int start, PyObject stop) {
        int iStop;
        if(stop == null) {
            iStop = size();
        } else {
            iStop = stop.asInt();
        }
        return _index(o, "list.index(x): x not in list", start, iStop);
    }

    final int list_index(PyObject o, int start) {
        return _index(o, "list.index(x): x not in list", start, size());
    }

    final int list_index(PyObject o) {
        return _index(o, "list.index(x): x not in list", 0, size());
    }

    private int _index(PyObject o, String message, int start, int stop) {
        // Follow Python 2.3+ behavior
        int validStop = calculateIndex(stop);
        int validStart = calculateIndex(start);
        PyObject[] array = getArray();
        for(int i = validStart; i < validStop && i < size(); i++) {
            if(array[i].equals(o)) {
                return i;
            }
        }
        throw Py.ValueError(message);
    }

    // This is closely related to fixindex in PySequence, but less strict
    // fixindex returns -1 if index += length < 0 or if index >= length
    // where this function returns 0 in former case and length in the latter.
    // I think both are needed in different cases, but if this method turns
    // out to be needed in other sequence subclasses, it should be moved to
    // PySequence.
    private int calculateIndex(int index) {
        int length = size();
        if(index < 0) {
            index = index += length;
            if(index < 0) {
                index = 0;
            }
        } else if(index > length) {
            index = length;
        }
        return index;
    }

    /**
     * Insert the argument element into the list at the specified index. <br>
     * Same as <code>s[index:index] = [o] if index &gt;= 0</code>.
     * 
     * @param index
     *            the position where the element will be inserted.
     * @param o
     *            the element to insert.
     */
    public void insert(int index, PyObject o) {
        list_insert(index, o);
    }

    @ExposedMethod
    final void list_insert(int index, PyObject o) {
        if(index < 0) {
            index = Math.max(0, size() + index);
        }
        if(index > size()) {
            index = size();
        }
        list.pyadd(index, o);
        gListAllocatedStatus = __len__();
    }

    /**
     * Remove the first occurence of the argument from the list. The elements arecompared with the
     * <code>==</code> operator. <br>
     * Same as <code>del s[s.index(x)]</code>
     * 
     * @param o
     *            the element to search for and remove.
     */
    public void remove(PyObject o) {
        list_remove(o);
    }

    @ExposedMethod
    final void list_remove(PyObject o) {
        del(_index(o, "list.remove(x): x not in list", 0, size()));
        gListAllocatedStatus = __len__();
    }

    /**
     * Reverses the items of s in place. The reverse() methods modify the list in place for economy
     * of space when reversing a large list. It doesn't return the reversed list to remind you of
     * this side effect.
     */
    public void reverse() {
        list_reverse();
    }

    @ExposedMethod
    final void list_reverse() {
        PyObject tmp;
        int n = size();
        PyObject[] array = getArray();
        int j = n - 1;
        for(int i = 0; i < n / 2; i++, j--) {
            tmp = array[i];
            array[i] = array[j];
            array[j] = tmp;
        }
        gListAllocatedStatus = __len__();
    }

    /**
     * Removes and return the last element in the list.
     */
    public PyObject pop() {
        return pop(-1);
    }

    /**
     * Removes and return the <code>n</code> indexed element in the list.
     * 
     * @param n
     *            the index of the element to remove and return.
     */
    public PyObject pop(int n) {
        return list_pop(n);
    }

    @ExposedMethod(defaults = "-1")
    final PyObject list_pop(int n) {
        int length = size();
        if(length == 0) {
            throw Py.IndexError("pop from empty list");
        }
        if(n < 0) {
            n += length;
        }
        if(n < 0 || n >= length) {
            throw Py.IndexError("pop index out of range");
        }
        PyObject v = pyget(n);
        setslice(n, n + 1, 1, Py.EmptyTuple);
        return v;
    }

    /**
     * Append the elements in the argument sequence to the end of the list. <br>
     * Same as <code>s[len(s):len(s)] = o</code>.
     * 
     * @param o
     *            the sequence of items to append to the list.
     */
    public void extend(PyObject o) {
        list_extend(o);
    }

    @ExposedMethod
    final void list_extend(PyObject o) {
        int length = size();
        setslice(length, length, 1, o);
        gListAllocatedStatus = __len__();
    }

    public PyObject __iadd__(PyObject o) {
        return list___iadd__(o);
    }

    @ExposedMethod
    final PyObject list___iadd__(PyObject o) {
        extend(fastSequence(o, "argument to += must be a sequence"));

        return this;
    }

    /**
     * Sort the items of the list in place. The compare argument is a function of two arguments
     * (list items) which should return -1, 0 or 1 depending on whether the first argument is
     * considered smaller than, equal to, or larger than the second argument. Note that this slows
     * the sorting process down considerably; e.g. to sort a list in reverse order it is much faster
     * to use calls to the methods sort() and reverse() than to use the built-in function sort()
     * with a comparison function that reverses the ordering of the elements.
     * 
     * @param compare
     *            the comparison function.
     */
    
        /**
     * Sort the items of the list in place. Items is compared with the normal relative comparison
     * operators.
     */


    @ExposedMethod
    final void list_sort(PyObject[] args, String[] kwds) {
        ArgParser ap = new ArgParser("list", args, kwds, new String[]{"cmp", "key", "reverse"}, 0);
        PyObject cmp = ap.getPyObject(0, Py.None);
        PyObject key = ap.getPyObject(1, Py.None);
        PyObject reverse = ap.getPyObject(2, Py.False);
        sort(cmp, key, reverse);
    }

    public void sort(PyObject compare) {
        sort(compare, Py.None, Py.False);
    }

    public void sort() {
        sort(Py.None, Py.None, Py.False);
    }

    public void sort(PyObject cmp, PyObject key, PyObject reverse) {
        MergeState ms = new MergeState(this, cmp, key, reverse.__nonzero__());
        ms.sort();
    }
  
    public int hashCode() {
        return list___hash__();
    }

    @ExposedMethod
    final int list___hash__() {
        throw Py.TypeError("unhashable type");
    }

    public PyTuple __getnewargs__() {
        return new PyTuple(new PyTuple(list.getArray()));
    }
}
