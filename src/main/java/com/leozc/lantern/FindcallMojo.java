package com.leozc.lantern;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.apache.commons.io.IOUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.FileUtils;

import java.io.*;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


@Mojo(name="findcaller")
public class FindcallMojo extends AbstractMojo {

    @Parameter(required = false,property = "lantern.rulefile",defaultValue = "")
    private String rulefile = "";
    @Parameter(required = false,property = "lantern.includedependency")
    private Boolean scandependency = true;
    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        int totalViolation = 0;
        int totalViolation_module = 0;
        MavenProject p = (MavenProject)this.getPluginContext().get("project");

        final String packagetype = p.getPackaging();
        if(!packagetype.equals("jar") && !packagetype.equals("war"))
        {
            getLog().warn("Project packaging type " + p.getPackaging() + " unsupported, skipping....");
            return;
        }






        /////////////////
        // PARSE THE RULES
        //////////////////

        List<RuleEntry> rules = parseRules();
        List<File> files = getArtifactsForScanning(p, packagetype);

        String outputfilename =  p.getArtifactId()+"_lantern.findcaller.out";
        FileWriter fw = null;

        try{
            fw = new FileWriter(outputfilename,false);
            for(RuleEntry r :rules){   // for each rule
                for(File artifact:files) // each files
                    try {
                        MethodcallerMatcher app = new MethodcallerMatcher();
                        boolean localViolation = false;
                        app.findCallingMethodsInJar(artifact.getAbsolutePath(),r.className,r.methodDescriptor);

                        for (MethodcallerMatcher.Callee c : app.getCallees()) {
                            String outStr = p.getArtifactId()+":"+artifact.getName()+":"+c.source+":L"+c.line+":"+c.className+"."+c.methodName+"@"+c.methodDesc + ":calls "+r.className+"."+r.methodDescriptor;
                            totalViolation++;
                            getLog().warn("@@:" + c.source + ":L" + c.line + ":" + ":calls " + r.className);
                            fw.write(outStr+"\n");
                            localViolation = true;
                        }
                        if(localViolation)
                            totalViolation_module++;
                        getLog().info("@@"+p.getArtifactId()+":"+artifact.getName()+":" + app.getCallees().size() + " methods invoke " +
                                app.getTargetClass() + " " +
                                app.getTargetMethod().getName() + " " + app.getTargetMethod().getDescriptor());

                    } catch (Exception e) {
                        getLog().error("=>Fail to process: "+artifact.getName(),e);
                    }
            }
            getLog().info("============================================");
            getLog().info("TOTAL Violated Module "+totalViolation_module);
            getLog().info("TOTAL Violation in method "+totalViolation);
            getLog().info(outputfilename + " is generated.");
            getLog().info("============================================");
        }   catch (IOException ioe){
            throw new MojoFailureException("Fail to write output log",ioe);
        }   finally {
           if(fw!=null)
               try {
                   fw.close();
               } catch (IOException e) {
                   e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
               }
        }

        }

    private List<File> getArtifactsForScanning(MavenProject p, final String packagetype) throws MojoFailureException {
        List<File> files = new ArrayList<File>();
        File dependancyPath = new File(p.getBuild().getDirectory()+"/dependency");
        File buildPath      = new File(p.getBuild().getDirectory());
        if(buildPath.exists()  && (!this.scandependency || dependancyPath.exists() )){
            File[] scantargetPackages =  dependancyPath.listFiles(new FilenameFilter() {
                public boolean accept(File dir, String name) {
                    return name.toLowerCase().endsWith(".jar");
                }
            });
            if (!scandependency)
                scantargetPackages=new File[0];

            File[] scantargetArtifact =  buildPath.listFiles(new FilenameFilter() {
                public boolean accept(File dir, String name) {
                    getLog().warn(dir.getAbsolutePath()+File.separator +name  +" packageType = "+packagetype +"  isfile=>"+(new File(dir.getAbsolutePath()+File.separator +name)).isFile());
                    return (new File(dir.getAbsolutePath()+File.separator +name)).isFile() && name.toLowerCase().endsWith(packagetype);
                }

            });
            getLog().info("scantargetArtifact=>"+scantargetArtifact.length);
            getLog().info("scantargetPackages=>"+scantargetPackages.length);



            files.addAll(Arrays.asList(scantargetPackages));
            files.addAll(Arrays.asList(scantargetArtifact));



            if(files.size()==0 ){
                throw new MojoFailureException("No jar/war found ? please run 'mvn dependency:copy-dependencies' to pull down the dependencies to target/dependency folder before running this plugin");
            }
            getLog().info("====== Found the dependency folder " + scantargetPackages.length + " scantargetPackages found.");

            }else{
                getLog().error("Cannnot find dependency folder");
                getLog().error("please run 'mvn dependency:copy-dependencies' to pull down the dependencies to target/dependency folder before running this plugin");
                throw new MojoFailureException(this,"fail to find target/dependency for "+ p.getArtifact().getArtifactId(),"run 'mvn dependency:copy-dependencies' ");
            }
        return files;
    }



    private List<RuleEntry> parseRules() throws MojoFailureException {
        File rule = null;
        List<RuleEntry> rules = new ArrayList<RuleEntry>();
        if(rulefile.length()==0)
        {

            try {

                InputStream is = getClass().getResourceAsStream("/defaultrules.json");
                String jsonStream = String.valueOf(IOUtils.toCharArray(is, "UTF8"));
                Type listType = new TypeToken<ArrayList<RuleEntry>>() { }.getType();
                rules  = new Gson().fromJson(jsonStream,listType);

            } catch (IOException e) {
                throw new MojoFailureException("cannot read/parse "+rulefile + " ERROR "+e.getMessage());
            }
        }
        else{
            rule = new File(rulefile);
            if(rule.exists()){
                try {
                    Type listType = new TypeToken<ArrayList<RuleEntry>>() { }.getType();
                    rules  = new Gson().fromJson(FileUtils.fileRead(rule),listType);

                } catch (IOException e) {
                    throw new MojoFailureException("cannot read/parse "+rulefile + " ERROR "+e.getMessage());
                }
            }
        }
        return rules;
    }


    protected class RuleEntry{
        String className;
        String methodDescriptor;
    }
}
