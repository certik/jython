"""
[ #448485 ] Tuple unpacking raises KeyError
"""

import support

import string
s = "elem1 elem2"
try:
   (a, b, c) = string.split(s)
except ValueError:
   pass
else:
   print support.TestError("Should raise a ValueError")

support.compileJPythonc("test314c.py", output="test314.err",
                        jar="test314.jar", core=1)
support.runJava("test314c", classpath="test314.jar")

