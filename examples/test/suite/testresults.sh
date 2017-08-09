# testresults.sh
# autogenerated Mon Aug 07 00:14:02 MDT 2017 by jax using command:
# gensh -to testresults.sh -path /opt/ublu/ublu.jar -optr s SYSNAME @sysname ${ System where test results reside }$ -opt z USESSL @usessl ${ Y to use SSL }$ -optr u USERID @uid ${ User profile }$ -optr p PASSWD @passwd ${ Password }$ -optr i IFSPATH @ifspath ${ IFS path to test report physical file }$ -includepath /opt/ublu/examples:/opt/ublu/examples/test/suite/ ${ testresults.sh }$ testresults.ublu ${ testresults ( @sysname @usessl @uid @passwd @ifspath ) }$

# Usage message
function usage {
echo "testresults.sh"
echo "This shell script was autogenerated Mon Aug 07 00:14:02 MDT 2017 by jax."
echo "Usage: $0 [silent] [-h] [-X...] [-Dprop=val] -s SYSNAME [-z USESSL] -u USERID -p PASSWD -i IFSPATH"
echo "	where"
echo "	-h		display this help message and exit 0"
echo "	-X xOpt		pass a -X option to the JVM (can be used many times)"
echo "	-D some.property=\"some value\"	pass a property to the JVM (can be used many times)"
echo "	-s SYSNAME	System where test results reside  (required option)"
echo "	-z USESSL	Y to use SSL "
echo "	-u USERID	User profile  (required option)"
echo "	-p PASSWD	Password  (required option)"
echo "	-i IFSPATH	IFS path to test report physical file  (required option)"
echo "---"
echo "If the keyword 'silent' appears ahead of all options, then included files will not echo and prompting is suppressed."
echo "Exit code is the result of execution, or 0 for -h or 2 if there is an error in processing options"
echo "This script sets \$SCRIPTDIR to the script's directory prior to executing prelude commands and Ublu invocation."
}

#Test if user wants silent includes
if [ "$1" == "silent" ]
then
	SILENT="-silent "
	shift
else
	SILENT=""
fi

# Process options
while getopts s:z:u:p:i:D:X:h the_opt
do
	case "$the_opt" in
		s)	SYSNAME="$OPTARG";;
		z)	USESSL="$OPTARG";;
		u)	USERID="$OPTARG";;
		p)	PASSWD="$OPTARG";;
		i)	IFSPATH="$OPTARG";;
		h)	usage;exit 0;;
		D)	JVMPROPS="${JVMPROPS} -D${OPTARG}";;
		X)	JVMOPTS="${JVMOPTS} -X${OPTARG}";;
		[?])	usage;exit 2;;

	esac
done
shift `expr ${OPTIND} - 1`
if [ $# -ne 0 ]
then
	echo "Superfluous argument(s) $*"
	usage
	exit 2
fi

# Translate options to tuple assignments
if [ "${SYSNAME}" != "" ]
then
	gensh_runtime_opts="${gensh_runtime_opts}string -to @sysname -trim \${ ${SYSNAME} }$ "
else
	echo "Option -s SYSNAME is a required option but is not present."
	usage
	exit 2
fi
if [ "${USESSL}" != "" ]
then
	gensh_runtime_opts="${gensh_runtime_opts}string -to @usessl -trim \${ ${USESSL} }$ "
fi
if [ "${USERID}" != "" ]
then
	gensh_runtime_opts="${gensh_runtime_opts}string -to @uid -trim \${ ${USERID} }$ "
else
	echo "Option -u USERID is a required option but is not present."
	usage
	exit 2
fi
if [ "${PASSWD}" != "" ]
then
	gensh_runtime_opts="${gensh_runtime_opts}string -to @passwd -trim \${ ${PASSWD} }$ "
else
	echo "Option -p PASSWD is a required option but is not present."
	usage
	exit 2
fi
if [ "${IFSPATH}" != "" ]
then
	gensh_runtime_opts="${gensh_runtime_opts}string -to @ifspath -trim \${ ${IFSPATH} }$ "
else
	echo "Option -i IFSPATH is a required option but is not present."
	usage
	exit 2
fi

SCRIPTDIR=$(CDPATH= cd "$(dirname "$0")" && pwd)

# Prelude commands to execute before invocation
# No prelude commands

# Invocation
java${JVMOPTS}${JVMPROPS} -Dublu.includepath="/opt/ublu/examples:/opt/ublu/examples/test/suite/" -jar /opt/ublu/ublu.jar ${gensh_runtime_opts} include ${SILENT}testresults.ublu testresults \( @sysname @usessl @uid @passwd @ifspath \) 
exit $?