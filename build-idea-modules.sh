#!/bin/bash


for N in  opscommon pegasus opsari reztripsim
do
	cd $N
        echo "************************************"
	echo "************building " $N " *********"
        echo "************************************"
	gradle cleanIdeaModule ideaModule
	cd ../
done

 
