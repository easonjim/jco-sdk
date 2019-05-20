#!/bin/bash

# define env
JCO_DIR_USER="www-data"

# main
while [ "$1" != "${1##[-+]}" ]; do
  case $1 in
    --jco-dir-user)
           JCO_DIR_USER=$2
           shift 2
           ;;
    --jco-dir-user=?*)
           JCO_DIR_USER=${1#--jco-dir-user=}
           shift
           ;;
  esac
done

# fix dir bug
cd `dirname $0`

# check root
if [[ "$(whoami)" != "root" ]]; then
    echo "please run this script as root !" >&2
    exit 1
fi

# check git
if type -p git; then
    echo -e "git is install!"
else 
    echo -e "git not install! plase install use: yum install -y git"
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
echo -e "end set env..."

# set dir user
echo -e "start set dir:/data/service/jco-sdk user..."
if [ ${JCO_DIR_USER} = "www-data" ]; then
    echo -e "the dir set default user: www-data! you can use command arg --jco-dir-user=\"user1\" to setting!"
fi
echo -e "dir user is:"${JCO_DIR_USER}
chown -R ${JCO_DIR_USER}:${JCO_DIR_USER} /data/service/jco-sdk
echo -e "end set dir..."
