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
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import static com.bewsoftware.mojo.buildnumber.simple.Utils.updateBuildNumber;
import static com.bewsoftware.mojo.buildnumber.simple.Utils.updateFinalName;

/**
 * AbstractBuildNumberMojo class is the parent of both of the classes:
 * {@link IncrementMojo} and {@link KeepMojo}.
 *
 * @author <a href="mailto:bw.opensource@yahoo.com">Bradley Willcott</a>
 *
 * @since 2.0
 * @version 2.0
 */
public abstract class AbstractBuildNumberMojo extends AbstractMojo {

    /**
     * This is set/updated by the {@code build} goal.
     * <p>
     * It should NOT be edited or removed, as the buildNumber will be incremented
     * and used next time you run this goal.
     * <p>
     * <b>Format syntax:</b><br>
     * &lt;major&gt;.&lt;minor&gt;.&lt;patch&gt;:&lt;buildNumber&gt;
     * <p>
     * Example: 1.0.3:32
     */
    @Parameter(property = "versioning.build.number", readonly = true)
    protected String buildNumber;

    /**
     * Project Version number length (>=2): 2 = n.bn, 3 = n.n.bn, 4 = n.n.n.bn
     * (n: number, bn: buildNumber).
     * <p>
     * Example:
     * <pre><code>
     * &lt;version.number.length&gt;3&lt;/version.number.length&gt;
     * </code></pre>
     * <b>Default:</b> 3
     */
    @Parameter(property = "version.number.length", defaultValue = "3", readonly = true)
    protected int length;
    /**
     * Define a static logger variable so that it references the
     * Logger instance named "CreateXMLMojoTest".
     */
//    protected final Log log = getLog();

    /**
     * If not set to <i>true</i>, then "-SNAPSHOT" will be appended to the value
     * to be set in "project.version" in the pom.xml file.
     */
    @Parameter(defaultValue = "false")
    protected boolean release;

    /**
     * Whether or not to skip this execution.
     */
    @Parameter(defaultValue = "false")
    protected boolean skip;

    /**
     * The maven project.
     *
     * @parameter property="project"
     * @readonly
     * @required
     */
    @Parameter(property = "project", readonly = true, required = true)
    protected MavenProject theProject;

    /**
     * Main method of Mojo.
     *
     * @param keep Determines whether or not to keep old number.
     *
     * @return {@code true} if completed, {@code false} otherwise.
     *
     * @throws MojoFailureException   if any.
     * @throws MojoExecutionException if any.
     */
    protected boolean run(final boolean keep) throws MojoFailureException, MojoExecutionException {
        getLog().info("Simple BuildNumber Maven Plugin (" + (keep ? "keep" : "increment") + ")");
        getLog().info("=================================" + (keep ? "====" : "=========") + "=");
        getLog().debug("\ntheProject: " + theProject.toString());

        if (skip)
        {
            getLog().info("\nSkipping execution.");
            return false;
        }

        if (theProject != null)
        {
            final StringReturn oldVersion = new StringReturn();
            final StringReturn newVersion = new StringReturn();

            if (updateBuildNumber(length, keep, release, getLog(), oldVersion, newVersion))
            {
                updateProjectVersion(newVersion, oldVersion);
            }
        }

        return true;
    }

    /**
     * Update "project.version".
     *
     * @param newVersion New version.
     * @param oldVersion Old version.
     */
    protected void updateProjectVersion(final StringReturn newVersion, final StringReturn oldVersion) {

        getLog().debug("\n[OLD] project.artifact().getVersion(): " + theProject.getArtifact().getVersion());
        theProject.getArtifact().selectVersion(newVersion.val);
        getLog().debug("[NEW] project.artifact().getVersion(): " + theProject.getArtifact().getVersion() + "\n");
        getLog().debug("[OLD] project.getModel().getVersion(): " + theProject.getModel().getVersion());
        theProject.getModel().setVersion(newVersion.val);
        getLog().debug("[NEW] project.getModel().getVersion(): " + theProject.getModel().getVersion() + "\n");

        // Final name of files
        getLog().debug("[OLD] theProject.getBuild().getFinalName(): " + theProject.getBuild().getFinalName());

        // Update /project/build/finalName.
        theProject.getBuild().setFinalName(
                updateFinalName(theProject.getBuild().getFinalName(),
                                oldVersion.val, newVersion.val, getLog()));

        getLog().debug("[NEW] theProject.getBuild().getFinalName(): " + theProject.getBuild().getFinalName());
    }

}
