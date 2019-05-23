* 来自官方的包：https://mvnrepository.com/artifact/com.sap.conn.jco/sapjco3/3.0.14  
* 为什么不放在Maven中央仓，而是放在MIT Pub，我猜测主要是为了方便上传动态链接库，观察文件会发现所有文件都不包含GPG的签名文件.asc
* 不包含GPG验证文件不代表不是官方的包，观察文件内容可以发现已经包含md5的验证，并且此包被很多官方的包作为依赖引入
