/*
 * Copyright (C) 2021 Bradley Willcott
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

import com.bewsoftware.utils.struct.StringReturn;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;

import static java.lang.String.format;
import static java.util.regex.Pattern.compile;

/**
 * Utils class description.
 *
 * @author <a href="mailto:bw.opensource@yahoo.com">Bradley Willcott</a>
 *
 * @since 2.0
 * @version 2.0
 */
public final class Utils {

    /**
     * RegEx string originally from: <a href="https://semver.org/">https://semver.org/</a>
     */
    private static final String SEMVAR = "^(?<major>0|[1-9]\\d*)\\."
                                         + "(?<minor>0|[1-9]\\d*)\\."
                                         + "(?<patch>0|[1-9]\\d*)"
                                         + "(?:-(?<prerelease>(?:0|[1-9]\\d*|\\d*[a-zA-Z-][0-9a-zA-Z-]*)"
                                         + "(?:\\.(?:0|[1-9]\\d*|\\d*[a-zA-Z-][0-9a-zA-Z-]*))*))?"
                                         + "(?:\\+(?<buildmetadata>[0-9a-zA-Z-]+(?:\\.[0-9a-zA-Z-]+)*))?$";

    /**
     * POM property to be set/updated by {@code build} goal.
     */
    private static final String VERSIONING = "versioning.build.number";

    /**
     * Name of file to operate on.
     */
    private static final String FILE_NAME = "pom.xml";

    /**
     * Project version suffix.
     */
    private static final String SUFFIX = "-SNAPSHOT";

    /**
     * Update build number component of the Project Version number in the 'pom.xml' file.
     * <p>
     * Also update whether or not there is to be a suffix: '-SNAPSHOT'.
     *
     * @param vLength        Length of version number (&gt;=2): 2 = n.bn, 3 = n.n.bn, 4 = n.n.n.bn
     *                       (n: number, bn: buildNumber).
     * @param keep           Keep current buildNumber - don't increment it.
     * @param release        Do not include '-SNAPSHOT' suffix in version text.
     * @param log            Maven logging.
     * @param oldVersion     To hold the old Project Version text to be returned.
     * @param projectVersion To hold the new Project Version text to be returned.
     *
     * @return {@code true} if version text changed, {@code false} otherwise.
     *
     * @throws MojoExecutionException if any.
     * @throws MojoFailureException   if any.
     */
    public static boolean updateBuildNumber(final int vLength, final boolean keep,
                                            final boolean release, final Log log,
                                            final StringReturn oldVersion,
                                            final StringReturn projectVersion)
            throws MojoExecutionException, MojoFailureException {

        log.debug("Entry: updateBuildNumber()");
        log.debug("release: " + release);

        boolean rtn = false;

        if (vLength < 2)
        {
            throw new MojoExecutionException("\nError: 'version.number.length' MUST be "
                                             + "greater than or equal to '2'.");
        }

        File inputFile = new File(FILE_NAME);
        SAXReader reader = new SAXReader();
        Document document = null;

        // Read file.
        try
        {
            document = reader.read(inputFile);
        } catch (DocumentException ex)
        {
            throw new MojoExecutionException("\nError reading from '" + FILE_NAME + "'", ex);
        }

        // root is 'project'.
        Element version = document.getRootElement().element("version");
        oldVersion.val = version.getTextTrim();

        log.debug("\n/project/version : " + oldVersion.val);

        // Prepare regex.
        Matcher m = compile("^(?<main>\\d+(\\.\\d+){" + (vLength - 2)
                            + "})(\\.(?<build>\\d+))?(?<suffix>\\" + SUFFIX + ")?$")
                .matcher(oldVersion.val);

        String output = "";

        // Process found data.
        if (m.find())
        {
            String main = m.group("main");
            String build = m.group("build");
            String suffix = m.group("suffix");

            log.debug("main : " + main);
            log.debug("build : " + build);
            log.debug("suffix : " + suffix);

            if (build == null)
            {
                if (keep)
                {
                    build = "";
                } else
                {
                    build = ".0";
                }
            } else
            {
                if (keep)
                {
                    build = "." + build;
                } else
                {
                    int iBuild = Integer.parseInt(build);
                    build = "." + ++iBuild;
                }
            }

            if (suffix == null)
            {
                if (release)
                {
                    suffix = "";
                } else
                {
                    suffix = SUFFIX;
                }
            } else if (release)
            {
                suffix = "";
            }

            // Build new version text string.
            output = format("%s%s%s", main, build, suffix);
            log.debug("output : " + output);

            // If changed, update file.
            if (!oldVersion.val.equals(output))
            {
                version.setText(output);

                try
                {
                    XMLWriter writer = new XMLWriter(
                            Files.newBufferedWriter(inputFile.toPath(),
                                                    StandardOpenOption.WRITE,
                                                    StandardOpenOption.TRUNCATE_EXISTING));
                    writer.write(document);
                    writer.flush();
                    writer.close();
                } catch (IOException ex)
                {
                    throw new MojoExecutionException("\nError writing to '" + FILE_NAME + "'", ex);
                }

                rtn = true;
            }
        } else
        {
            throw new MojoFailureException("\nWarning: No valid version text found: " + oldVersion.val
                                           + "\nPossible incorrect setting for 'version.number.length': " + vLength);
        }

        projectVersion.val = output;
        log.debug("Exit: updateBuildNumber()");
        return rtn;
    }

    /**
     * Update the /project/build/finalName.
     *
     * @param finalName  Current text.
     * @param oldVersion Old version text.
     * @param newVersion New version text.
     * @param log        Maven logging.
     *
     * @return new finalName text.
     */
    public static String updateFinalName(final String finalName, final String oldVersion,
                                         final String newVersion, final Log log) {

        log.debug("Entry: updateFinalName()");
        log.debug("projectVersion: " + newVersion);

        String regex = "(?<text>" + oldVersion + ")";
        StringBuilder sb = new StringBuilder();

        Pattern p = Pattern.compile(regex);
        Matcher m = p.matcher(finalName);

        if (m.find())
        {
            m.appendReplacement(sb, newVersion);
        } else
        {
            log.warn("WARNING: project.build.finalName - Not Updated");
        }

        m.appendTail(sb);
        log.debug("sb:\n" + sb);

        log.debug("Exit: updateFinalName()");
        return sb.toString();
    }

    /**
     * Not meant to be instantiated.
     */
    private Utils() {
    }
}
