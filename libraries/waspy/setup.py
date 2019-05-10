"""
Waspy - WASDI Python Library

Created on 11 Jun 2018

@author: p.campanella - FadeOut Software
"""
import setuptools

with open("README.md", "r") as fh:
    long_description = fh.read()

setuptools.setup(
    name="wasdi",
    version="0.1.1",
    author="FadeOut Software",
    author_email="info@fadeout.biz",
    description="The Wasdi Python library",
    long_description=long_description,
    long_description_content_type="text/markdown",
    url="http://www.wasdi.net",
    packages=setuptools.find_packages(),
    classifiers=(
        "Programming Language :: Python :: 3",
        "License :: OSI Approved :: MIT License",
        "Operating System :: OS Independent",
    ),
)