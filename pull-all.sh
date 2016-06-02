#!/bin/bash
 
for N in opscommon opsari pegasus reztripsim siteminder sitemindersim
do
	cd $N
        echo "************************************"
	echo "************ pulling  " $N " *********"
        echo "************************************"
        git pull  	
	cd ../
done
