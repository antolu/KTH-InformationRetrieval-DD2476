#!/bin/bash

if ! [ -d build ];
then
   mkdir build
fi

javac -g -cp . -d build pagerank/*.java montecarlo/*.java

java -Xmx2G -cp build montecarlo.MonteCarlo data/linksDavis.txt