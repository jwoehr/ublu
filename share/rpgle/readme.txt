This directory contains one or more RPGLE programs to use with Ublu.

dateParm.txt is an example by Booth Martin booth@martinvt.com which takes
a date and turns it into the weekday and a julian date. It is used with
examples/test/testprogramcall.ublu

Copy it up to the IFS and compile it something like:

  CRTBNDRPG PGM(JWOEHR/DATEPARMSR) +
  SRCSTMF('/home/jwoehr/work/rpgle/dateparm.txt') +
  TEXT('Multiparm program Booth Martin to test Ublu')

Thanks to Booth Martin for his help!