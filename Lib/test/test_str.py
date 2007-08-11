import unittest
from test import test_support, string_tests


class StrTest(
    string_tests.CommonTest,
    string_tests.MixinStrUnicodeUserStringTest,
    string_tests.MixinStrUserStringTest
    ):

    type2test = str

    # We don't need to propagate to str
    def fixtype(self, obj):
        return obj

    def test_formatting(self):
        string_tests.MixinStrUnicodeUserStringTest.test_formatting(self)
# Jython transition 2.3
# values outside of the size of a single char aren't prohibited in formatting %c
# http://jython.org/bugs/1768075     
#        self.assertRaises(OverflowError, '%c'.__mod__, 0x1234)

def test_main():
    test_support.run_unittest(StrTest)

if __name__ == "__main__":
    test_main()
