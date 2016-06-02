#!/bin/bash


for N in  opscommon pegasus opsari reztripsim siteminder sitemindersim
do
	cd $N
        echo "************************************"
	echo "************building " $N " *********"
        echo "************************************"
	gradle cleanEclipse eclipse
	cd ../
done

 
