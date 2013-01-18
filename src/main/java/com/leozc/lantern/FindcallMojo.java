package com.leozc.lantern;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.FileUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


@Mojo(name="findcaller")
public class FindcallMojo extends AbstractMojo {

    @Parameter(required = true,property = "lantern.rulefile")
    private String rulefile = null;
    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {

        MavenProject p = (MavenProject)this.getPluginContext().get("project");
        final String packagetype = p.getPackaging();
        if(!packagetype.equals("jar") && !packagetype.equals("war"))
        {
            getLog().info("Project packaging type "+p.getPackaging() +" unsupported, skipping....");
            return;
        }




        getLog().info("Rule => " + rulefile);

        /////////////////
        // PARSE THE RULES
        //////////////////
        File rule = new File(rulefile);
        List<RuleEntry> rules = new ArrayList<RuleEntry>();
        if(rule.exists()){
            try {
                Type listType = new TypeToken<ArrayList<RuleEntry>>() {
                }.getType();
                rules  = new Gson().fromJson(FileUtils.fileRead(rule),listType);

            } catch (IOException e) {
                throw new MojoFailureException("cannot read/parse "+rulefile + " ERROR "+e.getMessage());
            }
        }

        File dependancyPath = new File(p.getBuild().getDirectory()+"/dependency");
        File buildPath      = new File(p.getBuild().getDirectory());
        if(buildPath.exists() && dependancyPath.exists()){
            File[] scantargetPackages =  dependancyPath.listFiles(new FilenameFilter() {
                public boolean accept(File dir, String name) {
                    return name.toLowerCase().endsWith(".jar");
                }
            });
            File[] scantargetArtifact =  buildPath.listFiles(new FilenameFilter() {
                public boolean accept(File dir, String name) {
                    return name.toLowerCase().endsWith(packagetype);
                }
            });

            List<File> files = new ArrayList<File>();

            files.addAll(Arrays.asList(scantargetPackages));
            files.addAll(Arrays.asList(scantargetArtifact));



            if(scantargetPackages.length==0){
                throw new MojoFailureException("No jar found ? please run 'mvn dependency:copy-dependencies' to pull down the dependencies to target/dependency folder before running this plugin");
            }
            getLog().info("====== Found the dependency folder " + scantargetPackages.length + " scantargetPackages found.");
            getLog().info(scantargetPackages[0].getAbsolutePath());
            String outputfilename = "lantern.blacklist.out";
            FileWriter fw = null;

            try{
                fw = new FileWriter(outputfilename,false);
                for(RuleEntry r : rules){   // for each rule
                    for(File artifact:files) // each files
                        try {
                            MethodcallerMatcher app = new MethodcallerMatcher();
                            app.findCallingMethodsInJar(artifact.getAbsolutePath(),r.className,r.methodDescriptor);
                            for (MethodcallerMatcher.Callee c : app.getCallees()) {
                                String outStr = p.getArtifactId()+":"+artifact.getName()+":"+c.source+":L"+c.line+":"+c.className+"."+c.methodName+"@"+c.methodDesc + ":calls "+r.className+"."+r.methodDescriptor;

                                getLog().info("@@:" + c.source + ":L" + c.line + ":" + ":calls " + r.className);
                                fw.write(outStr+"\n");
                            }

                            getLog().info("@@" + app.getCallees().size() + " methods invoke " +
                                    app.getTargetClass() + " " +
                                    app.getTargetMethod().getName() + " " + app.getTargetMethod().getDescriptor());

                        } catch (Exception e) {
                            getLog().error("Fail to process"+artifact.getName(),e);
                        }
                }

                getLog().info(outputfilename + " is generated.");
            }   catch (IOException ioe){
                throw new MojoFailureException("Fail to write output log");
            }   finally {
               if(fw!=null)
                   try {
                       fw.close();
                   } catch (IOException e) {
                       e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                   }
            }

        }
        else{
            getLog().error("Cannnot find dependency folder");
            getLog().error("please run 'mvn dependency:copy-dependencies' to pull down the dependencies to target/dependency folder before running this plugin");
            throw new MojoFailureException(this,"fail to find target/dependency for "+ p.getArtifact().getArtifactId(),"run 'mvn dependency:copy-dependencies' ");
        }
    }



    protected class RuleEntry{
        String className;
        String methodDescriptor;
    }
}
