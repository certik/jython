
import glob, os

PYTHONDIR = r"d:/python/Python-2.1"

def listfiles(root, d, *masks, **kws):
    exclude = kws.get('exclude', [])
    for mask in masks:
        if root:
            rootdir = os.path.join(root, d, mask)
        else:
            rootdir = os.path.join(d, mask)
    
        fl = glob.glob(rootdir)
        for f in fl:
            if os.path.basename(f) in exclude: 
                continue
            # After this, the name is a true mess of different 
            # path seperator chars. Attempt to clean this by making
            # the jar contain only unix seps.
            if root:
                normname = f[len(root)+1:].replace('\\', '/')
                osname = f.replace('/', os.sep)
                print "t %-35s %s" % (normname, osname)
            else:
                normname = f.replace('\\', '/')
                osname = f.replace('/', os.sep)
                print "t %-35s ..%s%s" % (normname, os.sep, osname)

def comment(txt):
    print "#"
    print "#", txt
    print "#"


def binary(txt):
    print "b %-35s ..%s%s" % (txt, os.sep, txt)

def text(txt):
    print "t %-35s ..%s%s" % (txt, os.sep, txt)


demofiles = ("*.java", "*.html", "*.py", "*.txt", "Makefile")
javafiles = ("*.java", "*.jjt", "Makefile")

#list of files that will be taken from CPython.
pylibfiles = [
    '__future__.py',
    'BaseHTTPServer.py',
    'CGIHTTPServer.py',
    'ConfigParser.py',
    'Cookie.py',
    'MimeWriter.py',
    'Queue.py',
    'SimpleHTTPServer.py',
    'SocketServer.py',
    'StringIO.py',
    'UserDict.py',
    'UserList.py',
    'anydbm.py',
    'base64.py',
    'bdb.py',
    'binhex.py',
    'bisect.py',
    'calendar.py',
    'cgi.py',
    'cmd.py',
    'cmp.py',
    'cmpcache.py',
    'colorsys.py',
    'commands.py',
    'compileall.py',
    #'copy.py',
    'copy_reg.py',
    'dircache.py',
    'dircmp.py',
    'dospath.py',
    'dumbdbm.py',
    'exceptions.py',
    'fileinput.py',
    'fnmatch.py',
    'formatter.py',
    'fpformat.py',
    'ftplib.py',
    'getopt.py',
    'glob.py',
    'gopherlib.py',
    'gzip.py',
    'htmlentitydefs.py',
    'htmllib.py',
    'httplib.py',
    'imaplib.py',
    'imghdr.py',
    'keyword.py',
    'linecache.py',
    'macpath.py',
    'macurl2path.py',
    'mailbox.py',
    'mailcap.py',
    'mhlib.py',
    'mimetools.py',
    'mimetypes.py',
    'mimify.py',
    'multifile.py',
    'mutex.py',
    'nntplib.py',
    'ntpath.py',
    'nturl2path.py',
    'pdb.py',
    'pickle.py',
    'pipes.py',
    'popen2.py',
    'poplib.py',
    'posixfile.py',
    'posixpath.py',
    'pprint.py',
    'profile.py',
    'pyclbr.py',
    'quopri.py',
    'random.py',
    'reconvert.py',
    'repr.py',
    'rfc822.py',
    'sched.py',
    'sgmllib.py',
    'site.py',
    'shelve.py',
    'shutil.py',
    'smtplib.py',
    'sndhdr.py',
    'stat.py',
    'symbol.py',
    'telnetlib.py',
    'tempfile.py',
    'token.py',
    'tokenize.py',
    'traceback.py',
    'tzparse.py',
    'urllib.py',
    'urlparse.py',
    'user.py',
    'whichdb.py',
    'whrandom.py',
    'xdrlib.py',
    'xmllib.py',
    #'zipfile.py',
    'test/pystone.py',

    # New CPython files added in Jython
    'pstats.py', 
    'code.py', 
    'codecs.py',
    #'re.py',
    'sre*.py',
    'encodings/*.py',
    'threading.py',
    'atexit.py',
    'UserString.py',
    'warnings.py',
]

print "#===== generated by mklist.py ====="
print "@core:_top_"
text("ACKNOWLEDGMENTS")
text("NEWS")
text("README.txt")
text("registry")
binary("jython.jar")
text("LICENSE.txt")
text("installer/jython_template.win_bat")
text("installer/jython_template.unix_sh")
text("installer/jythonc_template.win_bat")
text("installer/jythonc_template.unix_sh")
binary("installer/jython.gif")

comment("freeze")
listfiles(None, "Tools/freeze", "*.py")
comment("jythonc")
listfiles(None, "Tools/jythonc", "*.py")
listfiles(None, "Tools/jythonc/jast", "*.py")

comment("special library modules")
listfiles(None, "Lib", '*.py', 'pawt/*.py', exclude=["site.py"])

comment("Docs")
listfiles(None, "Doc", '*.html')
binary("Doc/images/jython-new-small.gif")
binary("Doc/images/PythonPoweredSmall.gif")
listfiles(None, "Doc/api", '*.html', '*.css')
listfiles(None, "Doc/api/org/python/core", '*.html')
listfiles(None, "Doc/api/org/python/util", '*.html')

comment("Demos")
print "@demo:_top_"
listfiles(None, "Demo/applet", *demofiles)
listfiles(None, "Demo/awt", *demofiles)
listfiles(None, "Demo/bean", *demofiles)
listfiles(None, "Demo/embed", *demofiles)
listfiles(None, "Demo/javaclasses", *demofiles)
listfiles(None, "Demo/javaclasses/pygraph", *demofiles)
listfiles(None, "Demo/swing", *demofiles)

comment("The source files")
print "@source:_top_"
text("org/apache/LICENSE")
listfiles(None, "org/apache/oro/text/regex", *javafiles)
listfiles(None, "org/python/compiler", *javafiles)
listfiles(None, "org/python/core", *javafiles)
listfiles(None, "org/python/modules", *javafiles)
listfiles(None, "org/python/modules/sre", *javafiles)
listfiles(None, "org/python/parser", exclude=["python.java"], *javafiles)
listfiles(None, "org/python/rmi", *javafiles)
listfiles(None, "org/python/util", *javafiles)
listfiles(None, "Lib/jxxload_help", *javafiles)

comment("Library modules from CPython")
print "@lib:_top_"
print "t Lib/LICENSE %s\LICENSE.txt" % PYTHONDIR
listfiles(PYTHONDIR, "Lib", *pylibfiles)
#listfiles(None, "Lib", 'site.py')
print "#===== end of list generated by mklist.py ====="
