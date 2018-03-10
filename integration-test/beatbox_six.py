"""Simplified "six" package for Beatbox and Python >= 2.7 or >= 3.3"""
# It is in a setarate module because some checkers can report that
# something is not a valid code in Python 2 or 3 respectively.

import sys

PY2 = sys.version_info[0] == 2
PY3 = sys.version_info[0] == 3

from io import BytesIO
if PY3:
    from builtins import range as xrange
    from io import StringIO
    from http import client as http_client
    from urllib.parse import urlparse
    text_type = str
else:
    from __builtin__ import xrange
    from StringIO import StringIO
    import httplib as http_client
    from urlparse import urlparse
    text_type = unicode


def python_2_unicode_compatible(klass):
    """
    A decorator that defines __unicode__ and __str__ methods under Python 2.
    Under Python 3 it does nothing.

    To support Python 2 and 3 with a single code base, define a __str__ method
    returning text and apply this decorator to the class.
    """
    if PY2:
        if '__str__' not in klass.__dict__:
            raise ValueError("@python_2_unicode_compatible cannot be applied "
                             "to %s because it doesn't define __str__()." %
                             klass.__name__)
        klass.__unicode__ = klass.__str__
        klass.__str__ = lambda self: self.__unicode__().encode('utf-8')
    return klass
