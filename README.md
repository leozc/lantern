lantern
======

Maven Plugin for java static analysis tool to find out all the callers calling a particular method.<br/>
The method you want to hunt is defined in latern.rulefile (defined by latern.rulefile or rulefile in mvn config).<br/>
There is a switch -Dlantern.includedependency (or rulefile in mvn config) to instruct the plugin to scan the dependencies of the project in additional to the project itself.


To build
> `mvn package install`

To run with default rule from CLI:
> `mvn com.leozc:lantern:blacklist -Dlantern.rulefile="samplerule.json"`

It generates the scan result in YOURARTIFACT.latern.*.out, it contains lines of records, ':' deminated, which is cut and grep friendly. (You can import into excel too.)
>`commons-lang3-3.1.jar:SystemUtils.java:L0:org/apache/commons/lang3/SystemUtils.getSystemProperty@(Ljava/lang/String;)Ljava/lang/String;:calls java/io/PrintStream.void println(String)`


To include the plugin in you normal maven build process (mvn 3.01+), please put the follow sections in the plugins.<br/>
The first part is the maven dependencies tree, which we need that for actual work, second part is the actual configuration for the plugin.
> <pre><code>  
	&lt;plugin&gt;
    &lt;groupId&gt;org.apache.maven.plugins&lt;/groupId&gt;
    &lt;artifactId&gt;maven-dependency-plugin&lt;/artifactId&gt;
    &lt;executions&gt;
      &lt;execution&gt;
        &lt;id&gt;copy-dependencies&lt;/id&gt;
        &lt;phase&gt;process-test-classes&lt;/phase&gt;
        &lt;goals&gt;
          &lt;goal&gt;copy-dependencies&lt;/goal&gt;
        &lt;/goals&gt;
        &lt;configuration&gt;
          &lt;outputDirectory&gt;${project.build.directory}/dependency&lt;/outputDirectory&gt;
          &lt;overWriteReleases&gt;false&lt;/overWriteReleases&gt;
          &lt;overWriteSnapshots&gt;false&lt;/overWriteSnapshots&gt;
          &lt;overWriteIfNewer&gt;true&lt;/overWriteIfNewer&gt;
        &lt;/configuration&gt;
      &lt;/execution&gt;
    &lt;/executions&gt;
  &lt;/plugin&gt;
</code></pre>

> <pre><code>
  &lt;plugin&gt;
    &lt;groupId&gt;com.leozc&lt;/groupId&gt;
    &lt;artifactId&gt;lantern&lt;/artifactId&gt;
    &lt;executions&gt;
      &lt;execution&gt;
        &lt;phase&gt;test&lt;/phase&gt;
        &lt;goals&gt;
          &lt;goal&gt;findcaller&lt;/goal&gt;
        &lt;/goals&gt;
        &lt;configuration&gt;
            &lt;!--rulefile&gt;/media/JUNKBOX/leozc/workspace/tools/lantern/blacklist.json&lt;/rulefile--&gt;
            &lt;scandependency&gt;true&lt;/scandependency&gt;
        &lt;/configuration&gt;
      &lt;/execution&gt;
    &lt;/executions&gt;
  &lt;/plugin&gt;
</code></pre>



