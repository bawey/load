#!/bin/bash
#goes through the log file in parent directory, finds all the subsystem names and generates EPL statements monitoring the subsystems' change
dest='../subsystemsStateChange.epl'
echo '/** autogenerated EPL file **/' > $dest

for SSYS in $(cat ../../log.out | grep 'field: [^6]*_STATE' -o | sort | uniq | grep '[A-Z_]*' -o | sed "s/_STATE//g" | xargs -I {} echo {})
do
	cat ./SSYS_stateChange.epl | sed "s/{SSYS}/${SSYS}/g" >> $dest
done
