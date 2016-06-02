#!/bin/bash
 
for N in siteminder 
do
	cd $N
        echo "************************************"
	echo "************building " $N " *********"
        echo "************************************"
	gradle clean install copyLocal cleanEclipse eclipse
	cd ../
done


