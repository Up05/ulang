#! /bin/sh
clear
mvn compile exec:java -Dexec.args="test.u" -T 1C -q
