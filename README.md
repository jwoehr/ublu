# ublu
Ublu Midrange and Mainframe Life Cycle Extension Language
Copyright (c) 2015, Absolute Performance, Inc. http://www.absolute-performance.com
Copyright (c) 2016, Jack J. Woehr jwoehr@softwoehr.com http://www.softwoehr.com
All rights reserved.
See file LICENSE for license information.

Ublu is an interpretive language for remote systems programming of midrange or
mainframe host from a Java platform such as OpenBSD, Linux or Windows. It is
already at version 1.0 and is being released as Open Source Software now under
the BSD-2 license. However, the tree has to be reworked from an in-house tool
to an open source tool and that is why some stuff is mysterious.

Ublu is distributed with the open source libraries it needs and their license
files which permit such distribution.

There will be a distribution soon. All that is missing is the documentation,
which need to be cleaned up for public distribution. Ublu is already a stable
and useful tool which has seen much use in the real world.

For now, here's the quick start instructions:

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
Documentation and examples are being tidied up! Real Soon Now!
 
- Jack Woehr jwoehr@softwoehr.com 2016-02-21 
