#!/usr/bin/env bash
# This script is run by continuous integration server

set -eou pipefail

./script/lint.sh
./script/clj-tests.sh 1.9
./script/clj-tests.sh 1.10
./script/cljs-tests.sh --env node --optimizations none
./script/cljs-tests.sh --env chrome-headless --optimizations none
./script/cljs-tests.sh --env planck --optimizations none
