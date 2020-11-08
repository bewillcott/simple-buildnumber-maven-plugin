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

import java.io.File;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.maven.plugin.testing.MojoRule;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 *
 * @author <a href="mailto:bw.opensource@yahoo.com">Bradley Willcott</a>
 */
public class CreateXMLMojoTest {

    /**
     * Define a static logger variable so that it references the
     * Logger instance named "CreateXMLMojoTest".
     */
    private static final Logger log = LogManager.getLogger();

    @Rule
    public MojoRule rule = new MojoRule() {
        @Override
        protected void before() throws Throwable {
        }

        @Override
        protected void after() {
        }
    };

    /**
     * Test of create-xml goal, of class CreateXMLMojo.
     *
     * @throws java.lang.Exception
     */
    @Test
    public void testCreateXmlGoal() throws Exception {
        log.traceEntry();

        File baseDir = new File("target/test-classes/unit/project-to-test");
        assertNotNull(baseDir);
        log.debug("baseDir: {}", baseDir);
        assertTrue(baseDir.exists());

        CreateXMLMojo myMojo = (CreateXMLMojo) rule.lookupConfiguredMojo(baseDir, "create-xml");
        assertNotNull(myMojo);

        log.debug("myMojo: {}", myMojo);

        myMojo.execute();

//        File outputDirectory = (File) rule.getVariableValueFromObject(myMojo, "outputDirectory");
//        assertNotNull(outputDirectory);
//        assertTrue(outputDirectory.exists());
//
//        log.debug("outputDirectory: {}", outputDirectory);
//
//        File touch = new File(outputDirectory, "touch.txt");
//        assertTrue(touch.exists());
//        File pom = getTestFile("src/test/resources/unit/project-to-test/pom.xml");
//        assertNotNull(pom);
//        assertTrue(pom.exists());
//
//        log.trace("pom file: {}", pom);
//
//        CreateXMLMojo myMojo = (CreateXMLMojo) lookupMojo("create-xml", pom);
//        assertNotNull(myMojo);
//        myMojo.execute();
        log.traceExit();
    }
}
