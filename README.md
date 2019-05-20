# SAP JCO SDK
* 由于SAP官网需要购买过的用户才可登录下载SDK，目前网上可以找到比较全的全版本的SDK。 
* 全版本SDK版本：3.0.11-720.612  
* Linux有个比较新的版本：3.0.16
* 新添加3.0.17版本的Linux/Windows/Mac，但Mac下只有3.0.11，毕竟在生产环境Mac不会影响太大
# 官方下载
下载必须是SAP SMP (Market Place) valid account，也就是SAP管理员分配的子账号。
* [jco download](https://support.sap.com/en/product/connectors/jco.html)
## SDK配置（64位）
下载SDK：  
```shell
mkdir -p /data/service/jco-sdk
git clone https://github.com/easonjim/jco-sdk.git /data/service/jco-sdk
```
### Linux
把目录3.0.11-720.612/linuxx86_64/libsapjco3.so添加到LD_LIBRARY_PATH环境变量
```shell
# 设置
echo "export LD_LIBRARY_PATH=/data/service/jco-sdk/3.0.11-720.612/linuxx86_64/libsapjco3.so" >> /etc/profile
# 或（推荐此种方式）
cat > /etc/profile.d/jco.sh <<EOF
export LD_LIBRARY_PATH=/data/service/jco-sdk/3.0.11-720.612/linuxx86_64/libsapjco3.so
EOF
# 生效
source /etc/profile
```
### Mac
步骤类似，但文件夹需要指向darwinintel*(注意：系统为64位时要使用64位目录下的动态链接库)  
```shell
echo "export LD_LIBRARY_PATH=/data/service/jco-sdk/3.0.11-720.612/darwinintel64/libsapjco3.jnilib" >>/etc/profile
echo "export DYLD_LIBRARY_PATH=/data/service/jco-sdk/3.0.11-720.612/darwinintel64/libsapjco3.jnilib" >>/etc/profile
# 如果不行，可以设置为这个DYLD_LIBRARY_PATH，可能针对64位系统需要这个设置
```
### Linux&Mac针对Java 8+的配置
#### Java 7及以前
```shell
# Linux：
echo "export LD_LIBRARY_PATH=/data/service/jco-sdk/3.0.11-720.612/linuxx86_64/libsapjco3.so">>/etc/profile
# Mac
echo "export LD_LIBRARY_PATH=/data/service/jco-sdk/3.0.11-720.612/darwinintel64/libsapjco3.jnilib">>/etc/profile
echo "export DYLD_LIBRARY_PATH=/data/service/jco-sdk/3.0.11-720.612/darwinintel64/libsapjco3.jnilib" >>/etc/profile
```
#### Java 8+
```shell
# Linux
echo "export LD_LIBRARY_PATH=/data/service/jco-sdk/3.0.11-720.612/linuxx86_64/libsapjco3.so">>/etc/profile
# Mac
echo "export JAVA_LIBRARY_PATH=/data/service/jco-sdk/3.0.11-720.612/darwinintel64/libsapjco3.jnilib">>/etc/profile
```
#### Mac注意
* 针对Java 8+在Mac系统下，设置环境变量需要变更为：
    * LD_LIBRARY_PATH->JAVA_LIBRARY_PATH  
* 而针对Java 7及以前，Mac某些版本系统可能需要变更为（不是绝对，可以尝试）：
    * LD_LIBRARY_PATH->DYLD_LIBRARY_PATH
### Windows
将ntamd64/sapjco3.dll拷贝到c:/windows/system32与C:\Program Files (x86)\Java\jdk1.7.0_51\bin下
## SDK测试
```shell
java -jar /data/service/jco-sdk/3.0.11-720.612/sapjco3.jar
或者
java -classpath /data/service/jco-sdk/3.0.11-720.612/sapjco3.jar com.sap.conn.jco.rt.About
```
此时会出现对话框或者命令行的信息，注意类似以下信息即为成功，关心的信息在Versions，只要不为null即可。    
```shell
--------------------------------------------------------------------------------------
|                                 SAP Java Connector                                 |
|                Copyright (c) 2000-2016 SAP SE. All rights reserved.                |
|                                Version Information                                 |
--------------------------------------------------------------------------------------
Java Runtime:
 Operating System:       Linux 2.6.32-431.el6.x86_64 for amd64
 Java VM:                1.7.0_75 Oracle Corporation
 Default charset:        UTF-8
Versions:
 JCo API:                3.0.16 (2016-12-06)
 JCo middleware:         JavaRfc 2.2.19
 JCo library:            721.800
Library Paths:
 Path to JCo archive:    /opt/jdk1.7.0_75/lib/sapjco3.jar
 Path to JCo library:    /usr/local/tools/jco3/libsapjco3.so
--------------------------------------------------------------------------------------
|                                      Manifest                                      |
--------------------------------------------------------------------------------------
Manifest-Version: 1.0
Ant-Version: Apache Ant 1.6.4
Implementation-Title: com.sap.conn.jco
Implementation-Version: 20161207 2131 [3.0.16 (2016-12-06)]
Specification-Vendor: SAP SE, Walldorf
Specification-Title: SAP Java Connector v3
Implementation-Vendor-Id: com.sap
Created-By: 5.1.028 (SAP AG)
Specification-Version: 3.0.16
```

## 项目使用说明
### 使用传统项目，main方法
- 添加jar到项目目录
- 设置LD_LIBRARY_PATH
### 使用传统Web项目
- 添加jar到项目目录
- 添加动态链接库到Web容器的jre目录，比如Tomcat的jre目录下
### 使用hibersap组件（推荐使用这种方式）
* 官网：https://github.com/hibersap/hibersap  
* demo：http://hibersap.org/example/  
* demo code：https://github.com/hibersap/hibersap-example-simple  
#### 步骤：  
##### 1、安装sapjco3.jar到maven本地仓库：  
```shell
mvn install:install-file -DgroupId=org.hibersap -DartifactId=com.sap.conn.jco.sapjco3 -Dversion=3.0.11 -Dpackaging=jar -Dfile=/data/service/jco-sdk/3.0.11-720.612/sapjco3.jar
```
##### 2、项目引入  
```shell
# POM
        <dependency>
            <groupId>org.hibersap</groupId>
            <artifactId>hibersap-core</artifactId>
            <version>1.3.0</version>
        </dependency>
        <dependency>
            <groupId>org.hibersap</groupId>
            <artifactId>hibersap-jco</artifactId>
            <version>1.3.0</version>
        </dependency>
        <dependency>
            <groupId>org.hibersap</groupId>
            <artifactId>com.sap.conn.jco.sapjco3</artifactId>
            <version>3.0.11</version>
        </dependency>
```

## 注意
上面设置的变量都是Environment Property，而不能设置在System Property中，并且Java不能设置Environment，所以也就不能封装成Jar包，在启动时自动加载so文件，比如像这个项目：[sigar-loader](https://github.com/kamon-io/sigar-loader)  
因此，只能通过shell脚本进行环境变量配置。

## 自动配置脚本
注意：下面脚本默认用回为www-data，需要在实际使用中指定jco-sdk文件夹的用户，通过这个参数：--jco-dir-user="user1"
### Linux
```shell
curl -fsSL https://raw.githubusercontent.com/easonjim/jco-sdk/master/set-linux.sh | bash -s "linux set jco sdk env" 2>&1 | tee jco-sdk-set-linux.log
```
### Mac
```shell
curl -fsSL https://raw.githubusercontent.com/easonjim/jco-sdk/master/set-mac.sh | bash -s "mac set jco sdk env" 2>&1 | tee jco-sdk-set-mac.log
```