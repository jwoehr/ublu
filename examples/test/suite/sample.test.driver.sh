# Sample test driver
RUNNAME=$1
COMMENT=$2
/opt/ublu/examples/test/suite/test.all.sh silent \
-Djavax.net.ssl.trustStore=/opt/ublu/keystores/ublutruststore \
-Djavax.net.ssl.keyStore=/opt/ublu/keystore/ubluserverstore \
-Djavax.net.ssl.keyStorePassword=xxxxxxxx \
-d ~/work/Ublu/Testing/test.defaults.varian.ublu \
-w ~/work/Ublu/Testing/workdir \
-k Y \
-i /QSYS.LIB/UBLUTEST.LIB/${RUNNAME}.FILE \
-c "${COMMENT}" \
2>&1 | tee ${RUNNAME}.txt