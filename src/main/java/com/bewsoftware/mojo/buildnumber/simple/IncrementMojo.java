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

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;

import static org.apache.maven.plugins.annotations.LifecyclePhase.VALIDATE;

/**
 * IncrementMojo class increments the Project Version buildNumber.
 *
 * @author <a href="mailto:bw.opensource@yahoo.com">Bradley Willcott</a>
 *
 * @since 2.0
 * @version 2.0
 */
@Mojo(name = "increment", defaultPhase = VALIDATE, requiresProject = true,
      threadSafe = false, executionStrategy = "once-per-session")
public class IncrementMojo extends AbstractBuildNumberMojo {

    /**
     * KEEP is set to {@code false} so that the buildNumber will be incremented.
     */
    private static final boolean KEEP = false;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        getLog().debug("Entry: execute()");

        if (run(KEEP))
        {
            return;
        }

        getLog().debug("Exit: execute()");
    }

}
