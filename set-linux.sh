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
cat > /etc/profile.d/jco.sh <<EOF
export LD_LIBRARY_PATH=/data/service/jco-sdk/3.0.11-720.612/linuxx86_64/libsapjco3.so
EOF
# 生效
source /etc/profile