---
section: 32
x-masysma-name: uni/dke/u08/6
title: "UNI DKE U08/6: NormalizeEncodings"
date: 2020/01/01 00:00:00
lang: en-US
author: ["Linux-Fan, Ma_Sys.ma (Ma_Sys.ma@web.de)"]
keywords: ["uni", "dke", "encoding"]
x-masysma-version: 1.0.0
x-masysma-repository: https://www.github.com/m7a/bp-uni-dke-u08-6-normalize-encodings
x-masysma-owned: 1
x-masysma-copyright: |
  NormalizeEncodings 1.0.0.0, Copyright (c) 2016 Ma_Sys.ma.
  For further info send an e-mail to Ma_Sys.ma@web.de.
---
Introduction
============

This program demonstrates some of the issues when working with heterogeneous
text files which have different encodings, line endings and supply a set of
fixed fields sometimes in a slightly different writing.

This was designed as a solution to an exercise presented at the TU Darmstadt's
DKE course in summer 2016.

Compilation
===========

If any make-tool is available, you can compile this via

	$ make

(If you want a jar-file, use `$ make jar`).

Otherwise invoke the Java compiler directly:

	$ javac *.java

This program requires Java 7 or higher (it has only been tested with Java 7).

Usage
=====

To make use of the program, you need to create a set of input text-files
and make a ZIP-archive from them.

For instance, you can use the files supplied with the program by invoking
(provided 7-Zip is in your `$PATH`)

	$ 7z -tzip a test.zip encoding_tests

Then invoke the program as follows

	$ java NormalizeEncodings test.zip result.txt

See [how_to_start_java_programs(37)][../37/how_to_start_java_programs.xhtml] if
you need more help with getting this to work, `NormalizedEncodings` is the main
class here.

The result of the program execution will then be written to a new file called
`result.txt` (existing files are overwritten without notice).

Effects
=======

What is this program all about?

Basically, the exercise was to write an application which could read text files
gathered from multiple operating systems and even more different users which
all were in the same language (German) but different encodings and line-endings
etc. The success of reading and processing all these files is demonstrated
by writing all the files' contents to a result file which is encoded in UTF-8
and consistently uses UNIX line-endings.

In this concrete exercise, all data has to be in the text format “defined”
by `encoding_tests/vorlage.txt` which specifies six sections introduced
with `>` having fixed names. Contents for these sections is written below
that sections one entry per line. As this definition is rather informal and it
was initially not even suggested that the data from these files would probably
be processed automatically, the input files supplied are all in slightly
different variations from `vorlage.txt` -- some leave out part of the
section titles, spell them differently, change punctuation, add or remove
newlines, introduce bullet points, use different line-endings etc. Most of these
differences are normalized by this solution to result in data in a consistent
format specified in a later exercise.

Issues
======

Encoding is about the second-worst of these issues (the worst being -- of
course -- handling dates and times correctly). Therefore, this solution is --
as just about every solution to such a task -- incomplete.

Be aware that the incompleteness of this concrete implementation is
especially the restriction to processing German (and probably English) texts
only. Also, the list of detected encodings is very short: Unicode and Windows
encodings are recognized... nothing more.

Also, this program is slow: It reads all files sequentially into memory
and then looks for patterns to identify encodings in a very simple and
inefficient way. If you intend to introduce something similar to this
implementation in your application, be warned that it is likely not
to be good for perforance.
