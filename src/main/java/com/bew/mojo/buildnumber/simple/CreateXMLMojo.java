/*
 * Copyright (C) 2020 Bradley Willcott
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.bew.mojo.buildnumber.simple;

import java.io.*;
import java.util.Calendar;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import static java.util.regex.Pattern.MULTILINE;
import static org.apache.maven.plugins.annotations.LifecyclePhase.VALIDATE;

/**
 * Creates/Increments a build number for the project, and updates the "project.version" in the "pom.xml"
 * file for the project.
 * <p>
 * You must set the property: major.minor.version
 * </p><p>
 * For example, if your project was at version 1.0-SNAPSHOT,<br>
 * then you would set the following:
 * </p>
 * <pre><code>
 *
 *&lt;project ...&gt;
 *...
 *    &lt;properties&gt;
 *        &lt;major.minor.version&gt;1.0&lt;/major.minor.version&gt;
 *        ...
 *    &lt;/properties&gt;
 *...
 *    &lt;profiles&gt;
 *        &lt;profile&gt;
 *            &lt;!-- Increment {buildNumber} and update the 'pom.xml' file --&gt;
 *            &lt;id&gt;dev&lt;/id&gt;
 *            &lt;build&gt;
 *                &lt;plugins&gt;
 *                    &lt;plugin&gt;
 *                        &lt;groupId&gt;com.bew.mojo&lt;/groupId&gt;
 *                        &lt;artifactId&gt;simple-buildnumber-maven-plugin&lt;/artifactId&gt;
 *                        &lt;version&gt;1.0&lt;/version&gt;
 *                        &lt;executions&gt;
 *                            &lt;execution&gt;
 *                                &lt;id&gt;Update&lt;/id&gt;
 *                                &lt;goals&gt;
 *                                    &lt;goal&gt;create-xml&lt;/goal&gt;
 *                                &lt;/goals&gt;
 *                            &lt;/execution&gt;
 *                        &lt;/executions&gt;
 *                    &lt;/plugin&gt;
 *                &lt;/plugins&gt;
 *            &lt;/build&gt;
 *        &lt;/profile&gt;
 *        &lt;profile&gt;
 *            &lt;!-- Keep current {buildNumber}, remove '-SNAPSHOT' and update the 'pom.xml' file --&gt;
 *            &lt;id&gt;release&lt;/id&gt;
 *            &lt;build&gt;
 *                &lt;plugins&gt;
 *                    &lt;plugin&gt;
 *                        &lt;groupId&gt;com.bew.mojo&lt;/groupId&gt;
 *                        &lt;artifactId&gt;simple-buildnumber-maven-plugin&lt;/artifactId&gt;
 *                        &lt;version&gt;1.0&lt;/version&gt;
 *                        &lt;executions&gt;
 *                            &lt;execution&gt;
 *                                &lt;id&gt;Release&lt;/id&gt;
 *                                &lt;goals&gt;
 *                                    &lt;goal&gt;create-xml&lt;/goal&gt;
 *                                &lt;/goals&gt;
 *                                &lt;configuration&gt;
 *                                    &lt;release&gt;true&lt;/release&gt;
 *                                    &lt;keepNumber&gt;true&lt;/keepNumber&gt;
 *                                &lt;/configuration&gt;
 *                            &lt;/execution&gt;
 *                        &lt;/executions&gt;
 *                    &lt;/plugin&gt;
 *                &lt;/plugins&gt;
 *            &lt;/build&gt;
 *        &lt;/profile&gt;
 *        ...
 *    &lt;/profiles&gt;
 *...
 *&lt;/project&gt;
 *</code></pre>
 *
 * @author  <a href="mailto:bw.opensource@yahoo.com">Bradley Willcott</a>
 * @since 1.0
 * @version 1.0.9
 */
@Mojo(name = "create-xml", defaultPhase = VALIDATE, requiresProject = true,
      threadSafe = false, executionStrategy = "once-per-session")
public class CreateXMLMojo extends AbstractMojo {

    /**
     * The default name of the build number property.
     * Used to store number in the property file.
     */
    private static final String BUILDNUMBER = "buildNumber";

    /**
     * Define a static logger variable so that it references the
     * Logger instance named "CreateXMLMojoTest".
     */
    private static final Logger log = LogManager.getLogger();

    /**
     * You can rename the buildNumber property name to another property name if desired.
     */
    @Parameter(property = "simple.buildNumber.propertyName", defaultValue = BUILDNUMBER, readonly = true)
    private String buildNumberPropertyName;

    /**
     * Project output filename.
     */
    private String finalName;

    /**
     * Set this to "true" to just keep using the current number in the external properties file.
     * Incrementing of the number will be disabled.
     */
    @Parameter(defaultValue = "false")
    private boolean keepNumber;

    /**
     * The project's version as base: major.minor (eg: 1.0)
     * <p>
     * Add the new property to the project's "pom.xml":
     * </p><pre><code>
     * ...
     * &lt;properties&gt;
     *     &lt;major.minor.version&gt;1.0&lt;/major.minor.version&gt;
     *     ...
     * &lt;/properties&gt;
     * ...
     * </code></pre>
     */
    @Parameter(property = "major.minor.version", readonly = true)
    private String majorMinorVersion;

    /**
     * The number of spaces used to indent text.
     * <p>
     * TODO: This will go when I rewrite the updatePOM() method.
     */
    @Parameter(alias = "indentSpaces", property = "simple.buildNumber.indentSpaces", defaultValue = "4")
    private int numberOfIndentSpaces;

    /**
     * The old version text.
     */
    private String oldVersion;

    /**
     * The project's "pom.xml" file.
     */
    @Parameter(defaultValue = "${project.basedir}/pom.xml")
    private File pomFile;

    /**
     * Properties file to be created.
     */
    @Parameter(property = "simple.buildNumber.propertiesFilename",
               defaultValue = "${project.basedir}/buildNumber.properties", readonly = true)
    private File propertiesFileLocation;

    /**
     * If not set to <i>true</i>, then "-SNAPSHOT" will be appended to the value
     * to be set in "project.version" in the pom.xml file.
     */
    @Parameter(defaultValue = "false")
    private boolean release;

    // ////////////////////////////////////// internal variables ///////////////////////////////////
    /**
     * Whether to skip this execution.
     */
    @Parameter(defaultValue = "false")
    private boolean skip;

    /**
     * The maven project.
     *
     * @parameter property="project"
     * @readonly
     * @required
     */
    @Parameter(property = "project", readonly = true, required = true)
    private MavenProject theProject;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {

        logEntry();
        logTrace("simple.buildNumber.propertyName: {}", buildNumberPropertyName);
        logDebug("theProject: {}", theProject.toString());

        if (skip)
        {
            logInfo("Skipping execution.");
            return;
        }

        if (majorMinorVersion == null)
        {
            String msg1 = "ERROR: The 'major.minor.version' property has not been set.\n\n"
                          + "This is the project's version as base: major.minor (eg: 1.0)\n\n"
                          + "Add a new property to the project's 'pom.xml':\n\n"
                          + "<project ...>\n"
                          + "    ...\n"
                          + "    <properties>\n"
                          + "        <major.minor.version>1.0</major.minor.version>\n"
                          + "        ...\n"
                          + "    </properties>\n"
                          + "    ...\n"
                          + "</project>\n\n"
                          + "This text is NOT parsed in any way.  It is simply prepended\n"
                          + "to the generated 'build number'.\n";
            String msg2 = "WARNING: The pom's artifact's version text will be replaced.\n"
                          + "It will be replaced with the updated text which includes the\n"
                          + "'build number'.\n\n"
                          + "For example, with a buildNumber of '12':\n\n"
                          + "<project ...>\n"
                          + "    ...\n"
                          + "    <version>1.0.12-SNAPSHOT</version>\n"
                          + "    ...\n"
                          + "</project>\n\n"
                          + "Do NOT directly edit this as it WILL be replaced each time\n"
                          + "this plugin is run, unless <keepNumber>true</keepNumber> is\n"
                          + "configured.";
            logError(msg1);
            logWarn(msg2);
            throw new MojoFailureException("The 'major.minor.version' property has not been set.");
        }

        if (theProject != null)
        {
            // Get the timestamp.
            String now = Long.toString(Calendar.getInstance().toInstant().getEpochSecond());
            logDebug("Time stamp: " + now);

            // create if not exists
            if (!propertiesFileLocation.exists())
            {
                try
                {
                    if (!propertiesFileLocation.getParentFile().exists())
                    {
                        propertiesFileLocation.getParentFile().mkdirs();
                    }

                    propertiesFileLocation.createNewFile();
                } catch (IOException e)
                {
                    throw new MojoExecutionException("Couldn't create properties file: " + propertiesFileLocation, e);
                }
            }

            logTrace("Accessing properties file.");

            Properties properties = new Properties();
            String strBuildNumber = null;

            // get the number for the buildNumber specified
            try (FileInputStream inputStream = new FileInputStream(propertiesFileLocation))
            {
                properties.load(inputStream);

            } catch (IOException ex)
            {
                throw new MojoExecutionException("Couldn't load properties file: " + propertiesFileLocation, ex);
            }

            strBuildNumber = properties.getProperty(BUILDNUMBER);

            logTrace("{} : {}", BUILDNUMBER, strBuildNumber);

            if (strBuildNumber == null)
            {
                strBuildNumber = "0";
            }

            int buildNumber;

            try
            {
                buildNumber = Integer.parseInt(strBuildNumber);
            } catch (NumberFormatException e)
            {
                throw new MojoExecutionException(
                        "Couldn't parse buildNumber in properties file to an Integer: "
                        + strBuildNumber);
            }

            logTrace("keepNumber: {}", keepNumber);

            if (!keepNumber)
            {
                // Increment build number and store it in the property file.
                properties.setProperty(BUILDNUMBER, String.valueOf(++buildNumber));
                properties.setProperty("time", now);

                logTrace("Storing properties: {}", propertiesFileLocation);
                logDebug(properties.toString());

                try (FileOutputStream outputStream = new FileOutputStream(propertiesFileLocation))
                {
                    properties.store(outputStream, "simple-buildnumber-maven-plugin properties file");
                } catch (IOException ex)
                {
                    throw new MojoExecutionException("Couldn't save properties file: " + propertiesFileLocation, ex);
                }
            }

            logTrace("revision: {}", buildNumber);

            // Set the user set property  to the current/new version string.
            setProperty(buildNumberPropertyName, "" + buildNumber);

            if (!keepNumber)
            {
                logInfo("New " + buildNumberPropertyName + ": " + buildNumber + "\n");
            } else
            {
                logInfo("Keeping previous " + buildNumberPropertyName + ": " + buildNumber + "\n");
            }

            // Update 'project.version'
            String projectVersion = majorMinorVersion + "." + buildNumber + (release ? "" : "-SNAPSHOT");
            logDebug("[OLD] project.artifact().getVersion(): " + theProject.getArtifact().getVersion());
            theProject.getArtifact().selectVersion(projectVersion);
            logDebug("[NEW] project.artifact().getVersion(): " + theProject.getArtifact().getVersion() + "\n");
            logDebug("[OLD] project.getModel().getVersion(): " + theProject.getModel().getVersion());
            theProject.getModel().setVersion(projectVersion);
            logDebug("[NEW] project.getModel().getVersion(): " + theProject.getModel().getVersion() + "\n");

            logTrace("update project 'pom.xml' file");
            updatePOM(projectVersion);

            // Final name of files
            logDebug("[OLD] theProject.getBuild().getFinalName(): " + theProject.getBuild().getFinalName());
            finalName = theProject.getBuild().getFinalName();
            updateFinalName(projectVersion);
            theProject.getBuild().setFinalName(finalName);
            logDebug("[NEW] theProject.getBuild().getFinalName(): " + theProject.getBuild().getFinalName());
        }

        logExit();
    }

    private String getProperty(String property) {
        return theProject.getProperties().getProperty(property);
    }

    private void logDebug(String msg) {
        log.debug(msg);
    }

    private void logDebug(String message, Object... params) {
        log.debug(message, params);
    }

    private void logEntry(String msg) {
        log.traceEntry(msg);
    }

    private void logEntry() {
        log.traceEntry();
    }

    private void logEntry(String message, Object... params) {
        log.traceEntry(message, params);
    }

    private void logError(String msg) {
        log.error(msg);
    }

    private void logExit(String msg) {
        log.traceExit(msg);
    }

    private void logExit() {
        log.traceExit();
    }

    private void logFatal(String msg) {
        log.fatal(msg);
    }

    private void logInfo(String msg) {
        log.info(msg);
    }

    private void logTrace(String msg) {
        log.trace(msg);
    }

    private void logTrace(String message, Object... params) {
        log.trace(message, params);
    }

    private void logWarn(String msg) {
        log.warn(msg);
    }

    private void setProperty(String property, String value) {
        if (value != null)
        {
            theProject.getProperties().put(property, value);
        }
    }

    private void updateFinalName(String projectVersion) {
        logEntry("projectVersion: {}", projectVersion);

        String regex = "(?<text>" + oldVersion + ")";
        StringBuilder sb = new StringBuilder();

        Pattern p = Pattern.compile(regex);
        Matcher m = p.matcher(finalName);

        if (m.find())
        {
            m.appendReplacement(sb, projectVersion);
        } else
        {
            logWarn("WARNING: project.build.finalName - Not Updated");
        }

        m.appendTail(sb);
        logDebug("sb:\n" + sb);
        finalName = sb.toString();

        logExit();
    }

    /**
     * TODO: Replace this with a method that uses a full XML reader and writer.
     *
     * @param projectVersion
     *
     * @throws MojoFailureException
     * @throws MojoExecutionException
     */
    private void updatePOM(String projectVersion) throws MojoFailureException, MojoExecutionException {
        logEntry("projectVersion: {}", projectVersion);

        String regex = "^(?<lead>[ ]{" + numberOfIndentSpaces + "}\\<version\\>)(?<text>[^<]*)(?<tail>\\</version\\>[ ]*\\n)";
        StringBuilder pomText = new StringBuilder();
        StringBuilder sb = new StringBuilder();
        String input = "";

        logTrace("About to read in pom file: {}", pomFile);

        try (BufferedReader in = new BufferedReader(new FileReader(pomFile)))
        {
            while ((input = in.readLine()) != null)
            {
                pomText.append(input).append("\n");
            }
        } catch (IOException ex)
        {
            throw new MojoFailureException("pom.xml file input related error.", ex);
        }

        Pattern p = Pattern.compile(regex, MULTILINE);
        Matcher m = p.matcher(pomText.toString());

        if (m.find())
        {
            logDebug("Found: |" + m.group() + "|");
            oldVersion = m.group("text");
            logDebug("text: |" + oldVersion + "|");
            String replacement = m.group("lead") + projectVersion + m.group("tail");
            logDebug("replacement: |" + replacement + "|");
            m.appendReplacement(sb, replacement);
        } else
        {
            throw new MojoExecutionException("<version> tag: Not Found");
        }

        m.appendTail(sb);
        logDebug("sb:\n" + sb);

        try (PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(pomFile))))
        {
            out.print(sb);
        } catch (IOException ex)
        {
            throw new MojoFailureException("pom.xml file output related error.", ex);
        }

        logExit();
    }
}
