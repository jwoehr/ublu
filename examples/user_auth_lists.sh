# user_auth_lists.sh ... generate 2 lists, one of basic auth info, the other of ALLOBJ authorized accounts 
# autogenerated Mon Sep 24 13:40:28 MDT 2018 by jwoehr using command:
# gensh -to user_auth_lists.sh -path /opt/ublu/ublu.jar -includepath /opt/ublu/examples:/opt/ublu/extensions -optr s SYSNAME @sysname ${ system name }$ -optr u USERPROF @userprof ${ user profile }$ -optr p PASSWORD @password ${ password }$ -optr b BASIC @basicAcctListName ${ filename for basic account info }$ -optr a ALLOBJ @allobjAcctListName ${ filname for *ALLOBJ account info }$ ${ user_auth_lists.sh ... generate 2 lists, one of basic auth info, the other of ALLOBJ authorized accounts }$ user_auth_lists.ublu  ${ user_auth_lists ( @sysname @userprof @password @basicAcctListName @allobjAcctListName ) }$

# Usage message
function usage {
echo "user_auth_lists.sh ... generate 2 lists, one of basic auth info, the other of ALLOBJ authorized accounts "
echo "This shell script was autogenerated Mon Sep 24 13:40:28 MDT 2018 by jwoehr."
echo "Usage: $0 [silent] [-h] [-X...] [-Dprop=val] -s SYSNAME -u USERPROF -p PASSWORD -b BASIC -a ALLOBJ"
echo "	where"
echo "	-h		display this help message and exit 0"
echo "	-X xOpt		pass a -X option to the JVM (can be used many times)"
echo "	-D some.property=\"some value\"	pass a property to the JVM (can be used many times)"
echo "	-s SYSNAME	system name  (required option)"
echo "	-u USERPROF	user profile  (required option)"
echo "	-p PASSWORD	password  (required option)"
echo "	-b BASIC	filename for basic account info  (required option)"
echo "	-a ALLOBJ	filname for *ALLOBJ account info  (required option)"
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
while getopts s:u:p:b:a:D:X:h the_opt
do
	case "$the_opt" in
		s)	SYSNAME="$OPTARG";;
		u)	USERPROF="$OPTARG";;
		p)	PASSWORD="$OPTARG";;
		b)	BASIC="$OPTARG";;
		a)	ALLOBJ="$OPTARG";;
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
if [ "${USERPROF}" != "" ]
then
	gensh_runtime_opts="${gensh_runtime_opts}string -to @userprof -trim \${ ${USERPROF} }$ "
else
	echo "Option -u USERPROF is a required option but is not present."
	usage
	exit 2
fi
if [ "${PASSWORD}" != "" ]
then
	gensh_runtime_opts="${gensh_runtime_opts}string -to @password -trim \${ ${PASSWORD} }$ "
else
	echo "Option -p PASSWORD is a required option but is not present."
	usage
	exit 2
fi
if [ "${BASIC}" != "" ]
then
	gensh_runtime_opts="${gensh_runtime_opts}string -to @basicAcctListName -trim \${ ${BASIC} }$ "
else
	echo "Option -b BASIC is a required option but is not present."
	usage
	exit 2
fi
if [ "${ALLOBJ}" != "" ]
then
	gensh_runtime_opts="${gensh_runtime_opts}string -to @allobjAcctListName -trim \${ ${ALLOBJ} }$ "
else
	echo "Option -a ALLOBJ is a required option but is not present."
	usage
	exit 2
fi

SCRIPTDIR=$(CDPATH= cd "$(dirname "$0")" && pwd)

# Prelude commands to execute before invocation
# No prelude commands

# Invocation
java${JVMOPTS}${JVMPROPS} -Dublu.includepath="/opt/ublu/examples:/opt/ublu/extensions" -jar /opt/ublu/ublu.jar ${gensh_runtime_opts} include ${SILENT}user_auth_lists.ublu  user_auth_lists \( @sysname @userprof @password @basicAcctListName @allobjAcctListName \) 
exit $?
