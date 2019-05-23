# SAP JCO SDK
* 由于SAP官网需要购买过的用户才可登录下载SDK，目前网上可以找到比较全的全版本的SDK。 
* 全版本SDK版本：3.0.11-720.612  
* Linux有个比较新的版本：3.0.16
* 新添加3.0.17版本的Linux/Windows/Mac，但Mac下只有3.0.11，毕竟在生产环境Mac不会影响太大
* 新增3.0.14全平台版本，官方Maven仓库下载的包：[MIT Pub](https://mvnrepository.com/artifact/com.sap.conn.jco/sapjco3/3.0.14)
# 官方下载
下载必须是SAP SMP (Market Place) valid account，也就是SAP管理员分配的子账号。
* 特别说明：本项目的包只限于测试，部署生产环境必须联系SAP管理员拿账号通过官方下载授权过的包
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
echo "export LD_LIBRARY_PATH=/data/service/jco-sdk/3.0.11-720.612/linuxx86_64" >> /etc/profile
# 或（推荐此种方式）
cat > /etc/profile.d/jco.sh <<EOF
export LD_LIBRARY_PATH=/data/service/jco-sdk/3.0.11-720.612/linuxx86_64
EOF
# 生效
source /etc/profile
```
### Mac
步骤类似，但文件夹需要指向darwinintel*(注意：系统为64位时要使用64位目录下的动态链接库)  
```shell
echo "export LD_LIBRARY_PATH=/data/service/jco-sdk/3.0.11-720.612/darwinintel64" >>/etc/profile
echo "export DYLD_LIBRARY_PATH=/data/service/jco-sdk/3.0.11-720.612/darwinintel64" >>/etc/profile
# 如果不行，可以设置为这个DYLD_LIBRARY_PATH，可能针对64位系统需要这个设置
```
### Linux&Mac针对Java 8+的配置
#### Java 7及以前
```shell
# Linux：
echo "export LD_LIBRARY_PATH=/data/service/jco-sdk/3.0.11-720.612/linuxx86_64">>/etc/profile
# Mac
echo "export LD_LIBRARY_PATH=/data/service/jco-sdk/3.0.11-720.612/darwinintel64">>/etc/profile
echo "export DYLD_LIBRARY_PATH=/data/service/jco-sdk/3.0.11-720.612/darwinintel64" >>/etc/profile
```
#### Java 8+
```shell
# Linux
echo "export LD_LIBRARY_PATH=/data/service/jco-sdk/3.0.11-720.612/linuxx86_64">>/etc/profile
# Mac
echo "export JAVA_LIBRARY_PATH=/data/service/jco-sdk/3.0.11-720.612/darwinintel64">>/etc/profile
```
#### Mac注意
* 针对Java 8+在Mac系统下，设置环境变量需要变更为：
    * LD_LIBRARY_PATH->JAVA_LIBRARY_PATH  
* 而针对Java 7及以前，Mac某些版本系统可能需要变更为（不是绝对，可以尝试）：
    * LD_LIBRARY_PATH->DYLD_LIBRARY_PATH
* Mac系统不需要设置libsapjco3.jnilib，只要设置根目录即可
### Windows
将ntamd64/sapjco3.dll拷贝到c:/windows/system32与C:\Program Files (x86)\Java\jdk1.7.0_51\bin下
## SDK测试
```shell
java -jar /data/service/jco-sdk/3.0.11-720.612/sapjco3.jar
或者
java -classpath /data/service/jco-sdk/3.0.11-720.612/sapjco3.jar com.sap.conn.jco.rt.About
```
此时会出现对话框或者命令行的信息，注意类似以下信息即为成功，关心的信息在Versions，只要不为null即可。另外下面的信息只是例子！    
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

## 注意
上面设置的变量都是Environment Property，而不能设置在System Property中，并且Java不能设置Environment，所以也就不能封装成Jar包，在启动时自动加载so文件，比如像这个项目：[sigar-loader](https://github.com/kamon-io/sigar-loader)  
因此，只能通过shell脚本进行环境变量配置。  
由于历史原因，上面说法是错误的（用力甩一巴掌打脸！），其实除了Environment Property，jcosap.jar还会去找java.library.path这个路径，所以在启动时设置这个路径就能成功加载，比如这样设置是成功的：System.setProperty("java.library.path", "/data/service/jco-sdk/3.0.11-720.612/linuxx86_64");，需要特别注意，设置java.library.path路径时远没有前面的一句话这么简单，还需要根据环境进行判断叠加路径，因此这里是一个思路。那么接下来就是封装一个jar包：jar-loader，功能实现类似上面的sigar-loader

## 自动配置脚本
注意：下面脚本默认用回为www-data，需要在实际使用中指定jco-sdk文件夹的用户，通过这个参数：--jco-dir-user="user1"
### Linux
```shell
curl -fsSL https://raw.githubusercontent.com/easonjim/jco-sdk/master/set-linux.sh | sudo bash -s -- --jco-dir-user="www-data" 2>&1 | tee jco-sdk-set-linux.log
```
### Mac
```shell
curl -fsSL https://raw.githubusercontent.com/easonjim/jco-sdk/master/set-mac.sh | sudo bash -s -- --jco-dir-user="jim" 2>&1 | tee jco-sdk-set-mac.log
```
### Windows(by cygwin)
```shell
curl -fsSL https://raw.githubusercontent.com/easonjim/jco-sdk/master/set-windows.sh | tee jco-sdk-set-windows.log
```

## 项目使用说明
### 一、使用传统项目，main方法
- 添加jar到项目目录
- 设置LD_LIBRARY_PATH环境变量
### 二、使用传统Web项目
- 添加jar到项目目录
- 添加动态链接库到Web容器的jre目录，比如Tomcat的jre目录下，或者设置LD_LIBRARY_PATH环境变量
- 其实我基本没操作过Tomcat，估计类似吧
### 三、使用hibersap组件（Spring Boot推荐使用这种方式）
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
##### 3、配置环境变量
```shell
# linux
curl -fsSL https://raw.githubusercontent.com/easonjim/jco-sdk/master/set-linux.sh | sudo bash -s -- --jco-dir-user="www-data" 2>&1 | tee jco-sdk-set-linux.log
# Mac
curl -fsSL https://raw.githubusercontent.com/easonjim/jco-sdk/master/set-mac.sh | sudo bash -s -- --jco-dir-user="jim" 2>&1 | tee jco-sdk-set-mac.log
```
### 四、快速对接方式
这种方式免除配置环境变量，并且不用安装本地依赖，打包运行都非常方便。  
原理：sapjco3.jar在没有配置环境变量时，会自动找当前目录下的动态链接库；以这条线索，只要把动态链接库拷贝到jar包的当前目录即可识别。  
假设已经clone项目到本地。
### 1、以纯Application为例
#### 配置本地依赖文件
```shell
# 拷贝sapjco3.jar、sapjco3.dll、sapjco3.pdb、libsapjco3.jnilib、libsapjco3.so到lib文件夹
```
#### 配置Maven
```shell
<dependency>
    <groupId>org.hibersap</groupId>
    <artifactId>com.sap.conn.jco.sapjco3</artifactId>
    <version>3.0.11</version>
    <scope>system</scope>
    <systemPath>${project.basedir}/lib/sapjco3.jar</systemPath>
</dependency>
```
通过以上配置，即可成功运行项目，但如果要打包成可运行的jar包，那么需要在POM增加如下插件
```xml
<!-- 解决class path和main class -->
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-jar-plugin</artifactId>
    <version>2.6</version>
    <configuration>
        <archive>
            <addMavenDescriptor>false</addMavenDescriptor>
            <manifestEntries>
                <Class-Path>lib/sapjco3.jar</Class-Path>
            </manifestEntries>
            <manifest>
                <addClasspath>true</addClasspath>
                <classpathPrefix>lib/</classpathPrefix>
                <mainClass>com.github.easonjim.demo.ConnTest</mainClass>
            </manifest>
        </archive>
    </configuration>
</plugin>
<!-- 复制lib文件 -->
<plugin>
    <artifactId>maven-resources-plugin</artifactId>
    <version>3.0.2</version>
    <executions>
        <execution>
            <id>copy-resources</id>
            <phase>validate</phase>
            <goals>
                <goal>copy-resources</goal>
            </goals>
            <configuration>
                <outputDirectory>${project.build.directory}/lib</outputDirectory>
                <resources>
                    <resource>
                        <directory>lib</directory>
                        <filtering>false</filtering>
                    </resource>
                </resources>
            </configuration>
        </execution>
    </executions>
</plugin>
```
此时打包出来的文件有jar包以及lib文件夹，直接拷贝这些文件到目标机器执行即可。
### 2、Web项目，Tomcat这种也是如此操作，其实我基本没操作过Tomcat，估计类似吧
### 3、Spring Boot项目的特殊处理，还是以hibersap组件为例
一般生产，纯手写JCO比较少，hibersap组件比较适合。
* 其实思路和上面的一样，先确定动态链接库的so文件放在哪里，然后再设置java.library.path路径指向
* 由于Spring Boot需要将so这样的动态链接库打包进去jar包，那么后续就会导致无法把这个路径指向java.library.path，因为jar包不能作为路径
* 解决思路是将so文件打包进去jar包，然后启动时，复制出所有的so文件到jar包所在文件夹，然后再把这个路径添加到java.library.path变量中
* 通过上述思路，其实实现是非常麻烦的，实现如下：
```shell
# 把lib文件夹复制到resources文件夹中，lib文件夹有：sapjco3.jar、sapjco3.dll、sapjco3.pdb、libsapjco3.jnilib、libsapjco3.so
# POM引入本地Jar包
<dependency>
    <groupId>org.hibersap</groupId>
    <artifactId>com.sap.conn.jco.sapjco3</artifactId>
    <version>3.0.11</version>
    <scope>system</scope>
    <systemPath>${project.basedir}/src/main/resources/lib/sapjco3.jar</systemPath>
</dependency>
# 然后在spring-boot-maven-plugin插件配置可以包含system级别的包
<plugin>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-maven-plugin</artifactId>
    <configuration>
        <includeSystemScope>true</includeSystemScope>
    </configuration>
</plugin>
# 在spring boot启动时复制出文件到当前jar包的lib文件夹上，并设置java.library.path，下面展示的是最粗糙的Mac专用代码，没有任何操作系统判断，只为了能运行
# 1、先获取resources里面的lib文件夹的libsapjco3.jnilib文件（Spring Boot专用）
ClassPathResource resource = new ClassPathResource("lib/libsapjco3.jnilib");
# 2、获取Spring Boot Jar包运行目录（注意：是目录）
ApplicationHome home = new ApplicationHome(Main.class);
File jarFile = home.getDir();
String path = jarFile.getPath();
# 3、从jar包通过inputStream的方式复制出来
FileUtils.copyInputStreamToFile(resource.getInputStream(),new File(path+"/native-lib/libsapjco3.jnilib"));
# 4、再把这个路径写入到java.library.path，那么此时就能正常用了
System.setProperty("java.library.path", path+"/native-lib");
```
* 上面的方式展示了Spring Boot的用法，总的感觉思路是对了，但是对接非常麻烦，jar包能找，并要求在本地，复制的过程要封装代码，太繁琐
* 所以总结了一下，不如做一个通用的jar和jar-loader放在中央仓库下，只需配置好之后就能全部完成上面的操作。像sigar-loader的方式一样
* 下面是我封装的项目jar包，直接中央仓库即可获取：
