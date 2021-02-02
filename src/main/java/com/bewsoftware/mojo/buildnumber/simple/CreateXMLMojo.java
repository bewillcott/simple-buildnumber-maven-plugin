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
package com.bewsoftware.mojo.buildnumber.simple;

import java.io.*;
import java.util.Calendar;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import static java.util.regex.Pattern.MULTILINE;
import static org.apache.maven.plugins.annotations.LifecyclePhase.VALIDATE;

/**
 * The plugin Creates/Increments a build number for the project, and updates the "&lt;project.version&gt;"
 * in the "<i>pom.xml</i>" file for the project. Simple. So is setting up.
 * <h2>Setup</h2>
 * You must set the property: &lt;major.minor.version&gt;<br>
 * For example, if your project was at version "1.0-SNAPSHOT",<br>
 * then you would set the following:
 * <pre><code>
 *&lt;project ...&gt;
 *...
 *  &lt;properties&gt;
 *    &lt;major.minor.version&gt;1.0&lt;/major.minor.version&gt;
 *    ...
 *  &lt;/properties&gt;
 *  ...
 *  &lt;profiles&gt;
 *    &lt;profile&gt;
 *      &lt;!-- Increment {buildNumber} and update the 'pom.xml' file --&gt;
 *      &lt;id&gt;increment-build&lt;/id&gt;
 *      &lt;build&gt;
 *        &lt;plugins&gt;
 *          &lt;plugin&gt;
 *            &lt;groupId&gt;com.bew.mojo&lt;/groupId&gt;
 *            &lt;artifactId&gt;simple-buildnumber-maven-plugin&lt;/artifactId&gt;
 *            &lt;version&gt;1.0&lt;/version&gt;
 *            &lt;executions&gt;
 *              &lt;execution&gt;
 *                &lt;id&gt;Increment Build&lt;/id&gt;
 *                &lt;goals&gt;
 *                  &lt;goal&gt;create-xml&lt;/goal&gt;
 *                &lt;/goals&gt;
 *              &lt;/execution&gt;
 *            &lt;/executions&gt;
 *          &lt;/plugin&gt;
 *        &lt;/plugins&gt;
 *      &lt;/build&gt;
 *    &lt;/profile&gt;
 *      &lt;profile&gt;
 *        &lt;id&gt;increment-release&lt;/id&gt;
 *        &lt;build&gt;
 *          &lt;plugins&gt;
 *            &lt;!-- Increment {buildNumber}, remove '-SNAPSHOT' and update the 'pom.xml' file --&gt;
 *            &lt;plugin&gt;
 *              &lt;groupId&gt;com.bew.mojo&lt;/groupId&gt;
 *              &lt;artifactId&gt;simple-buildnumber-maven-plugin&lt;/artifactId&gt;
 *              &lt;executions&gt;
 *                &lt;execution&gt;
 *                  &lt;id&gt;Increment Release&lt;/id&gt;
 *                  &lt;goals&gt;
 *                    &lt;goal&gt;create-xml&lt;/goal&gt;
 *                  &lt;/goals&gt;
 *                  &lt;configuration&gt;
 *                    &lt;release&gt;true&lt;/release&gt;
 *                  &lt;/configuration&gt;
 *                &lt;/execution&gt;
 *              &lt;/executions&gt;
 *            &lt;/plugin&gt;
 *         &lt;/plugins&gt;
 *      &lt;/build&gt;
 *    &lt;/profile&gt;
 *    &lt;profile&gt;
 *      &lt;!-- Keep current {buildNumber}, remove '-SNAPSHOT' and update the 'pom.xml' file --&gt;
 *      &lt;id&gt;release&lt;/id&gt;
 *      &lt;build&gt;
 *        &lt;plugins&gt;
 *          &lt;plugin&gt;
 *            &lt;groupId&gt;com.bew.mojo&lt;/groupId&gt;
 *            &lt;artifactId&gt;simple-buildnumber-maven-plugin&lt;/artifactId&gt;
 *            &lt;version&gt;1.0&lt;/version&gt;
 *            &lt;executions&gt;
 *              &lt;execution&gt;
 *                &lt;id&gt;Release&lt;/id&gt;
 *                &lt;goals&gt;
 *                  &lt;goal&gt;create-xml&lt;/goal&gt;
 *                &lt;/goals&gt;
 *                &lt;configuration&gt;
 *                  &lt;release&gt;true&lt;/release&gt;
 *                  &lt;keepNumber&gt;true&lt;/keepNumber&gt;
 *                &lt;/configuration&gt;
 *              &lt;/execution&gt;
 *            &lt;/executions&gt;
 *          &lt;/plugin&gt;
 *        &lt;/plugins&gt;
 *      &lt;/build&gt;
 *    &lt;/profile&gt;
 *    ...
 *  &lt;/profiles&gt;
 *...
 *&lt;/project&gt;
 * </code></pre>
 * (<i>By the way. You could be really lazy, and just copy the relevant sections from above and paste them</i>
 * <i>directly into your <code>pom.xml</code> file.</i>)
 * <p>
 * When you run a profile other than one of the above, then nothing happens to the <code>&lt;project.version&gt;</code> setting.
 * So, to increment the build number, select the profile: <code>increment-build</code>.
 * <p>
 * Then go back to your normal development profile.
 * <p>
 * When you are ready to release a version:
 * <ul>
 * <li>with a new build number, either:
 * <ul>
 * <li>increment the build number (as above) then run the profile: <code>release</code>, or</li>
 * <li>run the profile: <code>increment-release</code>, which does both in one shot.</li></ul></li>
 * <li>with the current build number:
 * <ul>
 * <li>run the profile: <code>release</code>, only.</li></ul></li>
 * </ul>
 * Then if necessary, run your normal release profile.
 * <p>
 * Of course, you can merge these profiles into your own as you see fit. But remember, the <code>increment-build</code>
 * and <code>increment-release</code> profiles' version of the plugin <b>will</b> increment the number <b>every time</b>
 * it is run. So be aware of possible run-away build numbers.
 * <h3>Properties</h3>
 * <h4>&lt;major.minor.version&gt;</h4>
 * {<b>Required</b>}<br>
 * The project's base version number as: _major.minor_ (eg: 1.0). The text you place here is __not__
 * validated by this plugin. However, incorrectly setting this might affect other parts of the build
 * system/IDE environment. Also, if you want to use a longer version number, such as: "1.1.2.3.4", etc.,
 * then again this plugin won't check.
 * <p>
 * The build number (and if not <i>release</i>: "-SNAPSHOT") will be appended to this string.
 * For example: "1.0.1-SNAPSHOT", "1.1.2.3.4.1-SNAPSHOT".
 * <h4>&lt;simple.buildNumber.indentSpaces&gt;</h4>
 * Alternative to &lt;indentSpaces&gt;. Use either one, not both.
 * <h4>&lt;simple.buildNumber.propertiesFilename&gt;</h4>
 * {default: "<code>${project.basedir}/buildNumber.properties</code>"}<br>
 * The properties file to be created/used.
 * <h4>&lt;simple.buildNumber.propertyName&gt;</h4>
 * {default: "<code>buildNumber</code>"}<br>
 * Sets the build number property name. This is used in the properties file, and is available to other
 * plugins during the current run. (Access by other plugins has <b>not</b> been tested)
 * <h3>Configuration</h3>
 * The following settings are available for the goal: <code>create-xml</code>.
 * <h4>&lt;keepNumber&gt;</h4>
 * {default: <code>false</code>}<br>
 * Set this to "true" to just keep using the current number in the external properties file.
 * Incrementing of the number will be disabled for this run.
 * <h4>&lt;indentSpaces&gt;</h4>
 * {default: <code>4</code>}<br>
 * The number of spaces used to indent text. This important, as it is used to find the &lt;project.version&gt;
 * setting in the <code>pom.xml</code> file.
 * <h4>&lt;release&gt;</h4>
 * {default: <code>false</code>}<br>
 * If not set to <i>true</i>, then "-SNAPSHOT" will be appended to the value to be set in &lt;project.version&gt;
 * in the <code>pom.xml</code> file.
 * <h4>&lt;skip&gt;</h4>
 * {default: <code>false</code>}<br>
 * Whether or not to skip this execution. A little superfluous when you set things up as above. This may be
 * removed in a later release.
 *
 * @deprecated Replaced by new goals: <b>increment</b> and <b>keep</b>
 *
 * @author  <a href="mailto:bw.opensource@yahoo.com">Bradley Willcott</a>
 *
 * @since 1.0
 * @version 1.0.9
 */
@Deprecated
@Mojo(name = "create-xml", defaultPhase = VALIDATE, requiresProject = true,
      threadSafe = false, executionStrategy = "once-per-session")
public class CreateXMLMojo extends AbstractMojo {

    /**
     * The default name of the build number property.
     * Used to store number in the property file.
     */
    private static final String BUILDNUMBER = "buildNumber";

    /**
     * You can rename the build number property name to another name if desired.
     */
    @Parameter(property = "simple.buildNumber.propertyName", defaultValue = BUILDNUMBER, readonly = true)
    private String buildNumberPropertyName;

    /**
     * Set the property name of the returned value for the final base filename string.
     */
    @Parameter
    private String finalBaseNamePropertyName;

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
     * The properties file to be created/used.
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
     * Whether or not to skip this execution.
     */
    @Parameter(defaultValue = "false")
    private boolean skip;

    /**
     * The maven project.
     */
    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {

        getLog().debug("Entry");
        getLog().debug("simple.buildNumber.propertyName: " + buildNumberPropertyName);
        getLog().debug("theProject: " + project.toString());

        if (skip)
        {
            getLog().info("Skipping execution.");
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
            getLog().error(msg1);
            getLog().warn(msg2);
            throw new MojoFailureException("The 'major.minor.version' property has not been set.");
        }

        if (project != null)
        {
            // Get the timestamp.
            String now = Long.toString(Calendar.getInstance().toInstant().getEpochSecond());
            getLog().debug("Time stamp: " + now);

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

            getLog().debug("Accessing properties file.");

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

            getLog().debug(BUILDNUMBER + " : " + strBuildNumber);

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

            getLog().debug("keepNumber: " + keepNumber);

            if (!keepNumber)
            {
                // Increment build number and store it in the property file.
                properties.setProperty(BUILDNUMBER, String.valueOf(++buildNumber));
                properties.setProperty("time", now);

                getLog().debug("Storing properties: " + propertiesFileLocation);
                getLog().debug(properties.toString());

                try (FileOutputStream outputStream = new FileOutputStream(propertiesFileLocation))
                {
                    properties.store(outputStream, "simple-buildnumber-maven-plugin properties file");
                } catch (IOException ex)
                {
                    throw new MojoExecutionException("Couldn't save properties file: " + propertiesFileLocation, ex);
                }
            }

            getLog().debug("revision: " + buildNumber);

            // Set the property to the current/new version string.
            setProperty(buildNumberPropertyName, "" + buildNumber);

            if (!keepNumber)
            {
                getLog().info("New " + buildNumberPropertyName + ": " + buildNumber + "\n");

            } else
            {
                getLog().info("*** Keeping previous " + buildNumberPropertyName + ": " + buildNumber + "\n");
            }

            // Update 'project.version'
            String projectVersion = majorMinorVersion + "." + buildNumber + (release ? "" : "-SNAPSHOT");
            getLog().debug("[OLD] project.artifact().getVersion(): " + project.getArtifact().getVersion());
            project.getArtifact().selectVersion(projectVersion);
            getLog().debug("[NEW] project.artifact().getVersion(): " + project.getArtifact().getVersion() + "\n");
            getLog().debug("[OLD] project.getModel().getVersion(): " + project.getModel().getVersion());
            project.getModel().setVersion(projectVersion);
            getLog().debug("[NEW] project.getModel().getVersion(): " + project.getModel().getVersion() + "\n");

            getLog().debug("update project 'pom.xml' file");
            updatePOM(projectVersion);

            // Final name of files
            getLog().info("[OLD] theProject.getBuild().getFinalName(): " + project.getBuild().getFinalName());
            finalName = project.getBuild().getFinalName();
            updateFinalName(projectVersion);
            project.getBuild().setFinalName(finalName);
            getLog().info("[NEW] theProject.getBuild().getFinalName(): " + project.getBuild().getFinalName());

            if (finalBaseNamePropertyName != null)
            {
                setProperty(finalBaseNamePropertyName, finalName);
            }
        }

        getLog().debug("Exit");
    }

    private String getProperty(String property) {
        return project.getProperties().getProperty(property);
    }

    private void setProperty(String property, String value) {
        if (value != null)
        {
            project.getProperties().setProperty(property, value);
        }
    }

    private void updateFinalName(String projectVersion) {
        getLog().debug("Entry - projectVersion: " + projectVersion);

        String regex = "(?<text>" + oldVersion + ")";
        StringBuilder sb = new StringBuilder();

        Pattern p = Pattern.compile(regex);
        Matcher m = p.matcher(finalName);

        if (m.find())
        {
            m.appendReplacement(sb, projectVersion);
        } else
        {
            getLog().warn("WARNING: project.build.finalName - Not Updated");
        }

        m.appendTail(sb);
        getLog().debug("sb:\n" + sb);
        finalName = sb.toString();

        getLog().debug("Exit");
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
        getLog().debug("Entry - projectVersion: " + projectVersion);

        String regex = "^(?<lead>[ ]{" + numberOfIndentSpaces + "}\\<version\\>)(?<text>[^<]*)(?<tail>\\</version\\>[ ]*\\n)";
        StringBuilder pomText = new StringBuilder();
        StringBuilder sb = new StringBuilder();
        String input = "";

        getLog().debug("About to read in pom file: " + pomFile);

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
            getLog().debug("Found: |" + m.group() + "|");
            oldVersion = m.group("text");
            getLog().debug("text: |" + oldVersion + "|");
            String replacement = m.group("lead") + projectVersion + m.group("tail");
            getLog().debug("replacement: |" + replacement + "|");
            m.appendReplacement(sb, replacement);
        } else
        {
            throw new MojoExecutionException("<version> tag: Not Found");
        }

        m.appendTail(sb);
        getLog().debug("sb:\n" + sb);

        try (PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(pomFile))))
        {
            out.print(sb);
        } catch (IOException ex)
        {
            throw new MojoFailureException("pom.xml file output related error.", ex);
        }

        getLog().debug("Exit");
    }
}
