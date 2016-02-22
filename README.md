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

I am working on it in my Copious Spare Time to make Ublu's git repository
complete for this already pretty stable and useful tool! For now, here's the
quick start instructions:

To build and run:
1. Clone the Ublu Git repository https://github.com/jwoehr/ublu.git
2. Download or build the following and place them in the ./lib directory:
    * jtopen.jar (https://sourceforge.net/projects/jt400/)
    * jtopenlite.jar (https://sourceforge.net/projects/jt400/)
    * postgresql-9.2-1003.jdbc4.jar or later (http://www.postgresql.org)
        * (Do a global search on this string in ./nbproject/project.properties
           and replace with actual postgresql JDBC jar version you find/build.)
3. cd to the top dir of the checkout and type ant
4. ./dist/ublu.jar and its necessary .dist/lib directory are the runtime system.
5. java -jar ublu.jar to run Ublu

Documentation and examples are being tidied up! Real Soon Now!
 
- Jack Woehr jwoehr@softwoehr.com 2016-02-21 
