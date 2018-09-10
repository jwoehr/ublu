# Makefile to build and install Ublu from within an RPM build

all:
	ant
	
install:
	install    dist/ublu.jar ${prefix}/opt/ublu
	install -d dist/lib ${prefix}/opt/ublu
	install    bin/ublu ${prefix}/bin
	install -d examples ${prels fix}/opt/ublu
	install -t ${prefix}/opt/ublu/examples examples/*
	install -d examples/pigiron ${prefix}/opt/ublu
	install -t ${prefix}/opt/ublu/examples/pigiron examples/pigiron/*
	install -d examples/pigiron/test ${prefix}/opt/ublu
	install -t ${prefix}/opt/ublu/examples/pigiron/test examples/pigiron/test/*
	install -d examples/test ${prefix}/opt/ublu
	install -d examples/test/suite ${prefix}/opt/ublu
	install -d extensions ${prefix}/opt/ublu
	install -d extensions/sysshep ${prefix}/opt/ublu
	install -d userdoc ${prefix}/opt/ublu
	install -d scripts 	${prefix}/opt/ublu
	install -d share ${prefix}/opt/ublu
	install -d share/atom ${prefix}/opt/ublu
	install -d share/jEdit ${prefix}/opt/ublu
	install -d share/mssql ${prefix}/opt/ublu
	install -d share/perl ${prefix}/opt/ublu
	install -d share/RPM ${prefix}/opt/ublu
	install -d share/tn5250j ${prefix}/opt/ublu
	install share/ublu-vimfiles-master ${prefix}/opt/ublu/share
	install share/ubluwin.properties ${prefix}/opt/ublu/share
	install		JSON.license.txt  ${prefix}/opt/ublu
	install		JTOPEN_LICENSE.html  ${prefix}/opt/ublu
	install		LICENSE  ${prefix}/opt/ublu
	install		PIGIRON_LICENSE.txt  ${prefix}/opt/ublu
	install		POSTGRESQL_LICENSE  ${prefix}/opt/ublu
	install		README.md  ${prefix}/opt/ublu
	install		tn5250_LICENSE  ${prefix}/opt/ublu