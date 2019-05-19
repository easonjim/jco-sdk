#!/bin/bash

# fix dir bug
cd `dirname $0`

# check root
if [[ "$(whoami)" != "root" ]]; then
    echo "please run this script as root !" >&2
    exit 1
fi

# check git
if type -p git; then
    type -p git
else 
    echo -e "git not install!"
    exit 1
fi

# create dir
mkdir -p /data/service/jco-sdk

# clone project
git clone https://github.com/easonjim/jco-sdk.git /data/service/jco-sdk

# init env
if [[ `grep -c "LD_LIBRARY_PATH" /etc/profile` = 0 ]]; then
    echo "export LD_LIBRARY_PATH=/data/service/jco-sdk/3.0.11-720.612/darwinintel64/libsapjco3.jnilib">>/etc/profile    
else
    echo "LD_LIBRARY_PATH is set! please delete this env and set again!"
fi
if [[ `grep -c "DYLD_LIBRARY_PATH" /etc/profile` = 0 ]]; then
    echo "export DYLD_LIBRARY_PATH=/data/service/jco-sdk/3.0.11-720.612/darwinintel64/libsapjco3.jnilib" >>/etc/profile        
else
    echo "DYLD_LIBRARY_PATH is set! please delete this env and set again!"
fi
if [[ `grep -c "JAVA_LIBRARY_PATH" /etc/profile` = 0 ]]; then
    echo "export JAVA_LIBRARY_PATH=/data/service/jco-sdk/3.0.11-720.612/darwinintel64/libsapjco3.jnilib">>/etc/profile
else
    echo "JAVA_LIBRARY_PATH is set! please delete this env and set again!"
fi
# 生效
source /etc/profile