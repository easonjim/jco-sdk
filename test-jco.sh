#!/bin/bash

# fix dir bug
cd `dirname $0`

# check root
if [[ "$(whoami)" != "root" ]]; then
    echo "please run this script as root !" >&2
    exit 1
fi

# check java
if type -p java; then
    type -p java
else 
    echo -e "java not install!"
    exit 1
fi

# test
java -jar /data/service/jco-sdk/3.0.11-720.612/sapjco3.jar