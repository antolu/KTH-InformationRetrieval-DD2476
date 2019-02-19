#!/bin/sh
if ! [ -d build ];
then
   mkdir build
fi
javac -cp . -d build -g ir/*.java
