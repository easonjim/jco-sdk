#!/bin/bash

# fix dir bug
cd `dirname $0`

# check root
if [[ "$(whoami)" = "root" ]]; then
    echo "please don't use root run this !" >&2
    exit 1
fi

# check java
if type -p java; then
    echo -e "java is installed!"
else 
    echo -e "java not install!"
    exit 1
fi

read -p "The test jco script use less, exit this test you can use [Ctrl + C] or [Q]; in the time you must [Enter] to next test." TEST_INPUT

# test
java -jar /data/service/jco-sdk/3.0.11-720.612/sapjco3.jar | less