#!/bin/bash
 
for N in pegasus 
do
	cd $N
        echo "************************************"
	echo "************building " $N " *********"
        echo "************************************"
	gradle clean install copyLocal cleanEclipse eclipse
	cd ../
done


