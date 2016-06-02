#!/bin/bash
 
for N in sitemindersim 
do
	cd $N
        echo "************************************"
	echo "************building " $N " *********"
        echo "************************************"
	gradle clean install copyLocal #cleanEclipse eclipse
	cd ../
done


