"""rBrowser package containing the modularised application code."""
import os
import rbrowser
from os.path import join
from .web_browser import NomadNetWebBrowser
target_path = os.environ["HOME"]
os.chdir(target_path)
__all__ = ["NomadNetWebBrowser"]
