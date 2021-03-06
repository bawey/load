#!/bin/bash

tables[1]="BU"
tables[2]="EVM"                            
tables[3]="EventProcessorLegend"
tables[4]="EventProcessorScalersLegend"
tables[5]="EventProcessorStatus"
tables[6]="FMMFEDDeadTime"
tables[7]="FMMInput"
tables[8]="FMMInputDetail"
tables[9]="FMMPartitionDeadTime"
tables[10]="FMMStatus"
tables[11]="HLTS_L1Scalers"
tables[12]="RU"
tables[13]="ResourceBroker"                 
tables[14]="StorageManagerPerformance"      
tables[15]="diskInfo"                       
tables[16]="fbo"                            
tables[17]="fetchstamps"                    
tables[18]="frlBxHisto"                     
tables[19]="frlWcHisto"                     
tables[20]="frlcontrollerCard"              
tables[21]="frlcontrollerLink"              
tables[22]="frlcontrollerStatus"            
tables[23]="frllinkdbmondata"               
tables[24]="gmt_cell_mon"                   
tables[25]="gmt_cell_scalers_act"           
tables[26]="gmt_cell_scalers_pas"           
tables[27]="gmt_cell_status"                
tables[28]="gt_cell_general"                
tables[29]="gt_cell_lumiseg"                
tables[30]="gt_cell_trigger_table"          
tables[31]="gt_cell_ttcpartitions"          
tables[32]="hltCpuUsage"                    
tables[33]="hostInfo"                       
tables[34]="jobcontrol"                     
tables[35]="l1ts_cell"                      
tables[36]="l1ts_dbjobs"                    
tables[37]="levelZeroFM_dynamic"            
tables[38]="levelZeroFM_static"             
tables[39]="levelZeroFM_subsys"             
tables[40]="unique_fetchstamp"      

for table in "${tables[@]}"
do
	echo "delete from ${table} where fetchstamp>=(select startTime from dump_starts where dumpNo=4);" >> queries.sql
done

