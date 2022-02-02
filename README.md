# ublu
Ublu Midrange and Mainframe Life Cycle Extension Language<br>
Copyright (c) 2015, Absolute Performance, Inc. http://www.absolute-performance.com<br>
Copyright (c) 2018, Jack J. Woehr http://www.softwoehr.com<br>
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
perform ad-hoc process automation primarily on IBM i. I was
supporting consulting clients by writing individual utility programs using JTOpen
which I have used since 1998 to control the AS/400. I decided to consolidate
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

The example is syntax-colored using a [jEdit](https://jedit.org) edit mode provided with Ublu.

The latest release version of Ublu is [version 2.0.0](https://github.com/jwoehr/ublu/releases/tag/v2.0.0).

It is strongly advised you use a release version of Ublu, preferably the latest.

Ublu is distributed with some of the open source libraries it needs and their license files which permit such distribution.
Others are fetched into the project at build time via <a href="https://maven.apache.org/">`maven`</a>.

Ublu is already a stable and useful tool which has seen much use in the real world. It is neither complete nor perfect,
but what is in this world? As with all open source software, there is <b>NO WARRANTY nor GUARANTEE include as regards
suitability for any given application</b>.

<a name="quickstart"></a>
## Quick start instructions

Download the release, unpack and `java -jar ublu.jar` to run Ublu.

Or clone https://maven.apache.org/the source for Ublu and do a `maven` build:
<ul>
<li> Clone the [Ublu GitHub repository](https://github.com/jwoehr/ublu.git) or download source from the
[latest release](https://github.com/jwoehr/ublu/releases)</li>
<li> Load the project in <a href="http://www.netbeans.org">NetBeans</a> or <a href="https://www.eclipse.org/">Eclipse</a>
     or cd to the top dir of the checkout and type `make clean dist` which will run the appropriate
     `maven` commands for you.
</li>
<li> `target/ublu.jar` is the runtime system.</li>
<li> `java -jar target/ublu.jar` to run Ublu as a plain Java console application.</li>
</ul>

<b>Note regarding checking out the current source:</b> Release versions of Ublu come with a standard release of JTOpen.
Between Ublu releases, I build and sometimes modify JTOpen so that Ublu can leverage forthcoming features of JTOpen.
When you check out the source between releases, it is an intermediate version JTOpen that is checked out with Ublu.

<a name="goublu"></a>
## Goublu console support for Ublu

Ublu's interpreter relies on Java's console support, which is very weak.
So I have coded [Goublu](https://github.com/jwoehr/goublu) in [Go language](https://golang.org/).

![goublu_screenshot](https://user-images.githubusercontent.com/4604036/28322382-317d05fa-6b93-11e7-8457-b07eec2873af.png)

Goublu is a console front-end that provides an editable Ublu command line. The go command

`go get -u github.com/jwoehr/goublu`

will fetch the source to your `$GOPATH/src` directory. Use `go build main/goublu.go` to build Goublu for your architecture.

## Ublu in a Window
You can start Ublu in a window with the `-w [propsfilepath]` switch.
![Ublu in a Window](https://user-images.githubusercontent.com/4604036/27810879-ed42fa88-601c-11e7-9415-83437266c091.jpg)

## <a name="discussion">Discussion of Ublu</a>

Report bugs or make feature requests in the [Issue Tracker](https://github.com/jwoehr/ublu/issues)

There is some more information in the [Ublu Wiki](https://github.com/jwoehr/ublu/wiki) including zine article references.

Discuss Ublu in the [IBMiOSS Java forum on Ryver](https://ibmioss.ryver.com/index.html#forums/1057363).

Here is the [IBMiOSS signup page for the free Open Source Software on IBM i organization on Ryver](https://ibmioss.ryver.com/application/signup/members/9tJsXDG7_iSSi1Q)
that hosts Java forum.

![Ublu running native on IBM i](https://user-images.githubusercontent.com/4604036/30892141-33301764-a2f4-11e7-88e6-e7583866037e.jpg)
*_Ublu running native on IBM i_*

## Default Branch Renamed

The default branch has been renamed!

`master` is now named `main`

If you have a local clone, you can update it by running:
```
git branch -m master main
git fetch origin
git branch -u origin/main main
git remote set-head origin -a
```
Jack Woehr 2022-02-02
