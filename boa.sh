#!/bin/bash
 
for N in opsari 
do
	cd $N
        echo "************************************"
	echo "************building " $N " *********"
        echo "************************************"
	gradle clean install copyLocal #cleanEclipse eclipse
	cd ../
done


