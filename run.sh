#!/bin/bash

export JAVA_HOME=${HOME}/tmp/JDK-4926314/unpatched/jdk
$JAVA_HOME/bin/java -cp target/benchmarks.jar -Djmh.blackhole.mode=COMPILER com.github.marschall.readerbenchmarks.Main 4926314-before

export JAVA_HOME=${HOME}/tmp/JDK-4926314/patched/jdk
$JAVA_HOME/bin/java -cp target/benchmarks.jar -Djmh.blackhole.mode=COMPILER com.github.marschall.readerbenchmarks.Main 4926314-after

