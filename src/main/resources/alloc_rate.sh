#!/bin/bash

grep gc.alloc.rate 4926314-before-jmh-result.csv           | grep -v norm > 4926314-off-before-jmh-result-alloc-rate.csv
grep gc.alloc.rate 4926314-after-jmh-result.csv            | grep -v norm > 4926314-after-jmh-result-alloc-rate.csv
grep gc.alloc.rate 4926314-no-streamdecoder-jmh-result.csv | grep -v norm > 4926314-no-streamdecoder-jmh-result-alloc-rate.csv
grep gc.alloc.rate 4926314-off-on-heap-jmh-result.csv      | grep -v norm > 4926314-off-on-heap-jmh-result-alloc-rate.csv

