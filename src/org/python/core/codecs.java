/*
 * Copyright 2000 Finn Bock
 *
 * This program contains material copyrighted by:
 * Copyright (c) Corporation for National Research Initiatives.
 * Originally written by Marc-Andre Lemburg (mal@lemburg.com).
 */
package org.python.core;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * Contains the implementation of the builtin codecs.
 * @since Jython 2.0
 */
public class codecs {

    public static final String BACKSLASHREPLACE = "backslashreplace";
    public static final String IGNORE = "ignore";
    public static final String REPLACE = "replace";
    public static final String XMLCHARREFREPLACE = "xmlcharrefreplace";
    private static char Py_UNICODE_REPLACEMENT_CHARACTER = 0xFFFD;
    private static PyList searchPath;
    private static PyStringMap searchCache;
    private static PyStringMap errorHandlers;
    /** Used to synchronize registry_init. */
    private static final Object INIT_LOCK = new Object();
    private static String default_encoding = "ascii";

    public static String getDefaultEncoding() {
        return default_encoding;
    }

    public static void setDefaultEncoding(String encoding) {
        lookup(encoding);
        default_encoding = encoding;
    }

    public static PyObject lookup_error(String handlerName) {
        registry_init();
        if (handlerName == null) {
            handlerName = "strict";
        }
        PyObject handler = errorHandlers.__finditem__(handlerName.intern());
        if (handler == null) {
            throw new PyException(Py.LookupError,
                    "unknown error handler name '" + handlerName + "'");
        }
        return handler;
    }

    public static void register_error(String name, PyObject error) {
        registry_init();
        if (!error.isCallable()) {
            throw Py.TypeError("argument must be callable");
        }
        errorHandlers.__setitem__(name.intern(), error);
    }

    public static void register(PyObject search_function) {
        registry_init();
        if (!search_function.isCallable()) {
            throw Py.TypeError("argument must be callable");
        }
        searchPath.append(search_function);
    }

    public static PyTuple lookup(String encoding) {
        registry_init();
        PyString v = new PyString(normalizestring(encoding));
        PyObject result = searchCache.__finditem__(v);
        if (result != null) {
            return (PyTuple) result;
        }

        if (searchPath.__len__() == 0) {
            throw new PyException(Py.LookupError,
                    "no codec search functions registered: " +
                    "can't find encoding");
        }

        PyObject iter = searchPath.__iter__();
        PyObject func = null;
        while ((func = iter.__iternext__()) != null) {
            result = func.__call__(v);
            if (result == Py.None) {
                continue;
            }
            if (!(result instanceof PyTuple) || result.__len__() != 4) {
                throw Py.TypeError("codec search functions must " +
                        "return 4-tuples");
            }
            break;
        }
        if (func == null) {
            throw new PyException(Py.LookupError, "unknown encoding " +
                    encoding);
        }
        searchCache.__setitem__(v, result);
        return (PyTuple) result;
    }

    private static String normalizestring(String string) {
        return string.toLowerCase().replace(' ', '-');
    }
    private static boolean import_encodings_called = false;

    private static void import_encodings() {
        if (!import_encodings_called) {
            import_encodings_called = true;
            try {
                __builtin__.__import__("encodings");
            } catch (PyException exc) {
                if (exc.type != Py.ImportError) {
                    throw exc;
                }
            }
        }
    }

    public static PyObject decode(PyString v, String encoding,
            String errors) {
        if (encoding == null) {
            encoding = getDefaultEncoding();
        } else {
            encoding = normalizestring(encoding);
        }

        if (errors != null) {
            errors = errors.intern();
        }

        /* Shortcut for ascii encoding */
        if (encoding.equals("ascii")) {
            return new PyUnicode(
                    PyUnicode_DecodeASCII(v.toString(), v.__len__(), errors),
                    true);
        }

        /* Decode via the codec registry */
        PyObject decoder = getDecoder(encoding);
        PyObject result = null;
        if (errors != null) {
            result = decoder.__call__(v, new PyString(errors));
        } else {
            result = decoder.__call__(v);
        }

        if (!(result instanceof PyTuple) || result.__len__() != 2) {
            throw Py.TypeError("decoder must return a tuple " +
                    "(object,integer)");
        }
        return result.__getitem__(0);
    }

    private static PyObject getDecoder(String encoding) {
        PyObject codecs = lookup(encoding);
        return codecs.__getitem__(1);
    }

    public static String encode(PyString v, String encoding,
            String errors) {
        if (encoding == null) {
            encoding = getDefaultEncoding();
        } else {
            encoding = normalizestring(encoding);
        }

        if (errors != null) {
            errors = errors.intern();
        }

        /* Shortcuts for common default encodings.  latin-1 must not use the
         * lookup registry for the encodigs module to work correctly */
        if (encoding.equals("latin-1")) {
            return PyUnicode_EncodeLatin1(v.toString(), v.__len__(), errors);

        } else if (encoding.equals("ascii")) {
            return PyUnicode_EncodeASCII(v.toString(),
                    v.__len__(), errors);
        }

        /* Decode via the codec registry */
        PyObject encoder = getEncoder(encoding);
        PyObject result = null;
        if (errors != null) {
            result = encoder.__call__(v, new PyString(errors));
        } else {
            result = encoder.__call__(v);
        }

        if (!(result instanceof PyTuple) || result.__len__() != 2) {
            throw Py.TypeError("encoder must return a tuple " +
                    "(object,integer)");
        }
        PyObject encoded = result.__getitem__(0);
        if (encoded instanceof PyString) {
            return encoded.toString();
        } else {
            throw Py.TypeError("decoder did not return a string/unicode object (type=" +
                    encoded.getType().fastGetName() + ")");
        }
    }

    private static PyObject getEncoder(String encoding) {
        PyObject codecs = lookup(encoding);
        return codecs.__getitem__(0);
    }

    public static PyObject strict_errors(PyObject[] args, String[] kws) {
        ArgParser ap = new ArgParser("strict_errors", args, kws, "exc");
        PyObject exc = ap.getPyObject(0);
        if (Py.isInstance(exc, Py.UnicodeDecodeError)) {
            throw new PyException(Py.UnicodeDecodeError, exc);
        } else if (Py.isInstance(exc, Py.UnicodeEncodeError)) {
            throw new PyException(Py.UnicodeEncodeError, exc);
        } else if (Py.isInstance(exc, Py.UnicodeTranslateError)) {
            throw new PyException(Py.UnicodeTranslateError, exc);
        }
        throw wrong_exception_type(exc);
    }

    public static PyObject ignore_errors(PyObject[] args, String[] kws) {
        ArgParser ap = new ArgParser("ignore_errors", args, kws, "exc");
        PyObject exc = ap.getPyObject(0);
        if (!isUnicodeError(exc)) {
            throw wrong_exception_type(exc);
        }
        PyObject end = exc.__getattr__("end");
        return new PyTuple(Py.java2py(""), end);
    }

    private static boolean isUnicodeError(PyObject exc) {
        return Py.isInstance(exc, Py.UnicodeDecodeError) ||
                Py.isInstance(exc, Py.UnicodeEncodeError) ||
                Py.isInstance(exc, Py.UnicodeTranslateError);
    }

    public static PyObject replace_errors(PyObject[] args, String[] kws) {
        ArgParser ap = new ArgParser("replace_errors", args, kws, "exc");
        PyObject exc = ap.getPyObject(0);
        if (Py.isInstance(exc, Py.UnicodeDecodeError)) {
            PyObject object = exc.__getattr__("object");
            if (!Py.isInstance(object, PyString.TYPE) || Py.isInstance(object, PyUnicode.TYPE)) {
                throw Py.TypeError("object attribute must be str");        
            }
            PyObject end = exc.__getattr__("end");
            return new PyTuple(new PyUnicode(Py_UNICODE_REPLACEMENT_CHARACTER), end);
        } else if (Py.isInstance(exc, Py.UnicodeEncodeError)) {
            PyObject object = exc.__getattr__("object");
            if (!Py.isInstance(object, PyUnicode.TYPE)) {
                throw Py.TypeError("object attribute must be unicode");        
            }
            PyObject end = exc.__getattr__("end");
            return new PyTuple(Py.java2py("?"), end);
        } else if (Py.isInstance(exc, Py.UnicodeTranslateError)) {
            PyObject end = exc.__getattr__("end");
            return new PyTuple(new PyUnicode(Py_UNICODE_REPLACEMENT_CHARACTER), end);
        }
        throw wrong_exception_type(exc);
    }

    public static PyObject xmlcharrefreplace_errors(PyObject[] args, String[] kws) {
        ArgParser ap = new ArgParser("xmlcharrefreplace_errors", args, kws, "exc");
        PyObject exc = ap.getPyObject(0);
        if (!Py.isInstance(exc, Py.UnicodeEncodeError)) {
            throw wrong_exception_type(exc);
        }
        int start = ((PyInteger) exc.__getattr__("start")).getValue();
        int end = ((PyInteger) exc.__getattr__("end")).getValue();
        String object = exc.__getattr__("object").toString();
        StringBuilder replacement = new StringBuilder();
        xmlcharrefreplace_internal(start, end, object, replacement);
        return new PyTuple(Py.java2py(replacement.toString()), exc.__getattr__("end"));
    }

    public static StringBuilder xmlcharrefreplace(int start, int end, String toReplace) {
        StringBuilder replacement = new StringBuilder();
        xmlcharrefreplace_internal(start, end, toReplace, replacement);
        return replacement;
    }

    private static void xmlcharrefreplace_internal(int start, int end, String object, StringBuilder replacement) {
        for (int i = start; i < end; i++) {
            replacement.append("&#");
            char cur = object.charAt(i);
            int digits;
            int base;
            if (cur < 10) {
                digits = 1;
                base = 1;
            } else if (cur < 100) {
                digits = 2;
                base = 10;
            } else if (cur < 1000) {
                digits = 3;
                base = 100;
            } else if (cur < 10000) {
                digits = 4;
                base = 1000;
            } else if (cur < 100000) {
                digits = 5;
                base = 10000;
            } else if (cur < 1000000) {
                digits = 6;
                base = 100000;
            } else {
                digits = 7;
                base = 1000000;
            }
            while (digits-- > 0) {
                replacement.append((char) ('0' + cur / base));
                cur %= base;
                base /= 10;
            }
            replacement.append(';');
        }
    }

    private static PyException wrong_exception_type(PyObject exc) {
        PyObject excClass = exc.__getattr__("__class__");
        PyObject className = excClass.__getattr__("__name__");
        return new PyException(Py.TypeError, "Don't know how to handle " + className + " in error callback");
    }
    static char hexdigits[] = {
        '0', '1', '2', '3', '4', '5', '6', '7',
        '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'
    };

    public static PyObject backslashreplace_errors(PyObject[] args, String[] kws) {
        ArgParser ap = new ArgParser("backslashreplace_errors", args, kws, "exc");
        PyObject exc = ap.getPyObject(0);
        if (!Py.isInstance(exc, Py.UnicodeEncodeError)) {
            throw wrong_exception_type(exc);
        }
        int start = ((PyInteger) exc.__getattr__("start")).getValue();
        int end = ((PyInteger) exc.__getattr__("end")).getValue();
        String object = exc.__getattr__("object").toString();
        StringBuilder replacement = new StringBuilder();
        backslashreplace_internal(start, end, object, replacement);
        return new PyTuple(Py.java2py(replacement.toString()), exc.__getattr__("end"));
    }

    public static StringBuilder backslashreplace(int start, int end, String toReplace) {
        StringBuilder replacement = new StringBuilder();
        backslashreplace_internal(start, end, toReplace, replacement);
        return replacement;
    }

    private static void backslashreplace_internal(int start, int end, String object, StringBuilder replacement) {
        for (Iterator<Integer> iter = new StringSubsequenceIterator(object, start, end, 1); iter.hasNext();) {
            int c = iter.next();
            replacement.append('\\');
            if (c >= 0x00010000) {
                replacement.append('U');
                replacement.append(hexdigits[(c >> 28) & 0xf]);
                replacement.append(hexdigits[(c >> 24) & 0xf]);
                replacement.append(hexdigits[(c >> 20) & 0xf]);
                replacement.append(hexdigits[(c >> 16) & 0xf]);
                replacement.append(hexdigits[(c >> 12) & 0xf]);
                replacement.append(hexdigits[(c >> 8) & 0xf]);
            } else if (c >= 0x100) {
                replacement.append('u');
                replacement.append(hexdigits[(c >> 12) & 0xf]);
                replacement.append(hexdigits[(c >> 8) & 0xf]);
            } else {
                replacement.append('x');
            }
            replacement.append(hexdigits[(c >> 4) & 0xf]);
            replacement.append(hexdigits[c & 0xf]);
        }
    }

    private static void registry_init() {
        synchronized (INIT_LOCK) {
            if (searchPath != null) {
                return;
            }
            searchPath = new PyList();
            searchCache = new PyStringMap();
            errorHandlers = new PyStringMap();
            String[] builtinErrorHandlers = new String[]{"strict",
                IGNORE,
                REPLACE,
                XMLCHARREFREPLACE,
                BACKSLASHREPLACE
            };
            for (int i = 0; i < builtinErrorHandlers.length; i++) {
                register_error(builtinErrorHandlers[i], Py.newJavaFunc(codecs.class,
                        builtinErrorHandlers[i] + "_errors"));
            }
            import_encodings();
        }
    }
    /* --- UTF-7 Codec -------------------------------------------------------- */

    /* see RFC2152 for details */
    public static char utf7_special[] = {
        /*
         * indicate whether a UTF-7 character is special i.e. cannot be directly
         * encoded: 0 - not special 1 - special 2 - whitespace (optional) 3 -
         * RFC2152 Set O (optional)
         */
        1, 1, 1, 1, 1, 1, 1, 1, 1, 2, 2, 1, 1, 2, 1, 1,
        1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
        2, 3, 3, 3, 3, 3, 3, 0, 0, 0, 3, 1, 0, 0, 0, 1,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 3, 3, 3, 3, 0,
        3, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 3, 1, 3, 3, 3,
        3, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 3, 3, 3, 1, 1
    ,

            
           };
    
    private static boolean SPECIAL(char c, boolean encodeO, boolean encodeWS){
    return (c>127 || utf7_special[(c)] == 1) ||
                (encodeWS && (utf7_special[(c)] == 2)) ||
                (encodeO && (utf7_special[(c)] == 3));
    }
    private static final String B64_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";

    private static char B64(int n) {
        return B64_CHARS.charAt(n & 0x3f);
    }

    private static boolean B64CHAR(char c) {
        return B64_CHARS.indexOf(c) != -1;
    }

    private static int UB64(char c) {
        return ((c) == '+' ? 62 : (c) == '/' ? 63 : (c) >= 'a' ? (c) - 71 : (c) >= 'A' ? (c) - 65 : (c) + 4);
    }

    // note that we follow CPython 2.5 exactly here - it does not support surrogates,
    // but has to process as-if they are there for replacement purposes
    // fortunately no one really cares about utf-7
    public static String PyUnicode_DecodeUTF7(String str,
            String errors) {
        int s = 0;
        int e = str.length();
        boolean inShift = false;
        int bitsInCharsleft = 0;
        long charsleft = 0;
        boolean surrogate = false;
        char highOrderSurrogate = 0;
        StringBuilder unicode = new StringBuilder(e);
        while (s < e) {
            // restart:
            char ch = str.charAt(s);
            if (inShift) {
                if ((ch == '-') || !B64CHAR(ch)) {
                    inShift = false;
                    s++;
                    while (bitsInCharsleft >= 16) {
                        bitsInCharsleft -= 16;
                        char outCh = (char) ((charsleft >> bitsInCharsleft) & 0xffff);
                        if (surrogate) {
                            s = codecs.insertReplacementAndGetResume(unicode,
                                    errors,
                                    "utf-7",
                                    str,
                                    s,
                                    s + 1,
                                    "code pairs are not supported");
                            surrogate = false;
                        } else if (0xDC00 <= outCh && outCh <= 0xDFFF) {
                            surrogate = true;
                            highOrderSurrogate = outCh;
                        } else {
                            unicode.append(outCh);
                        }
                    }
                    if (bitsInCharsleft >= 6) {
                        /*
                         * The shift sequence has a partial character in it. If
                         * bitsleft < 6 then we could just classify it as
                         * padding but that is not the case here
                         */
                        s = insertReplacementAndGetResume(unicode,
                                errors,
                                "utf-7",
                                str,
                                s,
                                s + 1,
                                "partial character in shift sequence");
                    }
                    /*
                     * According to RFC2152 the remaining bits should be zero.
                     * We choose to signal an error/insert a replacement
                     * character here so indicate the potential of a misencoded
                     * character.
                     */
                    if (bitsInCharsleft > 0 && ((charsleft << 5 - bitsInCharsleft) & 0x1f) > 0) {
                        s = insertReplacementAndGetResume(unicode,
                                errors,
                                "utf-7",
                                str,
                                s,
                                s + 1,
                                "non-zero padding bits in shift sequence");
                    }
                    if (ch == '-') {
                        if ((s < e) && (str.charAt(s) == '-')) {
                            unicode.append('-');
                            inShift = true;
                        }
                    } else if (SPECIAL(ch, false, false)) {
                        s = insertReplacementAndGetResume(unicode,
                                errors,
                                "utf-7",
                                str,
                                s,
                                s + 1,
                                "unexpected special character");
                    } else {
                        unicode.append(ch);
                    }
                } else {
                    charsleft = (charsleft << 6) | UB64(ch);
                    bitsInCharsleft += 6;
                    s++;
                    while (bitsInCharsleft >= 16) {
                        bitsInCharsleft -= 16;
                        char outCh = (char) ((charsleft >> bitsInCharsleft) & 0xffff);
                        if (surrogate) {
                            s = codecs.insertReplacementAndGetResume(unicode,
                                    errors,
                                    "utf-7",
                                    str,
                                    s,
                                    s + 1,
                                    "code pairs are not supported");
                        } else if (0xDC00 <= outCh && outCh <= 0xDFFF) {
                            surrogate = true;
                            highOrderSurrogate = outCh;
                        } else {
                            unicode.append(outCh);
                        }
                    }
                }
            } else if (ch == '+') {
                s++;
                if (s < e && str.charAt(s) == '-') {
                    s++;
                    unicode.append('+');
                } else {
                    inShift = true;
                    bitsInCharsleft = 0;
                }
            } else if (SPECIAL(ch, false, false)) {
                s = insertReplacementAndGetResume(unicode,
                        errors,
                        "utf-7",
                        str,
                        s,
                        s + 1,
                        "unexpected special character");
            } else {
                unicode.append(ch);
                s++;
            }
            if (inShift && s == e) {
                s = insertReplacementAndGetResume(unicode,
                        errors,
                        "utf-7",
                        str,
                        s,
                        s,
                        "unterminated shift sequence");
            }
        }
        return unicode.toString();
    }

    public static String PyUnicode_EncodeUTF7(String str,
            boolean encodeSetO,
            boolean encodeWhiteSpace,
            String errors) {
        int size = str.length();

        if (size == 0) {
            return "";
        }
        boolean inShift = false;
        int bitsleft = 0;
        int charsleft = 0;

        StringBuilder v = new StringBuilder();

        for (int i = 0; i < size; ++i) {
            char ch = str.charAt(i);

            if (!inShift) {
                if (ch == '+') {
                    v.append('+');
                    v.append('-');
                } else if (SPECIAL(ch, encodeSetO, encodeWhiteSpace)) {
                    charsleft = ch;
                    bitsleft = 16;
                    v.append('+');
                    while (bitsleft >= 6) {
                        v.append(B64(charsleft >> (bitsleft - 6)));
                        bitsleft -= 6;
                    }
                    inShift = bitsleft > 0;
                } else {
                    v.append(ch);
                }
            } else {
                if (!SPECIAL(ch, encodeSetO, encodeWhiteSpace)) {
                    v.append(B64(charsleft << (6 - bitsleft)));
                    charsleft = 0;
                    bitsleft = 0;
                    /* Characters not in the BASE64 set implicitly unshift the sequence
                    so no '-' is required, except if the character is itself a '-' */
                    if (B64CHAR(ch) || ch == '-') {
                        v.append('-');
                    }
                    inShift = false;
                    v.append(ch);
                } else {
                    bitsleft += 16;
                    charsleft = (charsleft << 16) | ch;
                    while (bitsleft >= 6) {
                        v.append(B64(charsleft >> (bitsleft - 6)));
                        bitsleft -= 6;
                    }
                    /* If the next character is special then we dont' need to terminate
                    the shift sequence. If the next character is not a BASE64 character 
                    or '-' then the shift sequence will be terminated implicitly and we
                    don't have to insert a '-'. */

                    if (bitsleft == 0) {
                        if (i + 1 < size) {
                            char ch2 = str.charAt(i + 1);

                            if (SPECIAL(ch2, encodeSetO, encodeWhiteSpace)) {

                            } else if (B64CHAR(ch2) || ch2 == '-') {
                                v.append('-');
                                inShift = false;
                            } else {
                                inShift = false;
                            }

                        } else {
                            v.append('-');
                            inShift = false;
                        }
                    }
                }
            }
        }
        if (bitsleft > 0) {
            v.append(B64(charsleft << (6 - bitsleft)));
            v.append('-');
        }
        return v.toString();
    }
    /* --- UTF-8 Codec ---------------------------------------------------- */
    private static byte utf8_code_length[] = {
        /* Map UTF-8 encoded prefix byte to sequence length.  zero means
        illegal prefix.  see RFC 2279 for details */
        1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
        1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
        1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
        1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
        1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
        1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
        1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
        1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2,
        2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2,
        3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3,
        4, 4, 4, 4, 4, 4, 4, 4, 5, 5, 5, 5, 6, 6, 0, 0
    };


    // TODO: need to modify to use a codepoint approach (which is almost the case now,
    // ch is an
    public static String PyUnicode_DecodeUTF8(String str, String errors) {
        return PyUnicode_DecodeUTF8Stateful(str, errors, null);
    }

    public static String PyUnicode_DecodeUTF8Stateful(String str, String errors, int[] consumed) {
        int size = str.length();
        StringBuilder unicode = new StringBuilder(size);

        /* Unpack UTF-8 encoded data */
        int i;
        for (i = 0; i < size;) {
            int ch = str.charAt(i);

            if (ch < 0x80) {
                unicode.append((char) ch);
                i++;
                continue;
            }
            if (ch > 0xFF) {
                i = insertReplacementAndGetResume(unicode, errors, "utf-8", str, i, i + 1, "ordinal not in range(255)");
                continue;
            }

            int n = utf8_code_length[ch];

            if (i + n > size) {
                if (consumed != null) {
                    break;
                }
                i = insertReplacementAndGetResume(unicode, errors, "utf-8", str, i, i + 1, "unexpected end of data");
                continue;
            }


            switch (n) {
                case 0:
                    i = insertReplacementAndGetResume(unicode, errors, "utf-8", str, i, i + 1, "unexpected code byte");
                    continue;
                case 1:
                    i = insertReplacementAndGetResume(unicode, errors, "utf-8", str, i, i + 1, "internal error");
                    continue;
                case 2:
                    char ch1 = str.charAt(i + 1);
                    if ((ch1 & 0xc0) != 0x80) {
                        i = insertReplacementAndGetResume(unicode, errors, "utf-8", str, i, i + 2, "invalid data");
                        continue;
                    }
                    ch = ((ch & 0x1f) << 6) + (ch1 & 0x3f);
                    if (ch < 0x80) {
                        i = insertReplacementAndGetResume(unicode, errors, "utf-8", str, i, i + 2, "illegal encoding");
                        continue;
                    } else {
                        unicode.appendCodePoint(ch);
                    }
                    break;

                case 3:
                    ch1 = str.charAt(i + 1);
                    char ch2 = str.charAt(i + 2);
                    if ((ch1 & 0xc0) != 0x80 || (ch2 & 0xc0) != 0x80) {
                        i = insertReplacementAndGetResume(unicode, errors, "utf-8", str, i, i + 3, "invalid data");
                        continue;
                    }
                    ch = ((ch & 0x0f) << 12) + ((ch1 & 0x3f) << 6) + (ch2 & 0x3f);
                    if (ch < 0x800 || (ch >= 0xd800 && ch < 0xe000)) {
                        i = insertReplacementAndGetResume(unicode, errors, "utf-8", str, i, i + 3, "illegal encoding");
                        continue;
                    } else {
                        unicode.appendCodePoint(ch);
                    }
                    break;

                case 4:
                    ch1 = str.charAt(i + 1);
                    ch2 = str.charAt(i + 2);
                    char ch3 = str.charAt(i + 3);
                    if ((ch1 & 0xc0) != 0x80 ||
                            (ch2 & 0xc0) != 0x80 ||
                            (ch3 & 0xc0) != 0x80) {
                        i = insertReplacementAndGetResume(unicode, errors, "utf-8", str, i, i + 4, "invalid data");
                        continue;
                    }
                    ch = ((ch & 0x7) << 18) + ((ch1 & 0x3f) << 12) +
                            ((ch2 & 0x3f) << 6) + (ch3 & 0x3f);
                    /* validate and convert to UTF-16 */
                    if ((ch < 0x10000) || /* minimum value allowed for 4
                            byte encoding */
                            (ch > 0x10ffff)) {  /* maximum value allowed for
                        UTF-16 */
                        i = insertReplacementAndGetResume(unicode, errors, "utf-8", str, i, i + 4, "illegal encoding");
                        continue;
                    }

                    unicode.appendCodePoint(ch);
                    break;

                default:
                    // TODO: support
                /* Other sizes are only needed for UCS-4 */
                    i = insertReplacementAndGetResume(unicode, errors, "utf-8", str, i, i + n, "unsupported Unicode code range");
                    continue;
            }
            i += n;
        }

        if (consumed != null) {
            consumed[0] = i;
        }

        return unicode.toString();
    }

    public static String PyUnicode_EncodeUTF8(String str, String errors) {
        final Charset utf8 = Charset.forName("UTF-8");
        final ByteBuffer bbuf = utf8.encode(str);
        final StringBuilder v = new StringBuilder(bbuf.limit());
        while (bbuf.remaining() > 0) {
            int val = bbuf.get();
            if (val < 0) {
                val = 256 + val;
            }
            v.appendCodePoint(val);
        }
        return v.toString();
    }

    public static String PyUnicode_DecodeASCII(String str, int size,
            String errors) {
        return PyUnicode_DecodeIntLimited(str, size, errors, "ascii", 128);
    }

    public static String PyUnicode_DecodeLatin1(String str, int size,
            String errors) {
        return PyUnicode_DecodeIntLimited(str, size, errors, "latin-1", 256);
    }

    private static String PyUnicode_DecodeIntLimited(String str, int size, String errors, String encoding, int limit) {
        StringBuilder v = new StringBuilder(size);

        String reason = "ordinal not in range(" + limit + ")";
        for (int i = 0; i < size; i++) {
            char ch = str.charAt(i);
            if (ch < limit) {
                v.append(ch);
            } else {
                i = insertReplacementAndGetResume(v, errors,
                        encoding,
                        str,
                        i,
                        i + 1,
                        reason) - 1;
            }
        }

        return v.toString();
    }

    public static String PyUnicode_EncodeASCII(String str, int size,
            String errors) {
        return PyUnicode_EncodeIntLimited(str, size, errors, "ascii", 128);
    }

    public static String PyUnicode_EncodeLatin1(String str, int size,
            String errors) {

        return PyUnicode_EncodeIntLimited(str, size, errors, "latin-1", 256);
    }

    private static String PyUnicode_EncodeIntLimited(String str, int size,
            String errors, String encoding, int limit) {
        String reason = "ordinal not in range(" + limit + ")";
        StringBuilder v = new StringBuilder(size);
        for (int i = 0; i < size; i++) {
            char ch = str.charAt(i);
            if (ch >= limit) {
                int nextGood = i + 1;
                for (; nextGood < size; nextGood++) {
                    if (str.charAt(nextGood) < limit) {
                        break;
                    }
                }
                if (errors != null) {
                    if (errors.equals(IGNORE)) {
                        i = nextGood - 1;
                        continue;
                    } else if (errors.equals(REPLACE)) {
                        for (int j = i; j < nextGood; j++) {
                            v.append('?');
                        }
                        i = nextGood - 1;
                        continue;
                    } else if (errors.equals(XMLCHARREFREPLACE)) {
                        v.append(xmlcharrefreplace(i, nextGood, str));
                        i = nextGood - 1;
                        continue;
                    } else if (errors.equals(BACKSLASHREPLACE)) {
                        v.append(backslashreplace(i, nextGood, str));
                        i = nextGood - 1;
                        continue;
                    }
                }
                PyObject replacement = encoding_error(errors,
                        encoding,
                        str,
                        i,
                        nextGood,
                        reason);
                String replStr = replacement.__getitem__(0).toString();
                for (int j = 0; j < replStr.length(); j++) {
                    if (replStr.charAt(j) >= limit) {
                        throw Py.UnicodeEncodeError(encoding, str, i + j, i + j + 1, reason);
                    }
                }
                v.append(replStr);
                i = calcNewPosition(size, replacement) - 1;
            } else {
                v.append(ch);
            }
        }
        return v.toString();
    }

    public static int calcNewPosition(int size, PyObject errorTuple) {
        int newPosition = ((PyInteger) errorTuple.__getitem__(1)).getValue();
        if (newPosition < 0) {
            newPosition = size + newPosition;
        }
        if (newPosition > size || newPosition < 0) {
            throw Py.IndexError(newPosition + " out of bounds of encoded string");
        }
        return newPosition;
    }
    /* --- RawUnicodeEscape Codec ---------------------------------------- */
    private static char[] hexdigit = "0123456789ABCDEF".toCharArray();

    // The modified flag is used by cPickle.
    public static String PyUnicode_EncodeRawUnicodeEscape(String str,
            String errors,
            boolean modifed) {

        int size = str.length();
        StringBuilder v = new StringBuilder(str.length());

        for (int i = 0; i < size; i++) {
            char ch = str.charAt(i);
            if (ch >= 256 || (modifed && (ch == '\n' || ch == '\\'))) {
                v.append("\\u");
                v.append(hexdigit[(ch >>> 12) & 0xF]);
                v.append(hexdigit[(ch >>> 8) & 0xF]);
                v.append(hexdigit[(ch >>> 4) & 0xF]);
                v.append(hexdigit[ch & 0xF]);
            } else {
                v.append(ch);
            }
        }

        return v.toString();
    }

    public static String PyUnicode_DecodeRawUnicodeEscape(String str,
            String errors) {
        int size = str.length();
        StringBuilder v = new StringBuilder(size);
        for (int i = 0; i < size;) {
            char ch = str.charAt(i);
            /* Non-escape characters are interpreted as Unicode ordinals */
            if (ch != '\\') {
                v.append(ch);
                i++;
                continue;
            }
            /*
             * \\u-escapes are only interpreted iff the number of leading
             * backslashes is odd
             */
            int bs = i;
            while (i < size) {
                ch = str.charAt(i);
                if (ch != '\\') {
                    break;
                }
                v.append(ch);
                i++;
            }
            if (((i - bs) & 1) == 0 || i >= size || ch != 'u') {
                continue;
            }
            v.setLength(v.length() - 1);
            i++;
            /* \\uXXXX with 4 hex digits */
            int x = 0, d = 0, j = 0;
            for (; j < 4; j++) {
                ch = str.charAt(i + j);
                d = Character.digit(ch, 16);
                if (d == -1) {
                    break;
                }
                x = ((x << 4) & ~0xF) + d;
            }
            if (d == -1) {
                i = codecs.insertReplacementAndGetResume(v,
                        errors,
                        "unicodeescape",
                        str,
                        bs,
                        i + j,
                        "truncated \\uXXXX");
            } else {
                i += 4;
                v.append((char) x);
            }
        }
        return v.toString();
    }

    private static class Punycode {
        // specified by punycode, http://www.ietf.org/rfc/rfc3492.txt
        private static final int BASE = 36;
        private static final int TMIN = 1;
        private static final int TMAX = 26;
        private static final int SKEW = 38;
        private static final int DAMP = 700;
        private static final int INITIAL_BIAS = 72;
        private static final int INITIAL_N = 128;
        private static final int BASIC = 0x80;

        private Punycode() {

        }

        private static int adapt(int delta, int numpoints, boolean firsttime) {
            delta = firsttime ? delta / DAMP : delta >> 1;
            delta += delta / numpoints;
            int k = 0;
            while (delta > (((BASE - TMIN) * TMAX) / 2)) {
                delta /= BASE - TMIN;
                k += BASE;
            }
            return k + (((BASE - TMIN + 1) * delta) / (delta + SKEW));
        }

        private static boolean isBasic(int codePoint) {
            return codePoint < BASIC;
        }
    }

    public static String PyUnicode_EncodePunycode(PyUnicode input,
            String errors) {
        int n = Punycode.INITIAL_N;
        int delta = 0;
        long guard_delta;
        int bias = Punycode.INITIAL_BIAS;
        int b = 0;
        final StringBuilder buffer = new StringBuilder();
        for (Iterator<Integer> iter = input.iterator(); iter.hasNext();) {
            int c = iter.next();
            if (Punycode.isBasic(c)) {
                buffer.appendCodePoint(c);
                b++;
            }
        }
        if (b > 0) {
            buffer.appendCodePoint('-');
        }
        int h = b;
        int size = input.getCodePointCount();
        while (h < size) {
            int m = Integer.MAX_VALUE;
            int i = 0;
            int codePointIndex = 0;
            for (Iterator<Integer> iter = input.iterator(); iter.hasNext(); i++) {
                int c = iter.next();
                if (c > n && c < m) {
                    m = c;
                    codePointIndex = i;
                }
            }
            guard_delta = delta + ((m - n) * (h + 1));
            if (guard_delta > Integer.MAX_VALUE) {
                throw Py.UnicodeEncodeError("punycode", input.string, codePointIndex, codePointIndex + 1, "overflow");
            }
            delta = (int) guard_delta;

            n = m;
            i = 0;
            for (Iterator<Integer> iter = input.iterator(); iter.hasNext(); i++) {
                int c = iter.next();
                if (c < n) {
                    guard_delta = delta + 1;
                    if (guard_delta > Integer.MAX_VALUE) {
                        throw Py.UnicodeEncodeError("punycode", input.string, i, i + 1, "overflow");
                    }
                    delta = (int) guard_delta;
                }
                if (c == n) {
                    int q = delta;
                    for (int k = Punycode.BASE;; k += Punycode.BASE) {
                        int t = k <= bias ? Punycode.TMIN : (k >= bias + Punycode.TMAX ? Punycode.TMAX : k - bias);
                        if (q < t) {
                            break;
                        }
                        buffer.appendCodePoint(t + ((q - t) % (Punycode.BASE - t)));
                        q = (q - t) / (Punycode.BASE - t);
                    }
                    buffer.appendCodePoint(q);
                    bias = Punycode.adapt(delta, h + 1, h == b);
                    delta = 0;
                    h++;
                }
            }
            delta++;
            n++;
        }
        return buffer.toString();
    }

    public static PyUnicode PyUnicode_DecodePunycode(String input, String errors) {

        int input_size = input.length();
        int output_size = 0;
        ArrayList<Integer> ucs4 = new ArrayList(input_size);
        int j = 0;
        for (; j < input_size; j++) {
            int c = input.charAt(j);
            if (!Punycode.isBasic(c)) {
                throw Py.UnicodeDecodeError("punycode", input, j, j + 1, "not basic");
            } else if (c == '-') {
                break;
            } else {
                ucs4.add(c);
                output_size++;
            }
        }

        int n = Punycode.INITIAL_N;
        int i = 0;
        int bias = Punycode.INITIAL_BIAS;
        while (j < input_size) {
            int old_i = i;
            int w = 1;
            for (int k = Punycode.BASE;; k += Punycode.BASE) {
                int c = input.charAt(j++);
                int digit = c - '0';
                long guard_i = i + digit * w;
                if (guard_i > Integer.MAX_VALUE) {
                    throw Py.UnicodeDecodeError("punycode", input, j, j + 1, "overflow");
                }
                i = (int) guard_i;
                int t = k <= bias ? Punycode.TMIN : (k >= bias + Punycode.TMAX ? Punycode.TMAX : k - bias);
                if (digit < t) {
                    break;
                }
                long guard_w = w * Punycode.BASE - t;
                if (guard_w > Integer.MAX_VALUE) {
                    throw Py.UnicodeDecodeError("punycode", input, j, j + 1, "overflow");
                }
            }
            bias = Punycode.adapt(i - old_i, output_size + 1, old_i == 0);
            n += i / (output_size + 1);
            i %= output_size + 1;
            ucs4.add(i, n);

        }
        return new PyUnicode(ucs4);
    }

    public static String PyUnicode_EncodeIDNA(PyUnicode input,
            String errors) {

        throw new UnsupportedOperationException();


//   1. If the sequence contains any code points outside the ASCII range
//      (0..7F) then proceed to step 2, otherwise skip to step 3.
//
//   2. Perform the steps specified in [NAMEPREP] and fail if there is an
//      error.  The AllowUnassigned flag is used in [NAMEPREP].
// this basically enails changing out space, etc.
//
//   3. If the UseSTD3ASCIIRules flag is set, then perform these checks:
//
//     (a) Verify the absence of non-LDH ASCII code points; that is, the
//         absence of 0..2C, 2E..2F, 3A..40, 5B..60, and 7B..7F.
//
//     (b) Verify the absence of leading and trailing hyphen-minus; that
//         is, the absence of U+002D at the beginning and end of the
//         sequence.
//
//   4. If the sequence contains any code points outside the ASCII range
//      (0..7F) then proceed to step 5, otherwise skip to step 8.
//
//   5. Verify that the sequence does NOT begin with the ACE prefix.
//
//   6. Encode the sequence using the encoding algorithm in [PUNYCODE] and
//      fail if there is an error.
//
//   7. Prepend the ACE prefix.
//
//   8. Verify that the number of code points is in the range 1 to 63
//      inclusive.

    }

    public static PyUnicode PyUnicode_DecodeIDNA(String input, String errors) {
        throw new UnsupportedOperationException();
    }

    /* --- Utility methods -------------------------------------------- */
    public static PyObject encoding_error(String errors,
            String encoding,
            String toEncode,
            int start,
            int end,
            String reason) {
        PyObject errorHandler = lookup_error(errors);
        PyException exc = Py.UnicodeEncodeError(encoding,
                toEncode,
                start,
                end,
                reason);
        exc.normalize();
        PyObject replacement = errorHandler.__call__(new PyObject[]{exc.value});
        checkErrorHandlerReturn(errors, replacement);
        return replacement;
    }

    public static int insertReplacementAndGetResume(StringBuilder partialDecode,
            String errors,
            String encoding,
            String toDecode,
            int start,
            int end,
            String reason) {
        if (errors != null) {
            if (errors.equals(IGNORE)) {
                return end;
            } else if (errors.equals(REPLACE)) {
                while (start < end) {
                    partialDecode.appendCodePoint(Py_UNICODE_REPLACEMENT_CHARACTER);
                    start++;
                }
                return end;
            }
        }
        PyObject replacement = decoding_error(errors,
                encoding,
                toDecode,
                start,
                end,
                reason);
        checkErrorHandlerReturn(errors, replacement);
        partialDecode.append(replacement.__getitem__(0).toString());
        return calcNewPosition(toDecode.length(), replacement);
    }

    public static PyObject decoding_error(String errors,
            String encoding,
            String toEncode,
            int start,
            int end,
            String reason) {
        PyObject errorHandler = lookup_error(errors);
        PyException exc = Py.UnicodeDecodeError(encoding,
                toEncode,
                start,
                end,
                reason);
        exc.normalize();
        return errorHandler.__call__(new PyObject[]{exc.value});
    }

    private static void checkErrorHandlerReturn(String errors,
            PyObject replacement) {
        if (!(replacement instanceof PyTuple) || replacement.__len__() != 2 || !(replacement.__getitem__(0) instanceof PyBaseString) || !(replacement.__getitem__(1) instanceof PyInteger)) {
            throw new PyException(Py.TypeError, "error_handler " + errors + " must return a tuple of (replacement, new position)");
        }
    }
}


class StringSubsequenceIterator implements Iterator {

    private final String s;
    private int current,  k,  start,  stop,  step;

    StringSubsequenceIterator(String s, int start, int stop, int step) {
//        System.out.println("s=" + s.length() + ",start=" + start + ",stop=" + stop);
        this.s = s;
        k = 0;
        current = start;
        this.start = start;
        this.stop = stop;
        this.step = step;
      
        // this bounds checking is necessary to convert between use of code units elsewhere, and codepoints here
        // it would be nice if it were unnecessary!
        int count = getCodePointCount(s);
        if (start >= count) {
            this.stop = -1;
        }
        else if (stop >= count) {
            this.stop = count;
        }
        
        for (int i = 0; i < start; i++) {
            nextCodePoint();
        }
    }

    StringSubsequenceIterator(String s) {
        this(s, 0, getCodePointCount(s), 1);
    }

    private static int getCodePointCount(String s) {
        return s.codePointCount(0, s.length());
    }
    
    public boolean hasNext() {
        return current < stop;
    }

    public Object next() {
        int codePoint = nextCodePoint();
        current += 1;
        for (int j = 1; j < step && hasNext(); j++) {
            nextCodePoint();
            current += 1;
        }
        return codePoint;
    }

    private int nextCodePoint() {
        int U;
//        System.out.println("k=" + k);
        int W1 = s.charAt(k);
        if (W1 >= 0xD800 && W1 < 0xDC00) {
            int W2 = s.charAt(k + 1);
            U = (((W1 & 0x3FF) << 10) | (W2 & 0x3FF)) + 0x10000;
            k += 2;
        } else {
            U = W1;
            k += 1;
        }
        return U;
    }

    public void remove() {
        throw new UnsupportedOperationException("Not supported on String objects (immutable)");
    }
}
