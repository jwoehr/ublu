# test_pigiron.sh ... run automated tests on PigIron bindings in Ublu using defaults from the file included by the -f switch to this script 
# autogenerated Sun Dec 20 12:42:12 MST 2015 by jax using command:
# gensh -to test_pigiron.sh -path /opt/ublu/ublu.jar -optr f INCLUDE_FILE @include_file ${ file of test defaults to include ... see test_defaults.samp for an example }$ ${ test_pigiron.sh ... run automated tests on PigIron bindings in Ublu using defaults from the file included by the -f switch to this script }$ test_pigiron.ublu ${ test_pigiron ( @include_file ) }$

# Usage message
function usage { 
echo "test_pigiron.sh ... run automated tests on PigIron bindings in Ublu using defaults from the file included by the -f switch to this script "
echo "This shell script was autogenerated Sun Dec 20 12:42:10 MST 2015 by jax."
echo "Usage: $0 [silent] -h -f INCLUDE_FILE "
echo "	where"
echo "	-h		display this help message and exit 0"
echo "	-f INCLUDE_FILE	file of test defaults to include ... see test_defaults.samp for an example  (required option)"
echo "---"
echo "If the keyword 'silent' appears ahead of all options, then included files will not echo and prompting is suppressed."
echo "Exit code is the result of execution, or 0 for -h or 2 if there is an error in processing options"
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
while getopts f:h the_opt
do
	case "$the_opt" in
		f)	INCLUDE_FILE="$OPTARG";;
		h)	usage;exit 0;;
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
if [ "${INCLUDE_FILE}" != "" ]
then
	gensh_runtime_opts="${gensh_runtime_opts}string -to @include_file -trim \${ ${INCLUDE_FILE} }$ "
else
	echo "Option -f INCLUDE_FILE is a required option but is not present."
	usage
	exit 2
fi

# Invocation
java -jar /opt/ublu/ublu.jar ${gensh_runtime_opts} include ${SILENT}test_pigiron.ublu test_pigiron \( @include_file \) 
exit $?
