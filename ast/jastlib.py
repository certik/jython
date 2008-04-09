"""lispify_ast - returns a tuple representation of the AST

Uses 2.5's _ast, not other AST implementations in CPython, since these
are not used by the compilation phase. And that's what we're
interested in.

Since this is a tuple, we can directly compare, and this is going to
be handy when comparing Jython's implementation vs CPython.

"""
import org.python.antlr.PythonTree as AST
import org.python.antlr.Main as parser

from types import ArrayType

def lispify_ast(node):
    return tuple(lispify_ast2(node))

def lispify_ast2(node):
    s = node.__class__.__name__
    name = s.split(".")[-1]
    if name.endswith("Type"):
        name = name[:-4]
    if name == "Unicode":
        name = "Str"
    yield name
    try:
        for field in node._fields:
            yield tuple(lispify_field(field, getattr(node, field), node))
    except:
        pass

def lispify_field(field, child, parent):
    fname = field
    yield field
    if not isinstance(child, ArrayType):
        children = [child]
    else:
        children = child

    for node in children:
        if isinstance(node, AST):
            yield lispify_ast(node)
        else:
            if fname in ("ctx", "ops", "op"):
                yield tuple([str(node)])
            elif fname == "n":
                try:
                    if isinstance(node, float):
                        if .0001 < node < 10000:
                            yield "%5.5f" % node
                        else:
                            s = "%.5e" % node
                            #jython puts an extra zero in exponent when it has less than three digits.
                            s = s.replace('e+0', 'e+')
                            yield s.replace('e-0', 'e-')
                    else:
                        yield node
                except Exception, why:
                    print "error parsing 'n': %s" % why
            elif fname == "s":
                s = node.replace('\b', 'BACKSPACE')
                s = s.replace('\f', 'FORMFEED')
                if parent.__class__.__name__ == 'org.python.antlr.ast.Unicode':
                    yield unicode(s)
                else:
                    yield s
            else:
                yield node

if __name__ == '__main__':
    import sys
    from pprint import pprint

    code_path = sys.argv[1]
    ast = parser().parse([code_path])

    lispified = lispify_ast(ast)
    pprint(lispified)
    #assert(lispified == lispify_ast(ast2))
