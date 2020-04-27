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
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.IOUtil;

import static java.util.regex.Pattern.MULTILINE;

/**
 * Creates/Increments a build number for the project, and updates the "//project/version" in the "pom.xml"
 * file for the project.
 * <p>
 * You must set the property: major.minor.version
 * </p><p>
 * For example, if your project was at version 1.0-SNAPSHOT,<br>
 * then you would set the following:
 * </p>
 * <pre>
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
 *            &lt;id&gt;release-profile&lt;/id&gt;
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
 * </pre>
 *
 * @author Bradley Willcott
 * @since 1.0
 */
@Mojo(name = "create-xml", defaultPhase = LifecyclePhase.INITIALIZE, requiresProject = true, threadSafe = true)
public class CreateXMLMojo extends AbstractMojo {

    /**
     * Properties file to be created.
     */
    @Parameter(property = "maven.buildNumber.buildNumberPropertiesFileLocation", defaultValue = "${basedir}/buildNumber.properties")
    private File buildNumberPropertiesFileLocation;

    /**
     * You can rename the buildNumber property name to another property name if desired.
     */
    @Parameter(property = "maven.buildNumber.buildNumberPropertyName", defaultValue = "buildNumber")
    private String buildNumberPropertyName;
    /**
     * Project output filename;
     */
//    @Parameter(property = "project.build finalName", readonly = true, required = true)
    private String finalName;

    /**
     * Set this to "true" to just keep using the current number in the external properties file.
     * Incrementing of the number will be disabled.
     */
    @Parameter(property = "maven.buildNumber.keepNumber", defaultValue = "false")
    private boolean keepNumber;

    /**
     * The project's version as base: major.minor (eg: 1.0)
     * <p>
     * Add new property to project's 'pom.xml':
     * </p><p>
     * ...<br>
     * &lt;properties&gt;<br>
     * &nbsp;&nbsp;&nbsp;&nbsp;&lt;major.minor.version&gt;<br>
     * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;1.0<br>
     * &nbsp;&nbsp;&nbsp;&nbsp;&lt;/major.minor.version&gt;<br>
     * &nbsp;&nbsp;&nbsp;&nbsp;...<br>
     * &lt;/properties&gt;<br>
     * ...
     * </p>
     */
    @Parameter(property = "major.minor.version", readonly = true)
    private String majorMinorVersion;

    /**
     * The number of spaces used to indent text.
     */
    @Parameter(property = "maven.buildNumber.numberOfIndentSpaces", defaultValue = "4")
    private int numberOfIndentSpaces;
    private String oldVersion;

    /**
     * The project's "pom.xml" file.
     */
    @Parameter(defaultValue = "pom.xml", readonly = true, required = true)
    private File pomFile;

    /**
     * If not set to <i>true</i>, then "-SNAPSHOT" will be appended to the value
     * to be set in "//project/version" in the pom.xml file.
     */
    @Parameter(property = "maven.buildNumber.release", defaultValue = "false")
    private boolean release;

    // ////////////////////////////////////// internal variables ///////////////////////////////////
    private String revision;

    /**
     * If set to true, will get the build number once for the entire current build process.
     * Required for when using "maven.source.plugin:jar".
     */
    @Parameter(property = "maven.buildNumber.runOnce", defaultValue = "true")
    private boolean runOnce;

    /**
     * Whether to skip this execution.
     */
    @Parameter(property = "maven.buildNumber.skip", defaultValue = "false")
    private boolean skip;

    /**
     * The maven project.
     */
    @Parameter(property = "project", required = true, readonly = true)
    private MavenProject theProject;

    @Override
    public void execute()
            throws MojoExecutionException, MojoFailureException {
        if (skip) {
            logInfo("Skipping execution.");
            return;
        }

        if (majorMinorVersion == null) {
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
                          + "this plugin is run.";
            logError(msg1);
            logWarn(msg2);
            throw new MojoFailureException("The 'major.minor.version' property has not been set.");
        }

        if (theProject != null) {
            // Check if the plugin has already run.
            revision = getProperty(buildNumberPropertyName);

            if (runOnce && revision != null) {
                logInfo("Revision available from previous execution: " + revision);
                return;
            }

            String now = Long.toString(Calendar.getInstance().toInstant().getEpochSecond());
            logDebug("Time stamp: " + now);

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
                setProperty(buildNumberPropertyName, revision);

                if (!keepNumber) {
                    logInfo("New " + buildNumberPropertyName + ": " + revision + "\n");
                } else {
                    logInfo("Keeping previous " + buildNumberPropertyName + ": " + revision + "\n");
                }

                // Update 'project.version'
                String projectVersion = majorMinorVersion + "." + revision + (release ? "" : "-SNAPSHOT");
//                setProperty("project.version", projectVersion);
//                logInfo("project.version: " + projectVersion);
//                setProperty("project.artifact.version", projectVersion);
//                logInfo("project.artifact.version: " + projectVersion);
                logDebug("[OLD] project.artifact().getVersion(): " + theProject.getArtifact().getVersion());
                theProject.getArtifact().selectVersion(projectVersion);
                logDebug("[NEW] project.artifact().getVersion(): " + theProject.getArtifact().getVersion() + "\n");

                // update project 'pom.xml' file
                updatePOM(projectVersion);

                // Final name of files
//                logInfo("project.build.finalName: " + finalName);
                logDebug("[OLD] theProject.getBuild().getFinalName(): " + theProject.getBuild().getFinalName());
                finalName = theProject.getBuild().getFinalName();
                updateFinalName(projectVersion);
//                setProperty("project.build.finalName", finalName);
                theProject.getBuild().setFinalName(finalName);
                logDebug("[NEW] theProject.getBuild().getFinalName(): " + theProject.getBuild().getFinalName());
            }
        }
    }

    private String getProperty(String property) {
        return theProject.getProperties().getProperty(property);
    }

    private void logDebug(String msg) {
        getLog().debug(msg);
    }

    private void logError(String msg) {
        getLog().error(msg);
    }

    private void logInfo(String msg) {
        getLog().info(msg);
    }

    private void logWarn(String msg) {
        getLog().warn(msg);
    }

    private void setProperty(String property, String value) {
        if (value != null) {
            theProject.getProperties().put(property, value);
        }
    }

    private void updateFinalName(String projectVersion) {
        String regex = "(?<text>" + oldVersion + ")";
        StringBuilder sb = new StringBuilder();

        Pattern p = Pattern.compile(regex);
        Matcher m = p.matcher(finalName);

        if (m.find()) {
            m.appendReplacement(sb, projectVersion);
        } else {
            logWarn("WARNING: //project/build/finalName: Not Updated");
        }

        m.appendTail(sb);
        logDebug("sb:\n" + sb);
        finalName = sb.toString();
    }

    private void updatePOM(String projectVersion) throws MojoFailureException, MojoExecutionException {
        String regex = "^(?<lead>[ ]{" + numberOfIndentSpaces + "}\\<version\\>)(?<text>[^<]*)(?<tail>\\</version\\>[ ]*\\n)";
        StringBuilder pomText = new StringBuilder();
        StringBuilder sb = new StringBuilder();
        String input = "";

        try ( BufferedReader in = new BufferedReader(new FileReader(pomFile))) {
            while ((input = in.readLine()) != null) {
                pomText.append(input).append("\n");
            }
        } catch (IOException ex) {
            throw new MojoFailureException("pom.xml file input related error.", ex);
        }

        Pattern p = Pattern.compile(regex, MULTILINE);
        Matcher m = p.matcher(pomText.toString());

        if (m.find()) {
            logDebug("Found: |" + m.group() + "|");
            oldVersion = m.group("text");
            logDebug("text: |" + oldVersion + "|");
            String replacement = m.group("lead") + projectVersion + m.group("tail");
            logDebug("replacement: |" + replacement + "|");
            m.appendReplacement(sb, replacement);
        } else {
            throw new MojoExecutionException("<version> tag: Not Found");
        }

        m.appendTail(sb);
        logDebug("sb:\n" + sb);

        try ( PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(pomFile)))) {
            out.print(sb);
        } catch (IOException ex) {
            throw new MojoFailureException("pom.xml file output related error.", ex);
        }

//        // Use a SAX builder
//        SAXBuilder builder = new SAXBuilder();
//        // build a JDOM2 Document using the SAXBuilder.
//        Document jdomDoc = null;
//
//        try {
//            jdomDoc = builder.build(pomFile);
//        } catch (JDOMException | IOException ex) {
//            logError(CreateXMLMojo.class.getName() + ex);
//        }
//
//        //get the root element
//        Element project = jdomDoc.getRootElement();
//        logInfo(project.toString());
//
//        // get the first child with the name 'version'
//        Namespace projectNamespace = project.getNamespace();
//        Element version = project.getChild("version", projectNamespace);
//
//        if (version != null) {
//            logInfo("old 'project.version': " + version.getText());
//            version.setText(projectVersion);
//
//            // Output as XML
//            // create XMLOutputter
//            XMLOutputter xml = new XMLOutputter();
//            // we want to format the xml. This is used only for demonstration. pretty formatting adds extra spaces and is generally not required.
//            xml.setFormat(Format.getPrettyFormat());
//            xml.getFormat().setLineSeparator(LineSeparator.SYSTEM).setIndent("    ");
//            logInfo(xml.outputString(jdomDoc));
//        } else {
//            logInfo("version == null");
//            Namespace projectNamespace = project.getNamespace();
//        }
//        FileOutputStream outputStream = null;
//
//        try {
//            outputStream = new FileOutputStream(pomFile);
//            xml.output(jdomDoc, outputStream);
////        } catch (FileNotFoundException ex) {
////            logError(CreateXMLMojo.class.getName() + ex);
//        } catch (IOException ex) {
//            logError(CreateXMLMojo.class.getName() + ex);
//        } finally {
//            IOUtil.close(outputStream);
//        }
    }

}
