from SimpleCompiler import SimpleCompiler
from PythonModule import PythonModule
from ObjectFactory import ObjectFactory
from Object import PyObject
import jast
import ImportName
import os, string
from types import *
import java, org

import javac
import proxies

from java.lang import *

def getdata(filename):
	fp = open(filename, "r")
	data = fp.read()
	fp.close()
	return data

def getsig(doc):
	if doc is None: return None
	print doc
	
	lines = doc.split("\n")
	txt = None
	for line in lines:
		line = line.strip()
		if line.startswith("@sig "):
			txt = line[5:]
			break
	if txt is None: return None
	front, back = txt.split("(")
	back = back[:-1]
	front = front.split()
	ret = front[-2]
	mods = string.join(front[:-2], ' ')
	
	arglist = []
	args = back.split(',')
	for arg in args:
		c, name = arg.split()
		jc = getJavaClass(c)
		arglist.append( (jc, name) )
	
	return [mods, getJavaClass(ret), arglist]

primitives = {'void':Void.TYPE, 'int':Integer.TYPE, 'byte':Byte.TYPE, 
	'short':Short.TYPE, 'long':Long.TYPE, 'float':Float.TYPE, 'double':Double.TYPE,
	'boolean':Boolean.TYPE, 'char':Character.TYPE}

def getJavaClass(c):
	if isinstance(c, StringType):
		if primitives.has_key(c): return primitives[c]
		try:
			return Class.forName(c)
		except:
			return None
	elif isinstance(c, ImportName.JavaClass):
		return Class.forName(c.name)
	elif isinstance(c, Class):
		return c
	else:
		return None


def makeJavaProxy(module, pyc):
	frame = pyc.frame
	methods = []
	for name, func in frame.names.items():
		v = func.value.value
		args = None
		if hasattr(v, 'args'):
			args = v.args
		sig = None
		if hasattr(v, 'doc'):
			sig = getsig(v.doc)
		methods.append( (name, args, sig) )

	bases = []
	for base in pyc.bases:
		#print 'base', base, base.value
		base = base.value
		
		if hasattr(base, 'name'):
			jc = getJavaClass(base.name)
			if jc is not None:
				bases.append(jc)

	#print bases
	if len(bases) == 0:
		return None

	jp = proxies.JavaProxy(pyc.name, bases, methods, module)
	return jp # jp.makeClass()


def printNames(heading, dict):
	items = dict.items()
	if len(items) == 0:
		return
	
	print	
	print heading

	items1 = []
	for key, value in items:
		if hasattr(key, '__name__'):
			key = key.__name__	
		value = value.keys()
		items1.append( (key, value) )

	items1.sort()
	for key, value in items1:
		print '  %s used in %s' % (key, string.join(value, ', '))


class Compiler:
	def __init__(self, javapackage=None, deep = 1, skip=(), 
			include=('org.python.modules', 'com.oroinc.text.regex')):
		self.javapackage = javapackage
		self.deep = deep
		self.packages = {}
		self.events = {}
		self.depends = {}
		self.modules = {}
		self.javasources = []
		self.files = []
		self.javaclasses = []
		self.javadepends = {}
		self.pypackages = {}
		PyObject.attributes = {}
		self.skip = skip
		self.dependencies = {}
		self.include = include
		
	def write(self, msg):
		print msg

	def compilefile(self, filename, name):
		filename = java.io.File(filename).getCanonicalPath()
		
		if self.modules.has_key(filename): return
		self.write('processing %s' % name)
		
		self.modules[filename] = 1
		mod = self.compile(getdata(filename), filename, name)
		self.modules[filename] = mod

	def compile(self, data, filename, name):
		if self.javapackage is not None:
			name = self.javapackage+'.'+name
			
		data = "__file__=%s\n"%repr(filename)+data+"\n\n"

		mod = PythonModule(name, filename)
		fact = ObjectFactory()
		pi = SimpleCompiler(mod, fact)
		fact.parent = pi
		code = jast.Block(pi.execstring(data))
		mod.addMain(code, pi)
		
		self.addDependencies(mod)

		return mod
		
	def addJavaClass(self, name, parent):
		#print 'add java class', name
		
		for package in self.include:
			if name[:len(package)+1] == package+'.':
				ps = self.javadepends.get(name, [])
				ps.append(parent)
				if len(ps) == 1:
					self.javadepends[name] = ps
				
	def addDependency(self, m, attrs, mod):
		if m is None: return
		
		if isinstance(m, ImportName.Package):
			self.packages[m.name] = 1
		elif isinstance(m, ImportName.Module):
			if m.file is None:
				file = os.path.join(m.path[0], '__init__.py')
				name = m.name+'.__init__'
				self.depends[file] = name
				self.pypackages[m.path[0]] = m.name
			else:
				self.depends[m.file] = m.name
		elif isinstance(m, ImportName.JavaClass):
			m.addEvents(attrs, self.events, mod.name)
			self.addJavaClass(m.name, mod.name)
		
		if self.dependencies.has_key(m): return
		self.dependencies[m] = 1
		for depend in m.getDepends():
			#print 'depends on', depend
			self.addDependency(depend, attrs, mod)

	def addDependencies(self, mod):
		attrs = PyObject.attributes
		PyObject.attributes = {}
		#print '  attrs', attrs.keys()
		for name in mod.imports.keys():
			#print '  depends', name
			m = ImportName.lookupName(name)
			self.addDependency(m, attrs, mod)				
				
		if self.deep:
			for filename, name in self.depends.items():
				#self.write('%s requires %s' % (mod.name, name))
				if name in self.skip:
					self.write('  %s skipping %s' % (mod.name, name))
				self.compilefile(filename, name)

	def filterpackages(self):
		prefixes = {}
		for name in self.packages.keys():
			parts = name.split('.')
			for i in range(1, len(parts)):
				prefixes[string.join(parts[:i], '.')] = 1
		#print prefixes
		for name in self.packages.keys():
			if prefixes.has_key(name):
				del self.packages[name]
	
	def processModule(self, mod, outdir):
		self.write('  %s module' % mod.name)
		proxyClasses = []
		mainProxy = None
		for name, pyc in mod.classes.items():
			#print name, pyc
			proxy = makeJavaProxy(mod, pyc.value)
			if proxy is None: continue
			
			self.write('    '+proxy.getDescription())
			
			if name == mod.name:
				mainProxy = proxy
			else:
				proxyClasses.append(proxy)

		mod.packages = self.packages.keys()	
		specialClasses = {}
		pkg = mod.package
		if pkg is None: pkg = ""
		else: pkg = pkg+'.'
		
		if mainProxy is not None:
			mod.javaproxy = mainProxy
			specialClasses[mod.name+'.'+mainProxy.name] = pkg+mod.name

		for proxy in proxyClasses:
			proxy.modifier = "public static"
			mod.innerClasses.append(proxy)
			specialClasses[mod.name+'.'+proxy.name] = pkg+mod.name+'$'+proxy.name

		mod.specialClasses = specialClasses
		
		self.javasources.append(mod.dump(outdir))		
		self.javaclasses.extend(mod.javaclasses)
		
	def displayPackages(self):
		print
		print 'Required packages:'
		for package in self.packages.keys():
			print '  '+package

	def dump(self, outdir):
		self.filterpackages()
		self.displayPackages()
		adapters = {}
		for jc, sources in self.events.items():
			adapters[jc.__name__] = sources.keys()


		self.write('\nCreating adapters:')
		for adapter, sources in adapters.items():
			self.write('  %s used in %s' % (adapter, string.join(sources, ', ')))
			self.makeAdapter(outdir, adapter)
			
		self.write('\nCreating .java files:')
		for filename, mod in self.modules.items():
			self.processModule(mod, outdir)
			
		self.java2class()
	
	def java2class(self):
		self.write('\nCompiling .java to .class...')
		code, outtext, errtext = javac.compile(self.javasources)
		print code, outtext

	def makeAdapter(self, outdir, proxy):
		os = java.io.ByteArrayOutputStream()
		org.python.compiler.AdapterMaker.makeAdapter(proxy, os)
		filename = writeclass(outdir, 'org.python.proxies.'+proxy+'$Adapter', os)
		self.javaclasses.append('org.python.proxies.'+proxy+'$Adapter')

	def trackJavaDependencies(self):
		if len(self.javadepends) == 0: []
		
		from depend import depends
		done = {}
		self.write('Tracking java dependencies:')
		indent = 1
		while len(done) < len(self.javadepends):
			for name, parents in self.javadepends.items():
				if done.has_key(name): continue
				self.write(('  '*indent)+name) #'%s required by %s' % (name, string.join(parents, ', ')))
				ze, jcs = depends(name)
				done[name] = ze
				for jc in jcs:
					self.addJavaClass(jc, name)
			#print len(done), len(self.javadepends)
			indent = indent+1
		return done.values()


from java.io import *
def writefile(filename, instream):
	file = File(filename)
	File(file.getParent()).mkdirs()
	outstream = FileOutputStream(file)	
	instream.writeTo(outstream)
	
def writeclass(outdir, classname, stream):
	filename = apply(os.path.join, tuple(classname.split('.')))+'.class'
	filename = os.path.join(outdir, filename)
	writefile(filename, stream)
	return filename

def compile(files, outdir):
	c = Compiler()
	for filename, classname in files:
		c.compilefile(filename, classname)
	c.dump(outdir)
	return c.files, c.javaclasses
	
if __name__ == '__main__':
	import sys
	filenames = sys.argv[1:]
	print filenames
	outdir = "."
	files = []
	for filename in filenames:
		outdir = os.path.dirname(filename)
		classname = os.path.splitext(os.path.basename(filename))[0]
		files.append( (filename, classname) )
	files, javaclasses = compile(files, outdir)

	#sys.exit()	

	print 'Building archive...'
	from jar import JavaArchive
	ja = JavaArchive([('org.python.core', []),])
	for jc in javaclasses:
		ja.addClass(outdir, jc)
	outjar = "c:\\jpython\\tools\\jpythonc2\\test\\t.jar"	
	ja.dump(outjar)
