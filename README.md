# ublu

Ublu Midrange and Mainframe Life Cycle Extension Language  
Copyright (c) 2015, [Absolute Performance, Inc.](http://www.absolute-performance.com)  
Copyright (c) 2016 - 2025 [Jack J. Woehr](http://www.softwoehr.com)  
All rights reserved.  
See file LICENSE for license information.

* [General information](#general-information)
* [Quickstart instructions](#quickstart-instructions)
* [Goublu console support for Ublu](#goublu-console-support-for-ublu)
* [Ublu in a Window](#ublu-in-a-window)
* [Discussion of Ublu](#discussion-of-ublu)
* [Default Branch Renamed](#default-branch-renamed)
* [Software Bill of Materials](#software-bill-of-materials)

## General information

Ublu is an interpretive language for remote systems programming of midrange or
mainframe hosts from a Java platform such as Linux, Mac, OpenBSD or Windows. It
also can run natively on IBM i ®, IBM z/OS USS ® or any other reasonable Java
platform including Android [UserLAnd](https://userland.tech/).

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

* The user's guide is [userdoc/ubluguide.html](http://www.softwoehr.com/oss/ubludoc/ubluguide.html)
* The full reference is [userdoc/ubluref.html](http://www.softwoehr.com/oss/ubludoc/ubluref.html)
  * Both documents are found in the source tree in the `userdoc` directory.
* Here's an [example of Ublu code](http://www.softwoehr.com/oss/ubludoc/jobstuff-example.html)
  * The example is syntax-colored using a [jEdit](https://jedit.org) edit mode provided with Ublu in the `share/jEdit` directory.
* [Ublu Notebook](https://notebooklm.google.com/notebook/0799833f-a57b-4ad7-a017-aecc2069827b) offers _**experimental**_ AI assistance based on the source and the extant documentation.

The latest release version of Ublu is [version 2.0.6](https://github.com/jwoehr/ublu/releases/tag/v2.0.6).

Ublu is distributed with some of the open source libraries it needs and their license files which permit such distribution. Others are fetched into the project at build time via [`maven`](https://maven.apache.org/).

Ublu is already a stable and useful tool which has seen much use in the real world. It is neither complete nor perfect. As with all open source software, there is **NO WARRANTY nor GUARANTEE including as regards suitability for any given application**.

## Quickstart instructions

Download the release, unpack and `java -jar ublu.jar` to run Ublu.  
Or clone the source for Ublu and do a `maven` build:

* Clone the [Ublu GitHub repository](https://github.com/jwoehr/ublu.git) or download source from the [latest release](https://github.com/jwoehr/ublu/releases)
* Load the project in [NetBeans](http://www.netbeans.org) or [Eclipse](https://www.eclipse.org/) or cd to the top dir of the checkout and type `make clean dist` which will run the appropriate `maven` commands for you.
* `target/ublu*.jar` is the naming pattern for the runtime system, the sources, and the javadoc jars.
* `java -jar target/ublu-`_version_`-jar-with-dependencies.jar` to run Ublu as a plain Java console application.

## Goublu console support for Ublu

Ublu's interpreter relies on Java's console support, which is very weak.
So I have coded [Goublu](https://github.com/jwoehr/goublu) in [Go language](https://golang.org/).

![goublu_screenshot](https://user-images.githubusercontent.com/4604036/28322382-317d05fa-6b93-11e7-8457-b07eec2873af.png)

Goublu is a console front-end that provides an editable Ublu command line. The go command

`go get -u github.com/jwoehr/goublu`

will fetch the source to your `$GOPATH/src` directory. `cd $GOPATH/src/github.com/jwoehr/goublu; ./make.sh` to build Goublu for your architecture.

## Ublu in a Window

You can start Ublu in a window with the `-w [propsfilepath]` switch.
![Ublu in a Window](https://user-images.githubusercontent.com/4604036/27810879-ed42fa88-601c-11e7-9415-83437266c091.jpg)

## Discussion of Ublu

Report bugs or make feature requests in the [Issue Tracker](https://github.com/jwoehr/ublu/issues)

There is some more information in the [Ublu Wiki](https://github.com/jwoehr/ublu/wiki) including zine article references.

Discuss Ublu in the [IBMiOSS Forum Java channel](https://chat.ibmioss.org/#narrow/channel/12-java).

![Ublu running native on IBM i](https://user-images.githubusercontent.com/4604036/30892141-33301764-a2f4-11e7-88e6-e7583866037e.jpg)
_Ublu running native on IBM i_

## Default Branch Renamed

The default branch has been renamed!

`master` is now named `main`

If you have a local clone, you can update it by running:

```bash
git branch -m master main
git fetch origin
git branch -u origin/main main
git remote set-head origin -a
```

## Software Bill of Materials

The [Software Bill of Materials (SBOM)](https://docs.github.com/en/code-security/supply-chain-security/understanding-your-software-supply-chain/exporting-a-software-bill-of-materials-for-your-repository) is `SBOM_ublu_jwoehr_*.json`

Jack Woehr 2025-11-07
