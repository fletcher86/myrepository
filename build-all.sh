#!/bin/bash
 
for N in opscommon 
do
	cd $N
        echo "************************************"
	echo "************building " $N " *********"
        echo "************************************"
	gradle clean proto install copyLocal cleanEclipse eclipse
	cd ../
done


for N in  opsari pegasus reztripsim siteminder sitemindersim
do
	cd $N
        echo "************************************"
	echo "************building " $N " *********"
        echo "************************************"
	gradle clean install copyLocal cleanEclipse eclipse
	cd ../
done
