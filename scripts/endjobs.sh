#!/bin/bash
# End all interactive active jobs immediate with jobs excluded from ending
function usage() {
cat >&2 << ENDENDEND
Usage:
    $0 -h (for help)
        -or-
    $0 [-x excluded_user_id -x excluded_user_id ..] [-j /fully/qualified/jardirpath] system userid password  
    
End all interactive active jobs immediate with supplied user ids excluded from the mass ending.

The default jardirpath is /opt/api-java

Exit values:
    0   -   Correct execution or -h invoked at head of options
    1   -   -h found later in list of options
    2   -   wrong number of arguments to the command (see usage above) or error in getopts
ENDENDEND
}

# Check for help flag
if [ "$1" == "-h" ]
then
    usage
    exit 0
fi

# Preset defaults
JARDIR=/opt/ublu
EXCLUDED=""

# Process options
options=`getopt hj:x: $*`
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
    	-h)
            usage;exit 1
            ;;
        -j)
            JARDIR=$2;shift;
            shift;;
        -x)
            EXCLUDED="${EXCLUDED} $2";shift;
            shift;;
        --)
            shift; break;;
    esac
done

if [ $# -ne 3 ]
	then
		usage;
		exit 2
	fi
	
# Set credentials from script arguments
SYSTEM=$1
USERID=$2
PASSWD=$3

echo "Operating on ${SYSTEM} on behalf of user ${USERID}."
# echo "java -jar ${JARDIR}/ublu.jar"; cat <<EOF
java -jar ${JARDIR}/ublu.jar <<EOF

put -to @excluded \${ ${EXCLUDED} }$

FUNC showJobs ( joblist jobtype1 jobtype2 ) \$[
    FOR @j in @@joblist \$[
        put -n -from @@jobtype1 put -n -from @@jobtype2 put -n \${ job }$
        put -n -s -from @j
        put -n -s \${ is owned by }$ job -job @j -get user
    ]$
]$

FUNC endJobs ( joblist excluded ) \$[
    FOR @j in @@joblist \$[
        job -job @j -get user -to @jobuser
        FOR @user in @@excluded \$[
            test -to @isExcluded -eq @jobuser @user
            IF @isExcluded THEN \$[ BREAK ]$
        ]$
        IF @isExcluded THEN \$[
            put -n \${ found excluded user }$ put -n -s -from @jobuser put -n \${ in job }$ put -from @j
            ]$ ELSE \$[
            put -n \${ ending job immediate }$ put -from @j
            # put \${ ha ha didn't really end job just testing }$
            job -job @j -end -0
        ]$
    ]$
]$

as400 -to @as400 ${SYSTEM} ${USERID} ${PASSWD}
put \${ Getting list of all active interactive jobs }$
joblist -to @joblist -jobtype INTERACTIVE -active -as400 @as400
put \${ Showing all active interactive jobs }$
put -to @jobtype1 \${ active }$
put -to @jobtype2 \${ interactive }$
showJobs ( @joblist @jobtype1 @jobtype2 )
put -n \${ Ending immediate all active interactive jobs except excluded users: }$
put -from @excluded
endJobs ( @joblist @excluded )

put \${ Getting list of all disconnected interactive jobs }$
joblist -to @joblist -jobtype INTERACTIVE -disconnected -as400 @as400
put \${ Showing all disconnected interactive jobs }$
put -to @jobtype1 \${ disconnected }$
put -to @jobtype2 \${ interactive }$
showJobs ( @joblist @jobtype1 @jobtype2 )
put -n \${ Ending immediate all disconnected interactive jobs except excluded users: }$
put -from @excluded
endJobs ( @joblist @excluded )
put \${ Done. }$
EOF
exit 0