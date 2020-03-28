/*
 * Copyright (C) 2020-2021 Bradley Willcott
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

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

/**
 * Sets the "project.artifact.version" in accordance with the
 *
 * @author Bradley Willcott
 */
@Mojo(name = "project.artifact.version", defaultPhase = LifecyclePhase.INITIALIZE, requiresProject = true, threadSafe = true)
public class EvalMojo extends AbstractMojo {

    /**
     * Specify the formatted text to use as the "artifact.version".<br>
     * If not supplied, the default will be used.
     * <p>
     * The setting for "release" is also relevant.
     * </p>
     */
    @Parameter(property = "project.artifact.version.format",
               defaultValue = "${project.version}.${buildNumber}",
               required = true)
    private String versionFormat;

    /**
     * If <em>this</em> has <b>not</b> been set to "{@code true}",
     * then "<b>-SNAPSHOT</b>" will be appended to the "{@code versionFormat}".
     */
    @Parameter(property = "project.artifact.version.release", defaultValue = "false")
    private boolean release;

    /**
     * Whether to skip this execution.
     */
    @Parameter(property = "project.artifact.version.skip", defaultValue = "false")
    private boolean skip;

    @Parameter(property = "project", required = true, readonly = true)
    private MavenProject project;

    private String version;

    /**
     *
     * @throws MojoExecutionException nuts
     * @throws MojoFailureException   nuts
     */
    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (skip) {
            getLog().info("Skipping execution.");
            return;
        }

        if (project != null) {
//            version = project.getArtifact().getVersion();

            if (version != null) {
                getLog().info("'project.getArtifact().getVersion()' available from previous execution: " + version);
                getLog().info("format: " + versionFormat);
            }

            if (!release) {
                versionFormat += "-SNAPSHOT";
            }

            project.getArtifact().selectVersion(versionFormat);
            getLog().info("project.artifact.version: " + project.getArtifact().getVersion());
        }
    }
}
