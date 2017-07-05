# ublu
Ublu Midrange and Mainframe Life Cycle Extension Language<br>
Copyright (c) 2015, Absolute Performance, Inc. http://www.absolute-performance.com<br>
Copyright (c) 2017, Jack J. Woehr http://www.softwoehr.com<br>
All rights reserved.<br>
See file LICENSE for license information.

* <a href="#general">General information</a>
* <a href="#quickstart">Quickstart instructions</a>
* <a href="#goublu">Goublu console support for Ublu</a>
* <a href="#discussion">Discussion of Ublu</a>

<a name="general"></a>
## General information

Ublu is an interpretive language for remote systems programming of midrange or
mainframe hosts from a Java platform such as Linux, Mac, OpenBSD or Windows. It
also can run natively on IBM i ®, IBM z/OS USS ® or any other reasonable Java
platform.

I wrote Ublu because I wanted a language to run on OpenBSD/Mac/Linux/Windows to
perform ad-hoc process automation primarily on IBM i Series OS ®. I was
supporting consulting clients by writing individual utility programs using JTOpen
which I  have used since 1998 to control the AS/400. I decided to consolidate
the programs in a language, and the result is Ublu. Ublu is a work in progress,
as there is always more one could add. 

Additionally, Ublu can call Java directly allowing the user to extend the
language interpretively in nearly any direction desired.

Running Ublu directly on IBM i is especially useful for modelling processes
which you might later wish to code in straight Java. Or maybe you'll leave them
in Ublu. Whatever works!

Ublu is Open Source Software under the BSD-2 license.

The user's guide is [userdoc/ubluguide.html](http://www.softwoehr.com/softwoehr/oss/ubludoc/ubluguide.html)

The full reference is [userdoc/ubluref.html](http://www.softwoehr.com/softwoehr/oss/ubludoc/ubluref.html)

Here's an [example of Ublu code](http://www.softwoehr.com/softwoehr/oss/ubludoc/jobstuff-example.html)

The example is syntax-colored using a [jEdit](http://jedit.org) edit mode provided with Ublu.

The latest release version of Ublu is [version 1.1.7](https://github.com/jwoehr/ublu/releases/tag/v1.1.7).

The tip of the master branch calls itself 1.1.7+ as we head for 1.1.8

Ublu is distributed with the open source libraries it needs and their license
files which permit such distribution.

Ublu is already a stable and useful tool which has seen much use in the real
world. It is neither complete nor perfect, but what is in this world?

<a name="quickstart"></a>
## Quick start instructions

Download the release and <code>java -jar ublu.jar</code> to run Ublu.

Or clone, build and run:
<ul>
<li> Clone the Ublu Git repository https://github.com/jwoehr/ublu.git</li>
<li> Either load the project in <a href="http://www.netbeans.org">NetBeans</a>
     or cd to the top dir of the checkout and type <tt>ant</tt>.
<ul>
    <li> Of course you have <a href="http://ant.apache.org/">Apache Ant</a>
        installed, right?!</li>
</ul>
</li>
<li> <tt>./dist/ublu.jar</tt> and its necessary <tt>./dist/lib</tt> directory are the runtime system.</li>
<li> <tt>java -jar ublu.jar</tt> to run Ublu as a plain Java console application.</li>
</ul>

<a name="goublu"></a>
## Goublu console support for Ublu

Ublu's interpreter relies on Java's console support, which is very weak.
So I have coded [Goublu](https://github.com/jwoehr/goublu) in [Go language](https://golang.org/).

Goublu is a console front-end that provides an editable Ublu command line. The go command

`go get -u github.com/jwoehr/goublu`

will fetch the source to your `$GOPATH/src` directory. Use `go build main\goublu.go` to build Goublu for your architecture.

<a name="discussion"></a>

## Ublu in a Window
You can start Ublu in a window with the `-w [propsfilepath]` switch.
![Ublu in a Window](https://user-images.githubusercontent.com/4604036/27810879-ed42fa88-601c-11e7-9415-83437266c091.jpg)

## Discussion of Ublu

Report bugs or make feature requests in the [Issue Tracker](https://github.com/jwoehr/ublu/issues)

There is some more information in the [Ublu Wiki](https://github.com/jwoehr/ublu/wiki) including zine article references.

Discuss Ublu in the [IBMiOSS Ublu forum on Ryver](https://ibmioss.ryver.com/index.html#forums/1057363).

Here is the [IBMiOSS signup page for the free Open Source Software on IBM i organization on Ryver](https://ibmioss.ryver.com/application/signup/members/9tJsXDG7_iSSi1Q)
that hosts the Ublu forum.

Jack Woehr 2017-07-05
