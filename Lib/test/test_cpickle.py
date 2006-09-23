import cPickle
import test_support
import unittest
from cStringIO import StringIO
from pickletester import AbstractPickleTests, AbstractPickleModuleTests

class cPickleTests(AbstractPickleTests, AbstractPickleModuleTests):

    def setUp(self):
        self.dumps = cPickle.dumps
        self.loads = cPickle.loads

    error = cPickle.BadPickleGet
    module = cPickle

class cPicklePicklerTests(AbstractPickleTests):

    def dumps(self, arg, bin=0):
        f = StringIO()
        p = cPickle.Pickler(f, bin)
        p.dump(arg)
        f.seek(0)
        return f.read()

    def loads(self, buf):
        f = StringIO(buf)
        p = cPickle.Unpickler(f)
        return p.load()

    error = cPickle.BadPickleGet

class cPickleListPicklerTests(AbstractPickleTests):

    def dumps(self, arg, bin=0):
        p = cPickle.Pickler(bin)
        p.dump(arg)
        return p.getvalue()

    def loads(self, *args):
        f = StringIO(args[0])
        p = cPickle.Unpickler(f)
        return p.load()

    error = cPickle.BadPickleGet

class cPickleFastPicklerTests(AbstractPickleTests):

    def dumps(self, arg, bin=0):
        f = StringIO()
        p = cPickle.Pickler(f, bin)
        p.fast = 1
        p.dump(arg)
        f.seek(0)
        return f.read()

    def loads(self, *args):
        f = StringIO(args[0])
        p = cPickle.Unpickler(f)
        return p.load()

    error = cPickle.BadPickleGet

    def test_nonrecursive_deep(self):
        a = []
        for i in range(100):
            a = [a]
        b = self.loads(self.dumps(a))
        self.assertEqual(a, b)

def test_main():
    loader = unittest.TestLoader()
    suite = unittest.TestSuite()
    suite.addTest(loader.loadTestsFromTestCase(cPickleTests))
    suite.addTest(loader.loadTestsFromTestCase(cPicklePicklerTests))
    #Jython doesn't support list based picklers
    #suite.addTest(loader.loadTestsFromTestCase(cPickleListPicklerTests))
    suite.addTest(loader.loadTestsFromTestCase(cPickleFastPicklerTests))
    test_support.run_suite(suite)

if __name__ == "__main__":
    test_main()
