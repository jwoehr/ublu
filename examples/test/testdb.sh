# starfrom.sh -- connect to a db and SELECT * FROM a table

function usage () {
echo "$0 as400 | postgres system collection tablename username password"
}

if [ $# -ne 6 ]
then
	echo "$0 : Wrong number of arguments."
	usage
	exit 1
fi

DBTYPE=$1
SYSTEM=$2
COLLECTION=$3
TABLENAME=$4
USERNAME=$5
PASSWORD=$6

java -jar /opt/ublu/ublu.jar <<ENDENDEND
put -to @dbtype $DBTYPE
put -to @system $SYSTEM
put -to @dbname $COLLECTION
put -to @tablename $TABLENAME
put -to @username $USERNAME
put -to @password $PASSWORD
include dbstuff.os4
put \${ Connecting to $SYSTEM $COLLECTION }$
connectDb ( @db @dbtype @system @dbname @username @password )
put \${ Selecting * from $TABLENAME via a function }$
selectStarFrom ( @db @tablename )
put \${ Selecting * from $TABLENAME via an inline query }$
db -dbconnected @db -query \${ SELECT * FROM $TABLENAME }$
put \${ Done with db test }$
ENDENDEND
exit $?
# End

