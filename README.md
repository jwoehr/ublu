# ublu
Ublu Midrange and Mainframe Life Cycle Extension Language<br>
Copyright (c) 2015, Absolute Performance, Inc. http://www.absolute-performance.com<br>
Copyright (c) 2016, Jack J. Woehr http://www.softwoehr.com<br>
All rights reserved.<br>
See file LICENSE for license information.

Ublu is an interpretive language for remote systems programming of midrange or
mainframe hosts from a Java platform such as Linux, Mac, OpenBSD or Windows.

Ublu is already at version 1.0 and is being released as Open Source Software now
under the BSD-2 license.

Ublu is distributed with the open source libraries it needs and their license
files which permit such distribution.

The full reference is userdoc/ubluref.html

The user's guide is a work in progress (just started it): userdoc/ubluguide.html

Ublu is already a stable and useful tool which has seen much use in the real
world. It is neither complete nor perfect, but what is in this world?

We hope to make a formal release soon, but the system is fully useable as-is.

Quick start instructions:

To build and run:
<ul>
<li> Clone the Ublu Git repository https://github.com/jwoehr/ublu.git</li>
<li> Either load the project in <a href="http://www.netbeans.org">NetBeans</a>
     or cd to the top dir of the checkout and type <tt>ant</tt>.
<ul>
    <li> Of course you have <a href="http://ant.apache.org/">Apache Ant</a>
        installed, right?!</li>
</ul></li>
<li> ./dist/ublu.jar and its necessary ./dist/lib directory are the runtime system.
<li> java -jar ublu.jar to run Ublu.</li>
</ul>
 
- Jack Woehr 2016-05-16
