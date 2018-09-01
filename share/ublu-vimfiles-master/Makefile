.PHONY : install uninstall

VIMHOME ?= ~/.vim

install : syntax/ublu.vim indent/ublu.vim ftdetect/ublu.vim
	mkdir -vp $(VIMHOME)/syntax $(VIMHOME)/indent $(VIMHOME)/ftdetect
	cp -vt $(VIMHOME)/indent ./indent/ublu.vim
	cp -vt $(VIMHOME)/syntax ./syntax/ublu.vim
	cp -vt $(VIMHOME)/ftdetect ./ftdetect/ublu.vim

uninstall:
	-rm $(VIMHOME)/indent/ublu.vim $(VIMHOME)/syntax/ublu.vim $(VIMHOME)/ftdetect/ublu.vim
