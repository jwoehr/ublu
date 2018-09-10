# Makefile to build and install Ublu from within an RPM build

all:
	ant
	
install:
	install    dist/ublu.jar ${prefix}/opt/ublu
	install -d dist/lib ${prefix}/opt/ublu
	install    bin/ublu ${prefix}/bin
	install -d examples ${prefix}/opt/ublu
	install -d extensions ${prefix}/opt/ublu
	install -d userdoc ${prefix}/opt/ublu
	install -d scripts 	${prefix}/opt/ublu
	install -d share ${prefix}/opt/ublu
	install		JSON.license.txt  ${prefix}/opt/ublu
	install		JTOPEN_LICENSE.html  ${prefix}/opt/ublu
	install		LICENSE  ${prefix}/opt/ublu
	install		PIGIRON_LICENSE.txt  ${prefix}/opt/ublu
	install		POSTGRESQL_LICENSE  ${prefix}/opt/ublu
	install		README.md  ${prefix}/opt/ublu
	install		tn5250_LICENSE  ${prefix}/opt/ublu