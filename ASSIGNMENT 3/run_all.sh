#!/bin/bash
# Compile and run every program. Requires JDK 17+ (tested with Java 21).  Usage: ./run_all.sh
set -e
mkdir -p out
javac -d out/q1  Q1/ATM.java Q1/UncaughtDemo.java Q1/ErrorCodeATM.java
javac -d out/q2a Q2/StorageDemo/StorageDemo.java
javac -d out/q2b Q2/ExamPortal/ExamPortal.java
javac -d out/q3  Q3/ReportGenerator.java
javac -d out/q4  Q4/Scanning.java Q4/ScanExecutor.java
javac -d out/q5  Q5/SeatBooking.java Q5/BookingExperiment.java Q5/StaticSyncDemo.java
javac -d out/q6  Q6/TokenCounter.java Q6/TokenCounterExperiments.java Q6/WalletTransfer.java Q6/AlternativeDesigns.java
javac -d out/q7  Q7/ResultProcessor.java
echo "== Q1 =="; java -cp out/q1 ATM; java -cp out/q1 ErrorCodeATM; java -cp out/q1 UncaughtDemo || true
echo "== Q2 =="; java -cp out/q2a StorageDemo; java -cp out/q2b ExamPortal
echo "== Q3 =="; java -cp out/q3 ReportGenerator
echo "== Q4 =="; java -cp out/q4 Scanning; java -cp out/q4 ScanExecutor
echo "== Q5 =="; java -cp out/q5 SeatBooking; java -cp out/q5 StaticSyncDemo; java -cp out/q5 BookingExperiment
echo "== Q6 =="; java -cp out/q6 TokenCounter; java -cp out/q6 TokenCounterExperiments; java -cp out/q6 WalletTransfer; java -cp out/q6 AlternativeDesigns
echo "== Q7 =="; java -cp out/q7 ResultProcessor test; java -cp out/q7 ResultProcessor bench; java -cp out/q7 ResultProcessor alt
