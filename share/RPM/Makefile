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
	install -d examples ${prefix}/opt/ublu
	install -d examples/pigiron ${prefix}/opt/ublu
	install -d examples/pigiron/test ${prefix}/opt/ublu
	install -d examples/test ${prefix}/opt/ublu
	install -d examples/test/suite ${prefix}/opt/ublu
	install -m 644 -t ${prefix}/opt/ublu/examples examples/*.ublu
	install -m 644 -t ${prefix}/opt/ublu/examples examples/*.sh
	install -m 644 -t ${prefix}/opt/ublu/examples/pigiron examples/pigiron/*.txt
	install -m 644 -t ${prefix}/opt/ublu/examples/pigiron examples/pigiron/*.ublu
	install -m 644 -t ${prefix}/opt/ublu/examples/pigiron/test examples/pigiron/test/*
	install -m 644 -t ${prefix}/opt/ublu/examples/test examples/test/*.ublu
	install -m 644 -t ${prefix}/opt/ublu/examples/test examples/test/*.sh
	install -m 644 -t ${prefix}/opt/ublu/examples/test/suite examples/test/suite/*.ublu
	install -m 644 -t ${prefix}/opt/ublu/examples/test/suite examples/test/suite/*.sh
	install -d extensions ${prefix}/opt/ublu
	install -d extensions/sysshep ${prefix}/opt/ublu
	install -d share ${prefix}/opt/ublu
	install -d share/atom ${prefix}/opt/ublu
	install -d share/atom/language-ublu ${prefix}/opt/ublu
	install -d share/atom/language-ublu/grammars ${prefix}/opt/ublu
	install -d share/atom/language-ublu/spec ${prefix}/opt/ublu
	install -d share/atom/language-ublu/lib ${prefix}/opt/ublu
	install -d share/atom/language-ublu/lib/flyover ${prefix}/opt/ublu
	install -d share/atom/language-ublu/snippets ${prefix}/opt/ublu
	install -d share/atom/language-ublu/styles ${prefix}/opt/ublu
	install -d share/jEdit ${prefix}/opt/ublu
	install -d share/mssql ${prefix}/opt/ublu
	install -d share/perl ${prefix}/opt/ublu
	install -d share/RPM ${prefix}/opt/ublu
	install -d share/tn5250j ${prefix}/opt/ublu
	install -d share/ublu-vimfiles-master ${prefix}/opt/ublu/share
	install -d share/ublu-vimfiles-master/indent ${prefix}/opt/ublu/share
	install -d share/ublu-vimfiles-master/syntax ${prefix}/opt/ublu/share
	install -d share/ublu-vimfiles-master/ftdetect ${prefix}/opt/ublu/share
	install -m 644 share/ubluwin.properties ${prefix}/opt/ublu/share
	install -d userdoc ${prefix}/opt/ublu
	install -d userdoc/images ${prefix}/opt/ublu
	install -d scripts 	${prefix}/opt/ublu
	install	-m 644 JSON.license.txt  ${prefix}/opt/ublu
	install	-m 644 JTOPEN_LICENSE.html  ${prefix}/opt/ublu
	install	-m 644 LICENSE  ${prefix}/opt/ublu
	install	-m 644 PIGIRON_LICENSE.txt  ${prefix}/opt/ublu
	install	-m 644 POSTGRESQL_LICENSE  ${prefix}/opt/ublu
	install	-m 644 README.md  ${prefix}/opt/ublu
	install	-m 644 tn5250_LICENSE  ${prefix}/opt/ublu