r"""OS routines for Mac, DOS, NT, or Posix depending on what system we're on.

This exports:
  - all functions from posix, nt, dos, os2, mac, or ce, e.g. unlink, stat, etc.
  - os.path is one of the modules posixpath, ntpath, macpath, or dospath
  - os.name is 'posix', 'nt', 'dos', 'os2', 'mac', 'ce' or 'riscos'
  - os.curdir is a string representing the current directory ('.' or ':')
  - os.pardir is a string representing the parent directory ('..' or '::')
  - os.sep is the (or a most common) pathname separator ('/' or ':' or '\\')
  - os.altsep is the alternate pathname separator (None or '/')
  - os.pathsep is the component separator used in $PATH etc
  - os.linesep is the line separator in text files ('\r' or '\n' or '\r\n')
  - os.defpath is the default search path for executables

Programs that import and use 'os' stand a better chance of being
portable between different platforms.  Of course, they must then
only use functions that are defined by all platforms (e.g., unlink
and opendir), and leave all pathname manipulation to os.path
(e.g., split and join).
"""

__all__ = ["altsep", "curdir", "pardir", "sep", "pathsep", "linesep",
           "defpath", "name"]

import java
from java.io import File
import javapath
path = javapath

error = OSError

name = 'java' # descriminate based on JDK version?
curdir = '.'
pardir = '..' #This might not be right...
#curdir, pardir??
sep = java.io.File.separator
altsep = None
pathsep = java.io.File.pathSeparator
defpath = '.'
linesep = java.lang.System.getProperty('line.separator')

#I can do better than this...
environ = {}


def _exit(n=0):
    java.lang.System.exit(n)

def getcwd():
    foo = File(File("foo").getAbsolutePath())
    return foo.getParent()

def listdir(path):
    l = File(path).list()
    if l is None:
	raise OSError(0, 'No such directory', path)
    return list(l)

def mkdir(path, mode='ignored'):
    if not File(path).mkdir():
	raise OSError(0, "couldn't make directory", path)

def makedirs(path, mode='ignored'):
    if not File(path).mkdirs():
	raise OSError(0, "couldn't make directories", path)

def remove(path):
    if not File(path).delete():
	raise OSError(0, "couldn't delete file", path)

def rename(path, newpath):
    if not File(path).renameTo(File(newpath)):
	raise OSError(0, "couldn't rename file", path)

def rmdir(path):
    if not File(path).delete():
	raise OSError(0, "couldn't delete directory", path)

unlink = remove

def stat(path):
    """The Java stat implementation only returns a small subset of
    the standard fields"""
    f = File(path)
    size = f.length()
    # Sadly, if the returned length is zero, we don't really know if the file
    # is zero sized or does not exist.
    if size == 0 and not f.exists():
        raise OSError(0, 'No such file or directory', path)
    mtime = f.lastModified() / 1000.0
    return (0, 0, 0, 0, 0, 0, size, mtime, mtime, 0)
