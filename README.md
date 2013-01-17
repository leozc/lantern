latern
======

Maven Plugin for java static analysis

To run with default rule:
mvn com.leozc:lantern:blacklist -Dlatern.rulefile="samplerule.json"


It generates the scan result in latern.blacklist.out, it contains lines of records look like this:
commons-lang3-3.1.jar:SystemUtils.java:L0:org/apache/commons/lang3/SystemUtils.getSystemProperty@(Ljava/lang/String;)Ljava/lang/String;:calls java/io/PrintStream.void println(String)


You can use grep/cut and further process the result.

