# Makefile to build and install Ublu from within an RPM build

all:
	ant

install:
	install -m 644 dist/ublu.jar ${prefix}/opt/ublu
	install -d dist/lib ${prefix}/opt/ublu
	install -m 644 lib/jt400.jar ${prefix}/opt/ublu/lib
	install -m 644 lib/pigiron.jar ${prefix}/opt/ublu/lib
	install -m 644 lib/postgresql-*.jar ${prefix}/opt/ublu/lib
	install -m 644 lib/sblim-cim-client2-HEAD.jar ${prefix}/opt/ublu/lib
	install -m 644 lib/sblim-cim-client2.properties ${prefix}/opt/ublu/lib
	install -m 644 lib/sblim-slp-client2.properties ${prefix}/opt/ublu/lib
	install -m 644 lib/tn5250j.jar ${prefix}/opt/ublu/lib
	install -m 755 bin/ublu ${prefix}/bin
	gzip man/ublu.1
	install -m 644 man/ublu.1.gz ${prefix}/man/man1
	install -d examples ${prefix}/opt/ublu
	install -d examples/pigiron ${prefix}/opt/ublu
	install -d examples/pigiron/test ${prefix}/opt/ublu
	install -d examples/test ${prefix}/opt/ublu
	install -d examples/test/suite ${prefix}/opt/ublu
	install -m 644 -t ${prefix}/opt/ublu/examples examples/*.ublu
	install -m 755 -t ${prefix}/opt/ublu/examples examples/*.sh
	install -m 644 -t ${prefix}/opt/ublu/examples/pigiron examples/pigiron/*.txt
	install -m 644 -t ${prefix}/opt/ublu/examples/pigiron examples/pigiron/*.ublu
	install -m 644 -t ${prefix}/opt/ublu/examples/pigiron/test examples/pigiron/test/*
	install -m 644 -t ${prefix}/opt/ublu/examples/test examples/test/*.ublu
	install -m 755 -t ${prefix}/opt/ublu/examples/test examples/test/*.sh
	install -m 644 -t ${prefix}/opt/ublu/examples/test/suite examples/test/suite/*.ublu
	install -m 755 -t ${prefix}/opt/ublu/examples/test/suite examples/test/suite/*.sh
	install -d extensions ${prefix}/opt/ublu
	install -d extensions/sysshep ${prefix}/opt/ublu
	install -m 644 -t ${prefix}/opt/ublu/extensions extensions/*.ublu
#	install -m 755 -t ${prefix}/opt/ublu/extensions extensions/*.sh
	install -m 644 -t ${prefix}/opt/ublu/extensions/sysshep extensions/sysshep/*.ublu
#	install -m 755 -t ${prefix}/opt/ublu/extensions/sysshep extensions/sysshep/*.sh
	install -d share ${prefix}/opt/ublu
	install -d share/atom ${prefix}/opt/ublu
	install -d share/atom/language-ublu ${prefix}/opt/ublu
	install -d share/atom/language-ublu/grammars ${prefix}/opt/ublu
	install -d share/atom/language-ublu/spec ${prefix}/opt/ublu
	install -d share/atom/language-ublu/lib ${prefix}/opt/ublu
	install -d share/atom/language-ublu/lib/flyover ${prefix}/opt/ublu
	install -d share/atom/language-ublu/snippets ${prefix}/opt/ublu
	install -d share/atom/language-ublu/styles ${prefix}/opt/ublu
	install -d share/clle ${prefix}/opt/ublu
	install -d share/jEdit ${prefix}/opt/ublu
	install -d share/mssql ${prefix}/opt/ublu
	install -d share/perl ${prefix}/opt/ublu
	install -d share/rpgle ${prefix}/opt/ublu
	install -d share/RPM ${prefix}/opt/ublu
	install -d share/tn5250j ${prefix}/opt/ublu
	install -d share/ublu-vimfiles-master ${prefix}/opt/ublu
	install -d share/ublu-vimfiles-master/indent ${prefix}/opt/ublu
	install -d share/ublu-vimfiles-master/syntax ${prefix}/opt/ublu
	install -d share/ublu-vimfiles-master/ftdetect ${prefix}/opt/ublu
	install -m 644 -t ${prefix}/opt/ublu/share/ublu-vimfiles-master/indent share/ublu-vimfiles-master/ftdetect/*.vim
	install -m 644 -t ${prefix}/opt/ublu/share/ublu-vimfiles-master/syntax share/ublu-vimfiles-master/ftdetect/*.vim
	install -m 644 -t ${prefix}/opt/ublu/share/ublu-vimfiles-master/ftdetect share/ublu-vimfiles-master/ftdetect/*.vim
	install -m 644 share/ublu-vimfiles-master/Makefile ${prefix}/opt/ublu/share/ublu-vimfiles-master
	install -m 644 share/ublu-vimfiles-master/LICENSE ${prefix}/opt/ublu/share/ublu-vimfiles-master
	install -m 644 share/ublu-vimfiles-master/README.md ${prefix}/opt/ublu/share/ublu-vimfiles-master
	install -m 644 share/RPM/Makefile ${prefix}/opt/ublu/share/RPM
	install -m 644 share/RPM/ublu.PASE.spec ${prefix}/opt/ublu/share/RPM
	install -m 644 -t ${prefix}/opt/ublu/share/clle share/clle/*.txt
	install -m 644 -t ${prefix}/opt/ublu/share/rpgle share/rpgle/*.txt
	install -m 644 share/atom/language-ublu/package.json ${prefix}/opt/ublu/share/atom/language-ublu
	install -m 644 -t ${prefix}/opt/ublu/share/atom/language-ublu/grammars share/atom/language-ublu/grammars/*.cson
	install -m 644 -t ${prefix}/opt/ublu/share/atom/language-ublu/spec share/atom/language-ublu/spec/*.js
	install -m 644 -t ${prefix}/opt/ublu/share/atom/language-ublu/lib/flyover share/atom/language-ublu/lib/flyover/*.txt
	install -m 644 -t ${prefix}/opt/ublu/share/atom/language-ublu/snippets share/atom/language-ublu/snippets/*.cson
	install -m 644 -t ${prefix}/opt/ublu/share/atom/language-ublu/styles share/atom/language-ublu/styles/*.less
	install -m 644 share/jEdit/ublu.xml ${prefix}/opt/ublu/share/jEdit
	install -m 644 share/mssql/DbMSSQL.java ${prefix}/opt/ublu/share/mssql
	install -m 644 share/mssql/README.DbMSSQL.txt ${prefix}/opt/ublu/share/mssql
	install -m 755 -t ${prefix}/opt/ublu/share/perl share/perl/*.pl
	install -m 644 share/tn5250j/My5250.diff ${prefix}/opt/ublu/share/tn5250j
	install -m 644 share/tn5250j/My5250.java ${prefix}/opt/ublu/share/tn5250j
	install -m 644 share/tn5250j/README.txt ${prefix}/opt/ublu/share/tn5250j
	install -m 644 share/ubluwin.properties ${prefix}/opt/ublu/share
	install -d userdoc ${prefix}/opt/ublu
	install -d userdoc/images ${prefix}/opt/ublu
	install -m 644 -t ${prefix}/opt/ublu/userdoc userdoc/*.html
	install -m 644 -t ${prefix}/opt/ublu/userdoc userdoc/*.css
	install -m 644 -t ${prefix}/opt/ublu/userdoc/images userdoc/images/*.jpg
	install -m 644 -t ${prefix}/opt/ublu/userdoc/images userdoc/images/*.png
	install -m 644 -t ${prefix}/opt/ublu/userdoc/images userdoc/images/*.ico
	install -d scripts 	${prefix}/opt/ublu
	install -m 755 -t ${prefix}/opt/ublu/scripts scripts/*.sh
	install	-m 644 JSON.license.txt  ${prefix}/opt/ublu
	install	-m 644 JTOPEN_LICENSE.html  ${prefix}/opt/ublu
	install	-m 644 LICENSE  ${prefix}/opt/ublu
	install	-m 644 PIGIRON_LICENSE.txt  ${prefix}/opt/ublu
	install	-m 644 POSTGRESQL_LICENSE  ${prefix}/opt/ublu
	install	-m 644 README.md  ${prefix}/opt/ublu
	install	-m 644 tn5250_LICENSE  ${prefix}/opt/ublu