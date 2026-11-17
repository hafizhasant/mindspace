#!/bin/bash
DIR=$(dirname $(readlink -f $0))
export GRADLE_HOME=$DIR/gradle
export PATH=$GRADLE_HOME/bin:$PATH
gradle "$@"
