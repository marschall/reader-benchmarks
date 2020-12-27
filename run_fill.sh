#!/bin/bash

export JAVA_HOME=${HOME}/tmp/JDK-4926314/patched/jdk

$JAVA_HOME/bin/java -cp target/benchmarks.jar --add-exports java.management/sun.management=ALL-UNNAMED --add-opens java.management/sun.management=ALL-UNNAMED -Djmh.blackhole.mode=COMPILER com.github.marschall.readerbenchmarks.ScannerTests

