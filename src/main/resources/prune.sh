#!/bin/bash

grep -v gc 4926314-before-jmh-result.csv           > 4926314-off-before-jmh-result-pruned.csv
grep -v gc 4926314-after-jmh-result.csv            > 4926314-after-jmh-result-pruned.csv
grep -v gc 4926314-no-streamdecoder-jmh-result.csv > 4926314-no-streamdecoder-jmh-result-pruned.csv
grep -v gc 4926314-off-on-heap-jmh-result.csv      > 4926314-off-on-heap-jmh-result-pruned.csv

