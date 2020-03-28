package com.bew.mojo.buildnumber.simple;

/**
 * The MIT License
 * <p>
 * Copyright (c) 2015 Learning Commons, University of Calgary
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sub-license, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
 * NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Calendar;
import java.util.Properties;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.IOUtil;

/**
 * This mojo is designed to give you a build number. So when you might make 100 builds of version 1.0-SNAPSHOT, you can
 * differentiate between them all.
 * <p>
 * The build number is taken from a file, "buildNumberPropertiesFileLocation", then incremented and written back
 * into that file.<br>
 * Build numbers are not automatically reflected in your artifact's filename, but can be added to the metadata. You can
 * access the build number in your pom with ${buildNumber}.
 * </p><p>
 * <b>NOTE:</b> The code in this file has been modified and cut-down from the original code found in the
 * "{@code buildNumber-maven-plugin}" project. I was not interested in any of the <i>SCM</i> related code, so that went.
 * Neither does the coding layout style conform to the maven preferred style. It is the default styling used in Netbeans 11.3.
 * </p>
 *
 * @author Bradley Willcott (28/03/2020)
 *
 * @author <a href="mailto:woodj@ucalgary.ca">Julian Wood</a>
 * @version $Id$
 */
@Mojo(name = "create", defaultPhase = LifecyclePhase.INITIALIZE, requiresProject = true, threadSafe = true)
public class CreateMojo extends AbstractMojo {

    /**
     * You can rename the buildNumber property name to another property name if desired.
     *
     * @since 1.0-beta-1
     */
    @Parameter(property = "maven.buildNumber.buildNumberPropertyName", defaultValue = "buildNumber")
    private String buildNumberPropertyName;

    /**
     * Set this to "true" to just keep using the current number in the external properties file.
     * Incrementing of the number will be disabled.
     */
    @Parameter(property = "maven.buildNumber.keepNumber", defaultValue = "false")
    private boolean keepNumber;

    /**
     * Properties file to be created.
     *
     * @since 1.0-beta-2
     */
    @Parameter(defaultValue = "${basedir}/buildNumber.properties")
    private File buildNumberPropertiesFileLocation;

    /**
     * If set to true, will get the build number once for the entire current build process.
     * Required for when using "maven.source.plugin:jar".
     */
    @Parameter(property = "maven.buildNumber.runOnce", defaultValue = "true")
    private boolean runOnce;

    /**
     * Whether to skip this execution.
     *
     * @since 1.3
     */
    @Parameter(property = "maven.buildNumber.skip", defaultValue = "false")
    private boolean skip;

    @Parameter(property = "project", required = true, readonly = true)
    private MavenProject project;

    // ////////////////////////////////////// internal variables ///////////////////////////////////
    private String revision;

    @Override
    public void execute()
            throws MojoExecutionException, MojoFailureException {
        if (skip) {
            getLog().info("Skipping execution.");
            return;
        }

        if (project != null) {
            // Check if the plugin has already run.
            revision = project.getProperties().getProperty(buildNumberPropertyName);

            if (runOnce && revision != null) {
                getLog().info("Revision available from previous execution: " + revision);
                return;
            }

            String now = Long.toString(Calendar.getInstance().toInstant().getEpochSecond());
            getLog().info("Time stamp: " + now);

            String s = buildNumberPropertyName;

            // check for properties file
            File propertiesFile = buildNumberPropertiesFileLocation;

            // create if not exists
            if (!propertiesFile.exists()) {
                try {
                    if (!propertiesFile.getParentFile().exists()) {
                        propertiesFile.getParentFile().mkdirs();
                    }

                    propertiesFile.createNewFile();
                } catch (IOException e) {
                    throw new MojoExecutionException("Couldn't create properties file: " + propertiesFile, e);
                }
            }

            Properties properties = new Properties();
            String buildNumberString = null;
            FileInputStream inputStream = null;
            FileOutputStream outputStream = null;

            try {
                // get the number for the buildNumber specified
                inputStream = new FileInputStream(propertiesFile);
                properties.load(inputStream);
                buildNumberString = properties.getProperty(s);

                if (buildNumberString == null) {
                    buildNumberString = "0";
                }

                int buildNumber = Integer.valueOf(buildNumberString);

                if (!keepNumber) {
                    // store the increment
                    properties.setProperty(s, String.valueOf(++buildNumber));
                    properties.setProperty("time", now);
                    outputStream = new FileOutputStream(propertiesFile);
                    properties.store(outputStream, "maven.buildNumber.plugin properties file");
                }

                revision = Integer.toString(buildNumber);
            } catch (NumberFormatException e) {
                throw new MojoExecutionException(
                        "Couldn't parse buildNumber in properties file to an Integer: "
                        + buildNumberString);
            } catch (IOException e) {
                throw new MojoExecutionException("Couldn't load properties file: " + propertiesFile, e);
            } finally {
                IOUtil.close(inputStream);
                IOUtil.close(outputStream);
            }

            if (revision != null) {
                project.getProperties().put(buildNumberPropertyName, revision);
                getLog().info(buildNumberPropertyName + ": " + revision);
            }
        }
    }
}
