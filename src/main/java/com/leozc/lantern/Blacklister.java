package com.leozc.lantern;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.sun.org.apache.bcel.internal.classfile.StackMapEntry;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.util.FileUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;


@Mojo(name="blacklist")
public class Blacklister extends AbstractMojo {

    @Parameter(required = true,property = "latern.rulefile")
    private String rulefile = null;
    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        File f = new File("target/dependency");
        File rule = new File(rulefile);
        List<RuleEntry> rules = new ArrayList<RuleEntry>();

        System.out.println("Rule => "+ rulefile);
//        for(Object e: this.getPluginContext().keySet()){
//            System.out.println(e + " => "+ this.getPluginContext().get(e));
//        }
//
        if(rule.exists()){
            try {
                Type listType = new TypeToken<ArrayList<RuleEntry>>() {
                }.getType();
                rules  = new Gson().fromJson(FileUtils.fileRead(rule),listType);

            } catch (IOException e) {
                throw new MojoFailureException("cannot read/parse "+rulefile + " ERROR "+e.getMessage());
            }
        }


        if(f.exists()){
            File[] scantargetJars =  f.listFiles(new FilenameFilter() {
                public boolean accept(File dir, String name) {
                    return name.toLowerCase().endsWith(".jar");
                }
            });
            if(scantargetJars.length==0){
                throw new MojoFailureException("No jar found ? please run 'mvn dependency:copy-dependencies' to pull down the dependencies to target/dependency folder before running this plugin");
            }
            System.out.println("====== Found the dependency folder " + scantargetJars.length + " scantargetJars found.");
            System.out.println(scantargetJars[0].getAbsolutePath());
            String outputfilename = "lantern.blacklist.out";
            try{
                FileWriter fw = new FileWriter(outputfilename,false);

                for(RuleEntry r : rules){
                    for(File jarpath:scantargetJars)
                        try {
                            MethodcallerMatcher app = new MethodcallerMatcher();
                            app.findCallingMethodsInJar(jarpath.getAbsolutePath(),r.className,r.methodDescriptor);
                            for (MethodcallerMatcher.Callee c : app.getCallees()) {
                                String outStr = jarpath.getName()+":"+c.source+":L"+c.line+":"+c.className+"."+c.methodName+"@"+c.methodDesc + ":calls "+r.className+"."+r.methodDescriptor;


                                System.out.println("@@:"+ c.source+":L"+c.line+":"+":calls "+r.className);
                                fw.write(outStr+"\n");
                            }
                            System.out.println("@@\n"+app.getCallees().size()+" methods invoke "+
                                    app.getTargetClass()+" "+
                                    app.getTargetMethod().getName()+" "+app.getTargetMethod().getDescriptor());

                        } catch (Exception e) {
                            System.err.println();
                        }
                }
                System.out.println(outputfilename +" is generated.");
            }   catch (IOException ioe){
                throw new MojoFailureException("Fail to write output log");
            }

        }
        else{
            System.out.println("======= NO");
            throw new MojoFailureException("please run 'mvn dependency:copy-dependencies' to pull down the dependencies to target/dependency folder before running this plugin");
        }
    }



    protected class RuleEntry{
        String className;
        String methodDescriptor;
    }
}
