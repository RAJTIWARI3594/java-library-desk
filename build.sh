#!/bin/sh
set -eu
mkdir -p build
javac --release 17 -d build LibraryDesk.java
