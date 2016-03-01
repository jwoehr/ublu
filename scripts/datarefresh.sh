#!/bin/bash
# Grab a Savefile, FTP it over to the target and Restore it
# Currently, only library-level restore is supported by this script
# (although the underlying Ublu code supports object-by-object restore).
#
function usage() {
cat << ENDENDEND
Usage:
    $0 -h (for help)
        -or-
    $0 -a srclib -b src_savf [ -c dest_lib ] [ -d dest_savf ] -l lib_to_restore [ -p src_portnum ] [ -q dest-portnum ] -s src_host -t dest_host [ -u tempdir ] src_uid src_passwd dest_uid dest_passwd
Relays a savefile via the local host using FTP from src_host to dest_host then restores lib_to_restore (obligatory) from the dest_savf save file in dest_lib on the dest_host.
If dest_lib and dest_savf are not specified, they default to the same names as the source lib and save file.
Example: $0 -a SAVEFILES -b FOOBAR -l SAVEDLIB -s 111.111.111.111 -t 222.222.222.222 QSECOFR 1x2ya QSECOFR 1x2yb
(This leaves the save file as time-stamped temp file in the current directory which can be deleted after the operation
is complete, or used to complete the operation if it does not complete.)
Currently, only library-level restore is supported  by this script (although the underlying Ublu code supports object-by-object restore).
Exit values:
    0   -   Correct execution or -h invoked at head of options
    1   -   -h found later in list of options
    2   -   wrong number of arguments to the command (see usage above) or error in getopts
ENDENDEND
}
#
# Defaults for some vars
SRC_HOST=""
DEST_HOST=""
SRC_PORT=21
DEST_PORT=21
LIB_TO_RESTORE="IMPOSSIBLY_LONG_LIBNAME"
SRC_LIB=""
DEST_LIB=""
SRC_FILE=""
DEST_FILE=""
TEMP_DIR=`pwd`
#
if [ "$1" == "-h" ]
then
    usage
    exit 0
fi
#
options=`getopt a:b:c:d:hl:p:q:s:t:u: $*`
if [ $? != 0 ]
then
    usage
    exit 2
fi
set -- $options
for i in $options
do
    case "$i"
    in
        -a)
            SRC_LIB="$2";shift;
            shift;;
        -b)
            SRC_FILE="$2";shift;
            shift;;
        -c)
            DEST_LIB="$2";shift;
            shift;;
        -d)
            DEST_FILE="$2";shift;
            shift;;
        -h)
            usage;exit 1
            ;;
        -l)
            LIB_TO_RESTORE="$2";shift;
            shift;;
        -p)
            SRC_PORT="$2";shift;
            shift;;
	-q)
	    DEST_PORT="$2";shift;
            shift;;
        -s)
            SRC_HOST="$2";shift;
            shift;;
        -t)
            DEST_HOST="$2";shift;
            shift;;
        -u) 
            TEMP_DIR="$2";shift;
            shift;;
        --)
            shift; break;;
    esac
done

# Set some default vars that have to wait for others to be set
#echo $# args left
#echo $*
if [ "${DEST_LIB}" = "" ]
then
    DEST_LIB=${SRC_LIB}
fi
if [ "${DEST_FILE}" = "" ]
then
    DEST_FILE=${SRC_FILE}
fi
NOW=`date "+%Y%m%d.%H%M%S"`
TEMP_FILE=${TEMP_DIR}/${SRC_HOST}'$'${SRC_PORT}'$'${SRC_LIB}'$'${SRC_FILE}.${NOW}
# Grab the four arguments
if [ $# -ne 4 ]
then
    usage
    exit 2
fi
# Announce
echo "$0 preparing to restore ${SRC_HOST}.${SRC_PORT}:${SRC_LIB}/${SRC_FILE} to ${DEST_HOST}.${DEST_PORT}:${DEST_LIB}/${DEST_FILE} via ${TEMP_FILE}"
src_uid=$1
src_passwd=$2
dest_uid=$3
dest_passwd=$4
# Invoke
# java -jar /opt/api-java/ublu.jar << ENDENDEND1
# cat << ENDENDEND1
java -jar /opt/ublu/ublu.jar << ENDENDEND1
put \${ fetching file from host ... will take a while }$
ftp -mode pas -type bin -as400 -get /QSYS.LIB/${SRC_LIB}.LIB/${SRC_FILE}.SAVF -to ${TEMP_FILE} ${SRC_HOST} ${src_uid} ${src_passwd}
put \${ pushing file to destination host ... will take a long time }$
ftp -mode pas -type bin -as400 -put ${TEMP_FILE} -to /QSYS.LIB/${DEST_LIB}.LIB/${DEST_FILE}.SAVF ${DEST_HOST} ${dest_uid} ${dest_passwd}
put \${ restoring on destination host from save file }$
commandcall ${DEST_HOST} ${dest_uid} ${dest_passwd} \${ RSTLIB SAVLIB(${LIB_TO_RESTORE}) DEV(*SAVF) SAVF(${DEST_LIB}/${DEST_FILE}) MBROPT(*ALL) ALWOBJDIF(*ALL) }$
put \${ data refresh complete }$
ENDENDEND1
#savf -restore -lib ${LIB_TO_RESTORE} ${DEST_HOST} ${DEST_LIB} ${DEST_FILE} ${dest_uid} ${dest_passwd}
exit 0
# End
