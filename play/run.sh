#!/bin/sh
MVN_CMD='mvn'

while getopts "d" opt; do
    case "$opt" in
    d)
        MVN_CMD='mvnDebug'
    esac
done

$MVN_CMD play2:run \
-Dhttps.port=9443 \
-Dhttp.port=9000 

