# Makefile to install Ublu from within an RPM
install_sh_DATA = $(install_sh) -c -m 644
install_sh_PROGRAM = $(install_sh) -c
install_sh_SCRIPT = $(install_sh) -c

all:
	ant
	
install:
	cp dist/ublu.jar ${prefix}/opt/ublu
	cp -R dist/lib ${prefix}/opt/ublu
	cp -R dist/userdoc ${prefix}/opt/ublu
	cp -R dist/share ${prefix}/opt/ublu
	cp -R dist/lib ${prefix}/opt/ublu
