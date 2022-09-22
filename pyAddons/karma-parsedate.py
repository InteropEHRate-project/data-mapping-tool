import sys, os

sys.path.append(os.path.expandvars("$HOME/karma/python"))

from dateutil.parser import parse

def toISODate(value):
    d = parse(value)
    return d.replace(microsecond=0).isoformat()
