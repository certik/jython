package org.python.core;

public class PyFloat extends PyObject {
	private double value;

    public static PyClass __class__;
	public PyFloat(double v) { super(__class__); value = v; }
	public PyFloat(float v) { this((double)v); }

    public double getValue() {
        return value;
    }

	public String toString() {
		return Double.toString(value);
	}

	public int hashCode() {
	    double intPart = Math.floor(value);
	    double fractPart = value-intPart;

	    if (fractPart == 0) {
	        if (intPart <= Integer.MAX_VALUE && intPart >= Integer.MIN_VALUE) {
	            return (int)value;
	        } else {
	            return __long__().hashCode();
	        }
        } else {
            long v = Double.doubleToLongBits(value);
            return (int)v ^ (int)(v >> 32);
        }
	}

	public boolean __nonzero__() {
		return value != 0;
	}

	public Object __tojava__(Class c) {
		if (c == Double.TYPE || c == Number.class || 
		        c == Double.class || c == Object.class) {
		    return new Double(value);
		}
		if (c == Float.TYPE || c == Float.class) {
		    return new Float(value);
		}
		return super.__tojava__(c);
	}

	public int __cmp__(PyObject other) {
		double v = ((PyFloat)other).value;
		return value < v ? -1 : value > v ? 1 : 0;
	}

	public Object __coerce_ex__(PyObject other) {
		if (other instanceof PyFloat) return other;
		else {
			if (other instanceof PyInteger) return new PyFloat((double)((PyInteger)other).getValue());
			if (other instanceof PyLong) return new PyFloat(((PyLong)other).doubleValue());
			else return Py.None;
		}
	}


	public PyObject __add__(PyObject right) {
		return new PyFloat(value+((PyFloat)right).value);
	}

	public PyObject __sub__(PyObject right) {
		return new PyFloat(value-((PyFloat)right).value);
	}

	public PyObject __mul__(PyObject right) {
		return new PyFloat(value*((PyFloat)right).value);
	}

	public PyObject __div__(PyObject right) {
		double y = ((PyFloat)right).value;
		if (y == 0) throw Py.ZeroDivisionError("float division");
		return new PyFloat(value/y);
	}

    private double modulo(double x, double y) {
		if (y == 0) throw Py.ZeroDivisionError("float modulo");
	    double z = Math.IEEEremainder(x, y);
	    if (z*y < 0) z += y;
        return z;
    }

	public PyObject __mod__(PyObject right) {
	    return new PyFloat(modulo(value, ((PyFloat)right).value));
	}

	public PyObject __divmod__(PyObject right) {
		double y = ((PyFloat)right).value;
		if (y == 0) throw Py.ZeroDivisionError("float division");
		double z = Math.floor(value/y);

		return new PyTuple(new PyObject[] {new PyFloat(z), new PyFloat(value-z*y)});
	}

	public PyObject __pow__(PyObject right, PyObject modulo) {
	    // Rely completely on Java's pow function
	    double ret = Math.pow(value, ((PyFloat)right).value);
	    if (modulo == null) {
		    return new PyFloat(ret);
		} else {
		    return new PyFloat(modulo(ret, ((PyFloat)modulo).value));
		}
	}

	public PyObject __neg__() {
		return new PyFloat(-value);
	}

	public PyObject __pos__() {
		return this;
	}

	public PyObject __abs__() {
		if (value >= 0)
			return this;
		else
			return __neg__();
	}

	public PyInteger __int__() {
	    if (value <= Integer.MAX_VALUE && value >= Integer.MIN_VALUE) {
		    return new PyInteger((int)value);
		}
		throw Py.OverflowError("float too large to convert");
	}

	public PyLong __long__() {
        return new PyLong(value);
    }

	public PyFloat __float__() {
		return this;
	}

	public PyComplex __complex__() {
		return new PyComplex(value, 0.);
	}
}
