import java
import path

error = 'os.error'

name = 'jdk1.1'
curdir = '.'
#curdir, pardir??
sep = java.io.File.separator
pathsep = java.io.File.pathSeparator
#defpath?

#I can do better than this...
environ = {}


def _exit(n=0):
	java.lang.System.exit(n)
	
def listdir(path):
	dir = java.io.File(path)
	l = dir.list()
	if l is None:
		raise error, 'No such directory'
	return list(l)


def mkdir(path):
	dir = java.io.File(path)
	if not dir.mkdir():
		raise error, "couldn't make directory"

def remove(path):
	file = java.io.File(path)
	if not file.delete():
		raise error, "couldn't delete file"

def rmdir(path):
	dir = java.io.File(path)
	if not dir.delete():
		raise error, "couldn't delete directory"
				
unlink = remove


def stat(path):
	"""The Java stat implementation only returns a small subset of 
	the standard fields"""
	
	file = java.io.File(path)
	mtime = file.lastModified()
	size = file.length()
	
	return (0,0,0,0,0,0, size, mtime, mtime, 0)
