#!/usr/bin/python

#
# Test suite for Optik.  Supplied by Johannes Gijsbers
# (taradino@softhome.net) -- translated from the original Optik
# test suite to this PyUnit-based version.
#
# $Id: test_optparse.py 35634 2004-04-01 07:38:49Z fdrake $
#

import sys
import os
import copy
import unittest

from cStringIO import StringIO
from pprint import pprint
from test import test_support

from optparse import make_option, Option, IndentedHelpFormatter, \
     TitledHelpFormatter, OptionParser, OptionContainer, OptionGroup, \
     SUPPRESS_HELP, SUPPRESS_USAGE, OptionError, OptionConflictError, \
     BadOptionError, OptionValueError
from optparse import _match_abbrev

# Do the right thing with boolean values for all known Python versions.
try:
    True, False
except NameError:
    (True, False) = (1, 0)

class BaseTest(unittest.TestCase):
    def assertParseOK(self, args, expected_opts, expected_positional_args):
        """Assert the options are what we expected when parsing arguments.

        Otherwise, fail with a nicely formatted message.

        Keyword arguments:
        args -- A list of arguments to parse with OptionParser.
        expected_opts -- The options expected.
        expected_positional_args -- The positional arguments expected.

        Returns the options and positional args for further testing.
        """

        (options, positional_args) = self.parser.parse_args(args)
        optdict = vars(options)

        self.assertEqual(optdict, expected_opts,
                         """
Options are %(optdict)s.
Should be %(expected_opts)s.
Args were %(args)s.""" % locals())

        self.assertEqual(positional_args, expected_positional_args,
                         """
Positional arguments are %(positional_args)s.
Should be %(expected_positional_args)s.
Args were %(args)s.""" % locals ())

        return (options, positional_args)

    def assertRaises(self, func, expected_exception, expected_output,
                     get_output=None,
                     funcargs=[], funckwargs={}):
        """Assert the expected exception is raised when calling a function.

        Also check whether the right error message is given for a given error.

        Keyword arguments:
        func -- The function to be called.
        expected_exception -- The exception that should be raised.
        expected_output -- The output we expect to see.
        get_output -- The function to call to get the output.
        funcargs -- The arguments `func` should be called with.
        funckwargs -- The keyword arguments `func` should be called with.

        Returns the exception raised for further testing.
        """
        if get_output is None:
            get_output = self.exception

        try:
            out = func(*funcargs, **funckwargs)
        except expected_exception, err:
            output = get_output(err)

            self.failUnless(output.find(expected_output) != -1,
                            """
Message was:
%(output)s
Should contain:
%(expected_output)s
Function called:
%(func)s
With args/kwargs:
%(funcargs)s/%(funckwargs)s""" % locals())

            return err
        else:
            self.fail("""
No %(expected_exception)s raised.
Function called:
%(func)s
With args/kwargs:
%(funcargs)s/%(funckwargs)s""" % locals ())

    # -- Functions to be used as the get_output argument to assertRaises ------

    def exception(self, err):
        return str(err)

    def redirected_stdout(self, err):
        return sys.stdout.getvalue()

    # -- Assertions used in more than one class --------------------

    def assertParseFail(self, cmdline_args, expected_output):
        """Assert the parser fails with the expected message."""
        self.assertRaises(self.parser.parse_args, SystemExit, expected_output,
                          funcargs=[cmdline_args])

    def assertStdoutEquals(self, cmdline_args, expected_output):
        """Assert the parser prints the expected output on stdout."""
        sys.stdout = StringIO()
        self.assertRaises(self.parser.parse_args, SystemExit, expected_output,
                          self.redirected_stdout, [cmdline_args])
        sys.stdout = sys.__stdout__

    def assertTypeError(self, func, expected_output, *args):
        """Assert a TypeError is raised when executing func."""
        self.assertRaises(func, TypeError, expected_output, funcargs=args)

# -- Test make_option() aka Option -------------------------------------

# It's not necessary to test correct options here. All the tests in the
# parser.parse_args() section deal with those, because they're needed
# there. Duplication makes no sense to me.

class TestOptionChecks(BaseTest):
    def setUp(self):
        self.parser = OptionParser(usage=SUPPRESS_USAGE)

    def assertOptionError(self, expected_output, args=[], kwargs={}):
        self.assertRaises(make_option, OptionError, expected_output,
                          funcargs=args, funckwargs=kwargs)

    def test_opt_string_empty(self):
        self.assertTypeError(make_option,
                             "at least one option string must be supplied")

    def test_opt_string_too_short(self):
        self.assertOptionError("invalid option string 'b': "
                               "must be at least two characters long",
                               ["b"])

    def test_opt_string_short_invalid(self):
        self.assertOptionError("invalid short option string '--': must be "
                               "of the form -x, (x any non-dash char)",
                               ["--"])

    def test_opt_string_long_invalid(self):
        self.assertOptionError("invalid long option string '---': "
                               "must start with --, followed by non-dash",
                               ["---"])

    def test_attr_invalid(self):
        self.assertOptionError("invalid keyword arguments: foo, bar",
                               ["-b"], {'foo': None, 'bar': None})

    def test_action_invalid(self):
        self.assertOptionError("invalid action: 'foo'",
                               ["-b"], {'action': 'foo'})

    def test_type_invalid(self):
        self.assertOptionError("invalid option type: 'foo'",
                               ["-b"], {'type': 'foo'})

    def test_no_type_for_action(self):
        self.assertOptionError("must not supply a type for action 'count'",
                               ["-b"], {'action': 'count', 'type': 'int'})

    def test_no_choices_list(self):
        self.assertOptionError("must supply a list of "
                               "choices for type 'choice'",
                               ["-b", "--bad"], {'type': "choice"})

    def test_bad_choices_list(self):
        typename = type('').__name__
        self.assertOptionError("choices must be a list of "
                               "strings ('%s' supplied)" % typename,
                               ["-b", "--bad"],
                               {'type': "choice", 'choices':"bad choices"})

    def test_no_choices_for_type(self):
        self.assertOptionError("must not supply choices for type 'int'",
                               ["-b"], {'type': 'int', 'choices':"bad"})

    def test_no_const_for_action(self):
        self.assertOptionError("'const' must not be supplied for action "
                               "'store'",
                               ["-b"], {'action': 'store', 'const': 1})

    def test_no_nargs_for_action(self):
        self.assertOptionError("'nargs' must not be supplied for action "
                               "'count'",
                               ["-b"], {'action': 'count', 'nargs': 2})

    def test_callback_not_callable(self):
        self.assertOptionError("callback not callable: 'foo'",
                               ["-b"], {'action': 'callback',
                                        'callback': 'foo'})

    def dummy(self):
        pass

    def test_callback_args_no_tuple(self):
        self.assertOptionError("callback_args, if supplied, must be a tuple: "
                               "not 'foo'",
                               ["-b"], {'action': 'callback',
                                        'callback': self.dummy,
                                        'callback_args': 'foo'})

    def test_callback_kwargs_no_dict(self):
        self.assertOptionError("callback_kwargs, if supplied, must be a dict: "
                               "not 'foo'",
                               ["-b"], {'action': 'callback',
                                        'callback': self.dummy,
                                        'callback_kwargs': 'foo'})

    def test_no_callback_for_action(self):
        self.assertOptionError("callback supplied ('foo') for "
                               "non-callback option",
                               ["-b"], {'action': 'store',
                                        'callback': 'foo'})

    def test_no_callback_args_for_action(self):
        self.assertOptionError("callback_args supplied for non-callback "
                               "option",
                               ["-b"], {'action': 'store',
                                        'callback_args': 'foo'})

    def test_no_callback_kwargs_for_action(self):
        self.assertOptionError("callback_kwargs supplied for non-callback "
                               "option",
                               ["-b"], {'action': 'store',
                                        'callback_kwargs': 'foo'})

class TestOptionParser(BaseTest):
    def setUp(self):
        self.parser = OptionParser()
        self.parser.add_option("-v", "--verbose", "-n", "--noisy",
                          action="store_true", dest="verbose")
        self.parser.add_option("-q", "--quiet", "--silent",
                          action="store_false", dest="verbose")

    def test_add_option_no_Option(self):
        self.assertTypeError(self.parser.add_option,
                             "not an Option instance: None", None)

    def test_add_option_invalid_arguments(self):
        self.assertTypeError(self.parser.add_option,
                             "invalid arguments", None, None)

    def test_get_option(self):
        opt1 = self.parser.get_option("-v")
        self.assert_(isinstance(opt1, Option))
        self.assertEqual(opt1._short_opts, ["-v", "-n"])
        self.assertEqual(opt1._long_opts, ["--verbose", "--noisy"])
        self.assertEqual(opt1.action, "store_true")
        self.assertEqual(opt1.dest, "verbose")

    def test_get_option_equals(self):
        opt1 = self.parser.get_option("-v")
        opt2 = self.parser.get_option("--verbose")
        opt3 = self.parser.get_option("-n")
        opt4 = self.parser.get_option("--noisy")
        self.assert_(opt1 is opt2 is opt3 is opt4)

    def test_has_option(self):
        self.assert_(self.parser.has_option("-v"))
        self.assert_(self.parser.has_option("--verbose"))

    def assert_removed(self):
        self.assert_(self.parser.get_option("-v") is None)
        self.assert_(self.parser.get_option("--verbose") is None)
        self.assert_(self.parser.get_option("-n") is None)
        self.assert_(self.parser.get_option("--noisy") is None)

        self.failIf(self.parser.has_option("-v"))
        self.failIf(self.parser.has_option("--verbose"))
        self.failIf(self.parser.has_option("-n"))
        self.failIf(self.parser.has_option("--noisy"))

        self.assert_(self.parser.has_option("-q"))
        self.assert_(self.parser.has_option("--silent"))

    def test_remove_short_opt(self):
        self.parser.remove_option("-n")
        self.assert_removed()

    def test_remove_long_opt(self):
        self.parser.remove_option("--verbose")
        self.assert_removed()

    def test_remove_nonexistent(self):
        self.assertRaises(self.parser.remove_option, ValueError,
                          "no such option 'foo'", funcargs=['foo'])

# -- Test parser.parse_args() ------------------------------------------

class TestStandard(BaseTest):
    def setUp(self):
        options = [make_option("-a", type="string"),
                   make_option("-b", "--boo", type="int", dest='boo'),
                   make_option("--foo", action="append")]

        self.parser = OptionParser(usage=SUPPRESS_USAGE, option_list=options)

    def test_required_value(self):
        self.assertParseFail(["-a"], "-a option requires a value")

    def test_invalid_integer(self):
        self.assertParseFail(["-b", "5x"],
                             "option -b: invalid integer value: '5x'")

    def test_no_such_option(self):
        self.assertParseFail(["--boo13"], "no such option: --boo13")

    def test_long_invalid_integer(self):
        self.assertParseFail(["--boo=x5"],
                             "option --boo: invalid integer value: 'x5'")

    def test_empty(self):
        self.assertParseOK([], {'a': None, 'boo': None, 'foo': None}, [])

    def test_shortopt_empty_longopt_append(self):
        self.assertParseOK(["-a", "", "--foo=blah", "--foo="],
                           {'a': "", 'boo': None, 'foo': ["blah", ""]},
                           [])

    def test_long_option_append(self):
        self.assertParseOK(["--foo", "bar", "--foo", "", "--foo=x"],
                           {'a': None,
                            'boo': None,
                            'foo': ["bar", "", "x"]},
                           [])

    def test_option_argument_joined(self):
        self.assertParseOK(["-abc"],
                           {'a': "bc", 'boo': None, 'foo': None},
                           [])

    def test_option_argument_split(self):
        self.assertParseOK(["-a", "34"],
                           {'a': "34", 'boo': None, 'foo': None},
                           [])

    def test_option_argument_joined_integer(self):
        self.assertParseOK(["-b34"],
                           {'a': None, 'boo': 34, 'foo': None},
                           [])

    def test_option_argument_split_negative_integer(self):
        self.assertParseOK(["-b", "-5"],
                           {'a': None, 'boo': -5, 'foo': None},
                           [])

    def test_long_option_argument_joined(self):
        self.assertParseOK(["--boo=13"],
                           {'a': None, 'boo': 13, 'foo': None},
                           [])

    def test_long_option_argument_split(self):
        self.assertParseOK(["--boo", "111"],
                           {'a': None, 'boo': 111, 'foo': None},
                           [])

    def test_long_option_short_option(self):
        self.assertParseOK(["--foo=bar", "-axyz"],
                           {'a': 'xyz', 'boo': None, 'foo': ["bar"]},
                           [])

    def test_abbrev_long_option(self):
        self.assertParseOK(["--f=bar", "-axyz"],
                           {'a': 'xyz', 'boo': None, 'foo': ["bar"]},
                           [])

    def test_defaults(self):
        (options, args) = self.parser.parse_args([])
        defaults = self.parser.get_default_values()
        self.assertEqual(vars(defaults), vars(options))

    def test_ambiguous_option(self):
        self.parser.add_option("--foz", action="store",
                               type="string", dest="foo")
        possibilities = ", ".join({"--foz": None, "--foo": None}.keys())
        self.assertParseFail(["--f=bar"],
                             "ambiguous option: --f (%s?)" % possibilities)


    def test_short_and_long_option_split(self):
        self.assertParseOK(["-a", "xyz", "--foo", "bar"],
                           {'a': 'xyz', 'boo': None, 'foo': ["bar"]},
                           []),

    def test_short_option_split_long_option_append(self):
        self.assertParseOK(["--foo=bar", "-b", "123", "--foo", "baz"],
                           {'a': None, 'boo': 123, 'foo': ["bar", "baz"]},
                           [])

    def test_short_option_split_one_positional_arg(self):
        self.assertParseOK(["-a", "foo", "bar"],
                           {'a': "foo", 'boo': None, 'foo': None},
                           ["bar"]),

    def test_short_option_consumes_separator(self):
        self.assertParseOK(["-a", "--", "foo", "bar"],
                           {'a': "--", 'boo': None, 'foo': None},
                           ["foo", "bar"]),

    def test_short_option_joined_and_separator(self):
        self.assertParseOK(["-ab", "--", "--foo", "bar"],
                           {'a': "b", 'boo': None, 'foo': None},
                           ["--foo", "bar"]),

    def test_invalid_option_becomes_positional_arg(self):
        self.assertParseOK(["-ab", "-", "--foo", "bar"],
                           {'a': "b", 'boo': None, 'foo': ["bar"]},
                           ["-"])

    def test_no_append_versus_append(self):
        self.assertParseOK(["-b3", "-b", "5", "--foo=bar", "--foo", "baz"],
                           {'a': None, 'boo': 5, 'foo': ["bar", "baz"]},
                           [])

    def test_option_consumes_optionlike_string(self):
        self.assertParseOK(["-a", "-b3"],
                           {'a': "-b3", 'boo': None, 'foo': None},
                           [])

class TestBool(BaseTest):
    def setUp(self):
        options = [make_option("-v",
                               "--verbose",
                               action="store_true",
                               dest="verbose",
                               default=''),
                   make_option("-q",
                               "--quiet",
                               action="store_false",
                               dest="verbose")]
        self.parser = OptionParser(option_list = options)

    def test_bool_default(self):
        self.assertParseOK([],
                           {'verbose': ''},
                           [])

    def test_bool_false(self):
        (options, args) = self.assertParseOK(["-q"],
                                             {'verbose': 0},
                                             [])
        if hasattr(__builtins__, 'False'):
            self.failUnless(options.verbose is False)

    def test_bool_true(self):
        (options, args) = self.assertParseOK(["-v"],
                                             {'verbose': 1},
                                             [])
        if hasattr(__builtins__, 'True'):
            self.failUnless(options.verbose is True)

    def test_bool_flicker_on_and_off(self):
        self.assertParseOK(["-qvq", "-q", "-v"],
                           {'verbose': 1},
                           [])

class TestChoice(BaseTest):
    def setUp(self):
        self.parser = OptionParser(usage=SUPPRESS_USAGE)
        self.parser.add_option("-c", action="store", type="choice",
                               dest="choice", choices=["one", "two", "three"])

    def test_valid_choice(self):
        self.assertParseOK(["-c", "one", "xyz"],
                           {'choice': 'one'},
                           ["xyz"])

    def test_invalid_choice(self):
        self.assertParseFail(["-c", "four", "abc"],
                             "option -c: invalid choice: 'four' "
                             "(choose from 'one', 'two', 'three')")

    def test_add_choice_option(self):
        self.parser.add_option("-d", "--default",
                               choices=["four", "five", "six"])
        opt = self.parser.get_option("-d")
        self.assertEqual(opt.type, "choice")
        self.assertEqual(opt.action, "store")

class TestCount(BaseTest):
    def setUp(self):
        self.parser = OptionParser(usage=SUPPRESS_USAGE)
        self.v_opt = make_option("-v", action="count", dest="verbose")
        self.parser.add_option(self.v_opt)
        self.parser.add_option("--verbose", type="int", dest="verbose")
        self.parser.add_option("-q", "--quiet",
                               action="store_const", dest="verbose", const=0)

    def test_empty(self):
        self.assertParseOK([], {'verbose': None}, [])

    def test_count_one(self):
        self.assertParseOK(["-v"], {'verbose': 1}, [])

    def test_count_three(self):
        self.assertParseOK(["-vvv"], {'verbose': 3}, [])

    def test_count_three_apart(self):
        self.assertParseOK(["-v", "-v", "-v"], {'verbose': 3}, [])

    def test_count_override_amount(self):
        self.assertParseOK(["-vvv", "--verbose=2"], {'verbose': 2}, [])

    def test_count_override_quiet(self):
        self.assertParseOK(["-vvv", "--verbose=2", "-q"], {'verbose': 0}, [])

    def test_count_overriding(self):
        self.assertParseOK(["-vvv", "--verbose=2", "-q", "-v"],
                           {'verbose': 1}, [])

    def test_count_interspersed_args(self):
        self.assertParseOK(["--quiet", "3", "-v"],
                           {'verbose': 1},
                           ["3"])

    def test_count_no_interspersed_args(self):
        self.parser.disable_interspersed_args()
        self.assertParseOK(["--quiet", "3", "-v"],
                           {'verbose': 0},
                           ["3", "-v"])

    def test_count_no_such_option(self):
        self.assertParseFail(["-q3", "-v"], "no such option: -3")

    def test_count_option_no_value(self):
        self.assertParseFail(["--quiet=3", "-v"],
                             "--quiet option does not take a value")

    def test_count_with_default(self):
        self.parser.set_default('verbose', 0)
        self.assertParseOK([], {'verbose':0}, [])

    def test_count_overriding_default(self):
        self.parser.set_default('verbose', 0)
        self.assertParseOK(["-vvv", "--verbose=2", "-q", "-v"],
                           {'verbose': 1}, [])

class TestNArgs(BaseTest):
    def setUp(self):
        self.parser = OptionParser(usage=SUPPRESS_USAGE)
        self.parser.add_option("-p", "--point",
                               action="store", nargs=3, type="float", dest="point")

    def test_nargs_with_positional_args(self):
        self.assertParseOK(["foo", "-p", "1", "2.5", "-4.3", "xyz"],
                           {'point': (1.0, 2.5, -4.3)},
                           ["foo", "xyz"])

    def test_nargs_long_opt(self):
        self.assertParseOK(["--point", "-1", "2.5", "-0", "xyz"],
                           {'point': (-1.0, 2.5, -0.0)},
                           ["xyz"])

    def test_nargs_invalid_float_value(self):
        self.assertParseFail(["-p", "1.0", "2x", "3.5"],
                             "option -p: "
                             "invalid floating-point value: '2x'")

    def test_nargs_required_values(self):
        self.assertParseFail(["--point", "1.0", "3.5"],
                             "--point option requires 3 values")

class TestNArgsAppend(BaseTest):
    def setUp(self):
        self.parser = OptionParser(usage=SUPPRESS_USAGE)
        self.parser.add_option("-p", "--point", action="store", nargs=3,
                               type="float", dest="point")
        self.parser.add_option("-f", "--foo", action="append", nargs=2,
                               type="int", dest="foo")

    def test_nargs_append(self):
        self.assertParseOK(["-f", "4", "-3", "blah", "--foo", "1", "666"],
                           {'point': None, 'foo': [(4, -3), (1, 666)]},
                           ["blah"])

    def test_nargs_append_required_values(self):
        self.assertParseFail(["-f4,3"],
                             "-f option requires 2 values")

    def test_nargs_append_simple(self):
        self.assertParseOK(["--foo=3", "4"],
                           {'point': None, 'foo':[(3, 4)]},
                           [])

class TestVersion(BaseTest):
    def test_version(self):
        oldargv = sys.argv[0]
        sys.argv[0] = os.path.join(os.curdir, "foo", "bar")
        self.parser = OptionParser(usage=SUPPRESS_USAGE, version="%prog 0.1")
        self.assertStdoutEquals(["--version"], "bar 0.1\n")
        sys.argv[0] = oldargv

    def test_version_with_prog_keyword(self):
        oldargv = sys.argv[0]
        sys.argv[0] = "./foo/bar"
        self.parser = OptionParser(usage=SUPPRESS_USAGE, version="%prog 0.1",
                                   prog="splat")
        self.assertStdoutEquals(["--version"], "splat 0.1\n")
        sys.argv[0] = oldargv

    def test_version_with_prog_attribute(self):
        oldargv = sys.argv[0]
        sys.argv[0] = "./foo/bar"
        self.parser = OptionParser(usage=SUPPRESS_USAGE, version="%prog 0.1")
        self.parser.prog = "splat"
        self.assertStdoutEquals(["--version"], "splat 0.1\n")
        sys.argv[0] = oldargv

    def test_no_version(self):
        self.parser = OptionParser(usage=SUPPRESS_USAGE)
        self.assertParseFail(["--version"],
                             "no such option: --version")

# -- Test conflicting default values and parser.parse_args() -----------

class TestConflictingDefaults(BaseTest):
    """Conflicting default values: the last one should win."""
    def setUp(self):
        self.parser = OptionParser(option_list=[
            make_option("-v", action="store_true", dest="verbose", default=1)])

    def test_conflict_default(self):
        self.parser.add_option("-q", action="store_false", dest="verbose",
                               default=0)
        self.assertParseOK([], {'verbose': 0}, [])

    def test_conflict_default_none(self):
        self.parser.add_option("-q", action="store_false", dest="verbose",
                               default=None)
        self.assertParseOK([], {'verbose': None}, [])

class TestOptionGroup(BaseTest):
    def setUp(self):
        self.parser = OptionParser(usage=SUPPRESS_USAGE)

    def test_option_group_create_instance(self):
        group = OptionGroup(self.parser, "Spam")
        self.parser.add_option_group(group)
        group.add_option("--spam", action="store_true",
                         help="spam spam spam spam")
        self.assertParseOK(["--spam"], {'spam': 1}, [])

    def test_add_group_no_group(self):
        self.assertTypeError(self.parser.add_option_group,
                             "not an OptionGroup instance: None", None)

    def test_add_group_invalid_arguments(self):
        self.assertTypeError(self.parser.add_option_group,
                             "invalid arguments", None, None)

    def test_add_group_wrong_parser(self):
        group = OptionGroup(self.parser, "Spam")
        group.parser = OptionParser()
        self.assertRaises(self.parser.add_option_group, ValueError,
                          "invalid OptionGroup (wrong parser)", funcargs=[group])

    def test_group_manipulate(self):
        group = self.parser.add_option_group("Group 2",
                                             description="Some more options")
        group.set_title("Bacon")
        group.add_option("--bacon", type="int")
        self.assert_(self.parser.get_option_group("--bacon"), group)

# -- Test extending and parser.parse_args() ----------------------------

class TestExtendAddTypes(BaseTest):
    def setUp(self):
        self.parser = OptionParser(usage=SUPPRESS_USAGE,
                                   option_class=self.MyOption)
        self.parser.add_option("-a", None, type="string", dest="a")
        self.parser.add_option("-f", "--file", type="file", dest="file")

    class MyOption (Option):
        def check_file (option, opt, value):
            if not os.path.exists(value):
                raise OptionValueError("%s: file does not exist" % value)
            elif not os.path.isfile(value):
                raise OptionValueError("%s: not a regular file" % value)
            return value

        TYPES = Option.TYPES + ("file",)
        TYPE_CHECKER = copy.copy(Option.TYPE_CHECKER)
        TYPE_CHECKER["file"] = check_file

    def test_extend_file(self):
        open(test_support.TESTFN, "w").close()
        self.assertParseOK(["--file", test_support.TESTFN, "-afoo"],
                           {'file': test_support.TESTFN, 'a': 'foo'},
                           [])

        os.unlink(test_support.TESTFN)

    def test_extend_file_nonexistent(self):
        self.assertParseFail(["--file", test_support.TESTFN, "-afoo"],
                             "%s: file does not exist" %
                             test_support.TESTFN)

    def test_file_irregular(self):
        os.mkdir(test_support.TESTFN)
        self.assertParseFail(["--file", test_support.TESTFN, "-afoo"],
                             "%s: not a regular file" %
                             test_support.TESTFN)
        os.rmdir(test_support.TESTFN)

class TestExtendAddActions(BaseTest):
    def setUp(self):
        options = [self.MyOption("-a", "--apple", action="extend",
                                 type="string", dest="apple")]
        self.parser = OptionParser(option_list=options)

    class MyOption (Option):
        ACTIONS = Option.ACTIONS + ("extend",)
        STORE_ACTIONS = Option.STORE_ACTIONS + ("extend",)
        TYPED_ACTIONS = Option.TYPED_ACTIONS + ("extend",)

        def take_action (self, action, dest, opt, value, values, parser):
            if action == "extend":
                lvalue = value.split(",")
                values.ensure_value(dest, []).extend(lvalue)
            else:
                Option.take_action(self, action, dest, opt, parser, value,
                                   values)

    def test_extend_add_action(self):
        self.assertParseOK(["-afoo,bar", "--apple=blah"],
                           {'apple': ["foo", "bar", "blah"]},
                           [])

    def test_extend_add_action_normal(self):
        self.assertParseOK(["-a", "foo", "-abar", "--apple=x,y"],
                           {'apple': ["foo", "bar", "x", "y"]},
                           [])

# -- Test callbacks and parser.parse_args() ----------------------------

class TestCallback(BaseTest):
    def setUp(self):
        options = [make_option("-x",
                               None,
                               action="callback",
                               callback=self.process_opt),
                   make_option("-f",
                               "--file",
                               action="callback",
                               callback=self.process_opt,
                               type="string",
                               dest="filename")]
        self.parser = OptionParser(option_list=options)

    def process_opt(self, option, opt, value, parser_):
        if opt == "-x":
            self.assertEqual(option._short_opts, ["-x"])
            self.assertEqual(option._long_opts, [])
            self.assert_(parser_ is self.parser)
            self.assert_(value is None)
            self.assertEqual(vars(parser_.values), {'filename': None})

            parser_.values.x = 42
        elif opt == "--file":
            self.assertEqual(option._short_opts, ["-f"])
            self.assertEqual(option._long_opts, ["--file"])
            self.assert_(parser_ is self.parser)
            self.assertEqual(value, "foo")
            self.assertEqual(vars(parser_.values), {'filename': None, 'x': 42})

            setattr(parser_.values, option.dest, value)
        else:
            self.fail("Unknown option %r in process_opt." % opt)

    def test_callback(self):
        self.assertParseOK(["-x", "--file=foo"],
                           {'filename': "foo", 'x': 42},
                           [])

class TestCallBackExtraArgs(BaseTest):
    def setUp(self):
        options = [make_option("-p", "--point", action="callback",
                               callback=self.process_tuple,
                               callback_args=(3, int), type="string",
                               dest="points", default=[])]
        self.parser = OptionParser(option_list=options)

    def process_tuple (self, option, opt, value, parser_, len, type):
        self.assertEqual(len, 3)
        self.assert_(type is int)

        if opt == "-p":
            self.assertEqual(value, "1,2,3")
        elif opt == "--point":
            self.assertEqual(value, "4,5,6")

        value = tuple(map(type, value.split(",")))
        getattr(parser_.values, option.dest).append(value)

    def test_callback_extra_args(self):
        self.assertParseOK(["-p1,2,3", "--point", "4,5,6"],
                           {'points': [(1,2,3), (4,5,6)]},
                           [])

class TestCallBackMeddleArgs(BaseTest):
    def setUp(self):
        options = [make_option(str(x), action="callback",
                               callback=self.process_n, dest='things')
                   for x in range(-1, -6, -1)]
        self.parser = OptionParser(option_list=options)

    # Callback that meddles in rargs, largs
    def process_n (self, option, opt, value, parser_):
        # option is -3, -5, etc.
        nargs = int(opt[1:])
        rargs = parser_.rargs
        if len(rargs) < nargs:
            self.fail("Expected %d arguments for %s option." % (nargs, opt))
        dest = parser_.values.ensure_value(option.dest, [])
        dest.append(tuple(rargs[0:nargs]))
        parser_.largs.append(nargs)
        del rargs[0:nargs]

    def test_callback_meddle_args(self):
        self.assertParseOK(["-1", "foo", "-3", "bar", "baz", "qux"],
                           {'things': [("foo",), ("bar", "baz", "qux")]},
                           [1, 3])

    def test_callback_meddle_args_separator(self):
        self.assertParseOK(["-2", "foo", "--"],
                           {'things': [('foo', '--')]},
                           [2])

class TestCallBackManyArgs(BaseTest):
    def setUp(self):
        options = [make_option("-a", "--apple", action="callback", nargs=2,
                               callback=self.process_many, type="string"),
                   make_option("-b", "--bob", action="callback", nargs=3,
                               callback=self.process_many, type="int")]
        self.parser = OptionParser(option_list=options)

    def process_many (self, option, opt, value, parser_):
        if opt == "-a":
            self.assertEqual(value, ("foo", "bar"))
        elif opt == "--apple":
            self.assertEqual(value, ("ding", "dong"))
        elif opt == "-b":
            self.assertEqual(value, (1, 2, 3))
        elif opt == "--bob":
            self.assertEqual(value, (-666, 42, 0))

    def test_many_args(self):
        self.assertParseOK(["-a", "foo", "bar", "--apple", "ding", "dong",
                            "-b", "1", "2", "3", "--bob", "-666", "42",
                            "0"],
                           {},
                           [])

class TestCallBackCheckAbbrev(BaseTest):
    def setUp(self):
        self.parser = OptionParser()
        self.parser.add_option("--foo-bar", action="callback",
                               callback=self.check_abbrev)

    def check_abbrev (self, option, opt, value, parser):
        self.assertEqual(opt, "--foo-bar")

    def test_abbrev_callback_expansion(self):
        self.assertParseOK(["--foo"], {}, [])

class TestCallBackVarArgs(BaseTest):
    def setUp(self):
        options = [make_option("-a", type="int", nargs=2, dest="a"),
                   make_option("-b", action="store_true", dest="b"),
                   make_option("-c", "--callback", action="callback",
                               callback=self.variable_args, dest="c")]
        self.parser = OptionParser(usage=SUPPRESS_USAGE, option_list=options)

    def variable_args (self, option, opt, value, parser):
        self.assert_(value is None)
        done = 0
        value = []
        rargs = parser.rargs
        while rargs:
            arg = rargs[0]
            if ((arg[:2] == "--" and len(arg) > 2) or
                (arg[:1] == "-" and len(arg) > 1 and arg[1] != "-")):
                break
            else:
                value.append(arg)
                del rargs[0]
        setattr(parser.values, option.dest, value)

    def test_variable_args(self):
        self.assertParseOK(["-a3", "-5", "--callback", "foo", "bar"],
                           {'a': (3, -5), 'b': None, 'c': ["foo", "bar"]},
                           [])

    def test_consume_separator_stop_at_option(self):
        self.assertParseOK(["-c", "37", "--", "xxx", "-b", "hello"],
                           {'a': None,
                            'b': True,
                            'c': ["37", "--", "xxx"]},
                           ["hello"])

    def test_positional_arg_and_variable_args(self):
        self.assertParseOK(["hello", "-c", "foo", "-", "bar"],
                           {'a': None,
                            'b': None,
                            'c':["foo", "-", "bar"]},
                           ["hello"])

    def test_stop_at_option(self):
        self.assertParseOK(["-c", "foo", "-b"],
                           {'a': None, 'b': True, 'c': ["foo"]},
                           [])

    def test_stop_at_invalid_option(self):
        self.assertParseFail(["-c", "3", "-5", "-a"], "no such option: -5")


# -- Test conflict handling and parser.parse_args() --------------------

class ConflictBase(BaseTest):
    def setUp(self):
        options = [make_option("-v", "--verbose", action="count",
                               dest="verbose", help="increment verbosity")]
        self.parser = OptionParser(usage=SUPPRESS_USAGE, option_list=options)

    def show_version (self, option, opt, value, parser):
        parser.values.show_version = 1

class TestConflict(ConflictBase):
    """Use the default conflict resolution for Optik 1.2: error."""
    def assert_conflict_error(self, func):
        err = self.assertRaises(func, OptionConflictError,
                                "option -v/--version: conflicting option "
                                "string(s): -v",
                                funcargs=["-v", "--version"],
                                funckwargs={'action':"callback",
                                            'callback':self.show_version,
                                            'help':"show version"})

        self.assertEqual(err.msg, "conflicting option string(s): -v")
        self.assertEqual(err.option_id, "-v/--version")

    def test_conflict_error(self):
        self.assert_conflict_error(self.parser.add_option)

    def test_conflict_error_group(self):
        group = OptionGroup(self.parser, "Group 1")
        self.assert_conflict_error(group.add_option)

    def test_no_such_conflict_handler(self):
        self.assertRaises(self.parser.set_conflict_handler, ValueError,
                          "invalid conflict_resolution value 'foo'",
                          funcargs=['foo'])


class TestConflictIgnore(ConflictBase):
    """Test the old (Optik <= 1.1 behaviour) -- arguably broken, but
    still available so should be tested.
    """

    def setUp(self):
        ConflictBase.setUp(self)
        self.parser.set_conflict_handler("ignore")
        self.parser.add_option("-v", "--version", action="callback",
                          callback=self.show_version, help="show version")

    def test_conflict_ignore(self):
        v_opt = self.parser.get_option("-v")
        verbose_opt = self.parser.get_option("--verbose")
        version_opt = self.parser.get_option("--version")

        self.assert_(v_opt is version_opt)
        self.assert_(v_opt is not verbose_opt)
        self.assertEqual(v_opt._long_opts, ["--version"])
        self.assertEqual(version_opt._short_opts, ["-v"])
        self.assertEqual(verbose_opt._short_opts, ["-v"])

    def test_conflict_ignore_help(self):
        self.assertStdoutEquals(["-h"], """\
options:
  -v, --verbose  increment verbosity
  -h, --help     show this help message and exit
  -v, --version  show version
""")

    def test_conflict_ignore_short_opt(self):
        self.assertParseOK(["-v"],
                           {'show_version': 1, 'verbose': None},
                           [])

class TestConflictResolve(ConflictBase):
    def setUp(self):
        ConflictBase.setUp(self)
        self.parser.set_conflict_handler("resolve")
        self.parser.add_option("-v", "--version", action="callback",
                               callback=self.show_version, help="show version")

    def test_conflict_resolve(self):
        v_opt = self.parser.get_option("-v")
        verbose_opt = self.parser.get_option("--verbose")
        version_opt = self.parser.get_option("--version")

        self.assert_(v_opt is version_opt)
        self.assert_(v_opt is not verbose_opt)
        self.assertEqual(v_opt._long_opts, ["--version"])
        self.assertEqual(version_opt._short_opts, ["-v"])
        self.assertEqual(version_opt._long_opts, ["--version"])
        self.assertEqual(verbose_opt._short_opts, [])
        self.assertEqual(verbose_opt._long_opts, ["--verbose"])

    def test_conflict_resolve_help(self):
        self.assertStdoutEquals(["-h"], """\
options:
  --verbose      increment verbosity
  -h, --help     show this help message and exit
  -v, --version  show version
""")

    def test_conflict_resolve_short_opt(self):
        self.assertParseOK(["-v"],
                           {'verbose': None, 'show_version': 1},
                           [])

    def test_conflict_resolve_long_opt(self):
        self.assertParseOK(["--verbose"],
                           {'verbose': 1},
                           [])

    def test_conflict_resolve_long_opts(self):
        self.assertParseOK(["--verbose", "--version"],
                           {'verbose': 1, 'show_version': 1},
                           [])

class TestConflictOverride(BaseTest):
    def setUp(self):
        self.parser = OptionParser(usage=SUPPRESS_USAGE)
        self.parser.set_conflict_handler("resolve")
        self.parser.add_option("-n", "--dry-run",
                               action="store_true", dest="dry_run",
                               help="don't do anything")
        self.parser.add_option("--dry-run", "-n",
                               action="store_const", const=42, dest="dry_run",
                               help="dry run mode")

    def test_conflict_override_opts(self):
        opt = self.parser.get_option("--dry-run")
        self.assertEqual(opt._short_opts, ["-n"])
        self.assertEqual(opt._long_opts, ["--dry-run"])

    def test_conflict_override_help(self):
        self.assertStdoutEquals(["-h"], """\
options:
  -h, --help     show this help message and exit
  -n, --dry-run  dry run mode
""")

    def test_conflict_override_args(self):
        self.assertParseOK(["-n"],
                           {'dry_run': 42},
                           [])

# -- Other testing. ----------------------------------------------------

class TestHelp(BaseTest):
    def setUp(self):
        options = [
            make_option("-a", type="string", dest='a',
                        metavar="APPLE", help="throw APPLEs at basket"),
            make_option("-b", "--boo", type="int", dest='boo',
                        metavar="NUM",
                        help=
                        "shout \"boo!\" NUM times (in order to frighten away "
                        "all the evil spirits that cause trouble and mayhem)"),
            make_option("--foo", action="append", type="string", dest='foo',
                        help="store FOO in the foo list for later fooing"),
            ]

        usage = "%prog [options]"
        self.parser = OptionParser(usage=usage, option_list=options)

    def assertHelpEquals(self, expected_output):
        # This trick is used to make optparse believe bar.py is being executed.
        oldargv = sys.argv[0]
        sys.argv[0] = os.path.join(os.curdir, "foo", "bar.py")

        self.assertStdoutEquals(["-h"], expected_output)

        sys.argv[0] = oldargv

    def test_help(self):
        self.assertHelpEquals("""\
usage: bar.py [options]

options:
  -aAPPLE           throw APPLEs at basket
  -bNUM, --boo=NUM  shout "boo!" NUM times (in order to frighten away all
                    the evil spirits that cause trouble and mayhem)
  --foo=FOO         store FOO in the foo list for later fooing
  -h, --help        show this help message and exit
""")

    def test_help_old_usage(self):
        self.parser.set_usage("usage: %prog [options]")
        self.assertHelpEquals("""\
usage: bar.py [options]

options:
  -aAPPLE           throw APPLEs at basket
  -bNUM, --boo=NUM  shout "boo!" NUM times (in order to frighten away all
                    the evil spirits that cause trouble and mayhem)
  --foo=FOO         store FOO in the foo list for later fooing
  -h, --help        show this help message and exit
""")

    def test_help_long_opts_first(self):
        self.parser.formatter.short_first = 0
        self.assertHelpEquals("""\
usage: bar.py [options]

options:
  -aAPPLE           throw APPLEs at basket
  --boo=NUM, -bNUM  shout "boo!" NUM times (in order to frighten away all
                    the evil spirits that cause trouble and mayhem)
  --foo=FOO         store FOO in the foo list for later fooing
  --help, -h        show this help message and exit
""")

    def test_help_title_formatter(self):
        self.parser.formatter = TitledHelpFormatter()
        self.assertHelpEquals("""\
Usage
=====
  bar.py [options]

options
=======
-aAPPLE           throw APPLEs at basket
--boo=NUM, -bNUM  shout "boo!" NUM times (in order to frighten away all
                  the evil spirits that cause trouble and mayhem)
--foo=FOO         store FOO in the foo list for later fooing
--help, -h        show this help message and exit
""")

    def test_help_description_groups(self):
        self.parser.set_description(
            "This is the program description.  This program has "
            "an option group as well as single options.")

        group = OptionGroup(
            self.parser, "Dangerous Options",
            "Caution: use of these options is at your own risk.  "
            "It is believed that some of them bite.")
        group.add_option("-g", action="store_true", help="Group option.")
        self.parser.add_option_group(group)

        self.assertHelpEquals("""\
usage: bar.py [options]

This is the program description.  This program has an option group as well as
single options.
options:
  -aAPPLE           throw APPLEs at basket
  -bNUM, --boo=NUM  shout "boo!" NUM times (in order to frighten away all
                    the evil spirits that cause trouble and mayhem)
  --foo=FOO         store FOO in the foo list for later fooing
  -h, --help        show this help message and exit

  Dangerous Options:
    Caution: use of these options is at your own risk.  It is believed that
    some of them bite.
    -g              Group option.
""")

class TestMatchAbbrev(BaseTest):
    def test_match_abbrev(self):
        self.assertEqual(_match_abbrev("--f",
                                       {"--foz": None,
                                        "--foo": None,
                                        "--fie": None,
                                        "--f": None}),
                         "--f")

    def test_match_abbrev_error(self):
        s = "--f"
        wordmap = {"--foz": None, "--foo": None, "--fie": None}
        possibilities = ", ".join(wordmap.keys())
        self.assertRaises(_match_abbrev, BadOptionError,
                          "ambiguous option: --f (%s?)" % possibilities,
                          funcargs=[s, wordmap])

def test_main():
    mod = sys.modules[__name__]
    test_support.run_unittest(
        *[getattr(mod, name) for name in dir(mod) if name.startswith('Test')]
    )

if __name__ == '__main__':
    unittest.main()
