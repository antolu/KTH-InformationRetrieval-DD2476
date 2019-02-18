#!/bin/sh
if ! [ -d build ];
then
   mkdir build
fi
javac -cp . -d build ir/*.java pagerank/*.java
