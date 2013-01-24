lantern
======

Maven Plugin for java static analysis

To build
> `mvn package install``

To run with default rule:
> `mvn com.leozc:lantern:blacklist -Dlantern.rulefile="samplerule.json"`

It generates the scan result in latern.blacklist.out, it contains lines of records look like this:
> `commons-lang3-3.1.jar:SystemUtils.java:L0:org/apache/commons/lang3/SystemUtils.getSystemProperty@(Ljava/lang/String;)Ljava/lang/String;:calls java/io/PrintStream.void println(String)`

To include the plugin in you normal maven build process (mvn 3.01+), please put the follow sections in the plugins.<br/>
The first part is the maven dependencies tree, which we need that for actual work, second part is the actual configuration for the plugin.
>`
      <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-dependency-plugin</artifactId>
        <executions>
          <execution>
            <id>copy-dependencies</id>
            <phase>process-test-classes</phase>
            <goals>
              <goal>copy-dependencies</goal>
            </goals>
            <configuration>
              <outputDirectory>${project.build.directory}/dependency</outputDirectory>
              <overWriteReleases>false</overWriteReleases>
              <overWriteSnapshots>false</overWriteSnapshots>
              <overWriteIfNewer>true</overWriteIfNewer>
            </configuration>
          </execution>
        </executions>
      </plugin>
 
      <plugin>
        <groupId>com.leozc</groupId>
        <artifactId>lantern</artifactId>
        <executions>
          <execution>
            <phase>test</phase>
            <goals>
              <goal>findcaller</goal>
            </goals>
            <configuration>
                <!--rulefile>/media/JUNKBOX/leozc/workspace/tools/lantern/blacklist.json</rulefile-->
                <scandependency>true</scandependency>
            </configuration>
          </execution>
        </executions>
      </plugin>
`
You can use grep/cut and further process the result.

