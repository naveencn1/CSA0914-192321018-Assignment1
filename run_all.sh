#!/usr/bin/env bash
# Compile the shared model once, then compile and run every task from the repository root.
# Requires a JDK (javac) 17 or newer.
set -e
rm -rf out && mkdir -p out/classes
javac -d out/classes $(find src -name "*.java")
run() {  # run <folder> <MainClass>
  mkdir -p out/$1
  javac -cp out/classes -d out/$1 $1/*.java
  echo "----- $1 : $2 -----"
  java -cp out/classes:out/$1 $2
}
run Task01 Task01Demo
run Task02 Task02Demo
run Task03 ParkingFacility
run Task04 Task04Demo
run Task05 Task05Demo
run Task06 Task06Demo
run Task07 Task07Demo
run Task08 FeeDemo
run Task08 DispatchDemo
run Task09 Task09Demo
run Task10 Task10Demo
run Task11 Task11Tests
run Task12 Task12Demo
mkdir -p out/Benchmarks && javac -d out/Benchmarks Benchmarks/Bench.java && echo "----- Benchmarks : Bench -----" && java -cp out/Benchmarks Bench
