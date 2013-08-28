#!/bin/bash

export JAR_PATH=/home/bawey/.m2/repository/
for file in $(find $JAR_PATH -name "*jar"); do
	if [[ -z "$CLASSPATH" ]]; then
		export CLASSPATH=${file}
	else
		export CLASSPATH=${file}:${CLASSPATH}
	fi
done

java org.apache.axis.wsdl.WSDL2Java http://localhost:10000/rcms/services/ParameterController?wsdl -p testMe

# how to generically map anything between http and port to sth?
#--NStoPkg urn:AddressFetcher2=samples.addr
