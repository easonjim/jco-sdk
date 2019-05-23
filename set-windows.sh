#!/bin/bash
# run this script by cygwin! so, you must install sygwin. 

# fix dir bug
cd `dirname $0`

# check git
if type -p git; then
    echo -e "git is install!"
else 
    echo -e "git not install! please install git!"
    exit 1
fi

# create dir 
echo -e "start create dir:/data/service/jco-sdk ..."
mkdir -p /data/service/jco-sdk
echo -e "end create dir..."

# clone project
echo -e "start clone project to /data/service/jco-sdk ..."
git clone https://github.com/easonjim/jco-sdk.git /data/service/jco-sdk
echo -e "end clone project..."

# init env
echo -e "start set env..."
cp /data/service/jck-sdk/3.0.11-720.612/ntamd64/sapjco3.dll /cygdrive/c/Windows/System32/
echo -e "end set env..."