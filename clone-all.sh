#!/bin/bash
 
for N in opscommon opsari pegasus reztripsim siteminder sitemindersim
do
        echo "************************************"
	echo "************ pulling  " $N " *********"
        echo "************************************"
        git clone ssh://fletcherk@itstcb.com/home/gituser/repos/openpath/$N.git
	cd $N
	git checkout development
	cd ..
done
