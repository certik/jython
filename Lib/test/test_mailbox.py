import mailbox
import os
import test_support
import time
import unittest

# cleanup earlier tests
try:
    os.unlink(test_support.TESTFN)
except os.error:
    pass


DUMMY_MESSAGE = """\
From: some.body@dummy.domain
To: me@my.domain

This is a dummy message.
"""


class MaildirTestCase(unittest.TestCase):

    def setUp(self):
        # create a new maildir mailbox to work with:
        self._dir = test_support.TESTFN
        os.mkdir(self._dir)
        os.mkdir(os.path.join(self._dir, "cur"))
        os.mkdir(os.path.join(self._dir, "tmp"))
        os.mkdir(os.path.join(self._dir, "new"))
        self._counter = 1
        self._msgfiles = []

    def tearDown(self):
        map(os.unlink, self._msgfiles)
        os.rmdir(os.path.join(self._dir, "cur"))
        os.rmdir(os.path.join(self._dir, "tmp"))
        os.rmdir(os.path.join(self._dir, "new"))
        os.rmdir(self._dir)

    def createMessage(self, dir):
        t = int(time.time() % 1000000)
        pid = self._counter
        self._counter += 1
        filename = os.extsep.join((str(t), str(pid), "myhostname", "mydomain"))
        tmpname = os.path.join(self._dir, "tmp", filename)
        newname = os.path.join(self._dir, dir, filename)
        fp = open(tmpname, "w")
        self._msgfiles.append(tmpname)
        fp.write(DUMMY_MESSAGE)
        fp.close()
        if hasattr(os, "link"):
            os.link(tmpname, newname)
        else:
            fp = open(newname, "w")
            fp.write(DUMMY_MESSAGE)
            fp.close()
        self._msgfiles.append(newname)

    def assert_msg_exists(self):
	msg = self.mbox.next()
	self.assert_(msg is not None)
	#Force the file closed on Jython since Windows won't allow a file to 
        #be deleted if something has an open handle on it and garbage collection
        #doesn't happen quickly enough to make this occur naturally
	if os.name == 'java':
	    msg.fp.close() 

    def test_empty_maildir(self):
        """Test an empty maildir mailbox"""
        # Test for regression on bug #117490:
        # Make sure the boxes attribute actually gets set.
        self.mbox = mailbox.Maildir(test_support.TESTFN)
        self.assert_(hasattr(self.mbox, "boxes"))
        self.assert_(len(self.mbox.boxes) == 0)
        self.assert_(self.mbox.next() is None)
        self.assert_(self.mbox.next() is None)

    def test_nonempty_maildir_cur(self):
        self.createMessage("cur")
        self.mbox = mailbox.Maildir(test_support.TESTFN)
        self.assert_(len(self.mbox.boxes) == 1)
        self.assert_msg_exists()
        self.assert_(self.mbox.next() is None)
        self.assert_(self.mbox.next() is None)

    def test_nonempty_maildir_new(self):
        self.createMessage("new")
        self.mbox = mailbox.Maildir(test_support.TESTFN)
        self.assert_(len(self.mbox.boxes) == 1)
        self.assert_msg_exists()
        self.assert_(self.mbox.next() is None)
        self.assert_(self.mbox.next() is None)

    def test_nonempty_maildir_both(self):
        self.createMessage("cur")
        self.createMessage("new")
        self.mbox = mailbox.Maildir(test_support.TESTFN)
        self.assert_(len(self.mbox.boxes) == 2)
        self.assert_msg_exists()
        self.assert_msg_exists()
        self.assert_(self.mbox.next() is None)
        self.assert_(self.mbox.next() is None)

    # XXX We still need more tests!


def test_main():
    test_support.run_unittest(MaildirTestCase)


if __name__ == "__main__":
    test_main()
