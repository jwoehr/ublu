# monitorIBMJVMMem.sh ... poll IBM JVM memory via JMX and output SystemShepherd datapoints 
# autogenerated Mon Oct 10 21:16:46 MDT 2016 by jax using command:
# gensh -to monitorIBMJVMMem.sh -path /opt/ublu/ublu.jar -optr s SERVER @server ${ Server to monitor }$ -optr j JMX_PORT @jmxport ${ JMX listening port }$ -optr p PASSWORD @password ${ Password for JXM monitorRole }$ -optr l CRITLEVEL @critlevel ${ Critical heap used level }$ -optr m CRITMSG @critmsg ${ Message attached to datapoint when heap goes critical }$ ${ monitorIBMJVMMem.sh ... poll IBM JVM memory via JMX and output SystemShepherd datapoints }$ /opt/ublu/examples/monitorJVMMem.ublu ${ monitorIBMJVMMem ( @server @jmxport @password @critlevel @critmsg ) }$

# Usage message
function usage { 
echo "monitorIBMJVMMem.sh ... poll IBM JVM memory via JMX and output SystemShepherd datapoints "
echo "This shell script was autogenerated Mon Oct 10 21:16:46 MDT 2016 by jax."
echo "Usage: $0 [silent] -h -s SERVER -j JMX_PORT -p PASSWORD -l CRITLEVEL -m CRITMSG "
echo "	where"
echo "	-h		display this help message and exit 0"
echo "	-s SERVER	Server to monitor  (required option)"
echo "	-j JMX_PORT	JMX listening port  (required option)"
echo "	-p PASSWORD	Password for JXM monitorRole  (required option)"
echo "	-l CRITLEVEL	Critical heap used level  (required option)"
echo "	-m CRITMSG	Message attached to datapoint when heap goes critical  (required option)"
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
while getopts s:j:p:l:m:h the_opt
do
	case "$the_opt" in
		s)	SERVER="$OPTARG";;
		j)	JMX_PORT="$OPTARG";;
		p)	PASSWORD="$OPTARG";;
		l)	CRITLEVEL="$OPTARG";;
		m)	CRITMSG="$OPTARG";;
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
if [ "${SERVER}" != "" ]
then
	gensh_runtime_opts="${gensh_runtime_opts}string -to @server -trim \${ ${SERVER} }$ "
else
	echo "Option -s SERVER is a required option but is not present."
	usage
	exit 2
fi
if [ "${JMX_PORT}" != "" ]
then
	gensh_runtime_opts="${gensh_runtime_opts}string -to @jmxport -trim \${ ${JMX_PORT} }$ "
else
	echo "Option -j JMX_PORT is a required option but is not present."
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
if [ "${CRITLEVEL}" != "" ]
then
	gensh_runtime_opts="${gensh_runtime_opts}string -to @critlevel -trim \${ ${CRITLEVEL} }$ "
else
	echo "Option -l CRITLEVEL is a required option but is not present."
	usage
	exit 2
fi
if [ "${CRITMSG}" != "" ]
then
	gensh_runtime_opts="${gensh_runtime_opts}string -to @critmsg -trim \${ ${CRITMSG} }$ "
else
	echo "Option -m CRITMSG is a required option but is not present."
	usage
	exit 2
fi

# Invocation
java -jar /opt/ublu/ublu.jar ${gensh_runtime_opts} include ${SILENT}/opt/ublu/examples/monitorJVMMem.ublu monitorIBMJVMMem \( @server @jmxport @password @critlevel @critmsg \) 
exit $?
