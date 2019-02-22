#!/bin/sh
if ! [ -d build ];
then
   mkdir build
fi

javac -g -cp . -d build ir/*.java pagerank/*.java montecarlo/*.java
