# simple-buildnumber-maven-plugin
I am by nature, a lazy person.  I hate doing something more than once.  Which is probably why I became
a programmer.  So, having to increment a build/version number every time it was needed, was becoming a major
pain.  Sure, you could keep doing it the way you've always done it, _manually_, or you could be a little
lazy and do it this way ...

The plugin Creates/Increments a build number for the project, and updates the "&lt;project.version&gt;" 
in the "_pom.xml_" file for the project. Simple. So is setting up.

## Setup
You must set the property: &lt;major.minor.version&gt;

For example, if your project was at version "1.0-SNAPSHOT",<br>
then you would set the following:

```
<project ...>
...
   <properties>
       <major.minor.version>1.0</major.minor.version>
       ...
   </properties>
...
   <profiles>
       <profile>
           <!-- Increment {buildNumber} and update the 'pom.xml' file -->
           <id>increment-build</id>
           <build>
               <plugins>
                   <plugin>
                       <groupId>com.bew.mojo</groupId>
                       <artifactId>simple-buildnumber-maven-plugin</artifactId>
                       <version>1.0</version>
                       <executions>
                           <execution>
                               <id>Increment Build</id>
                               <goals>
                                   <goal>create-xml</goal>
                               </goals>
                           </execution>
                       </executions>
                   </plugin>
               </plugins>
           </build>
       </profile>
        <profile>
            <id>increment-release</id>
            <build>
                <plugins>
                    <!-- Increment {buildNumber}, remove '-SNAPSHOT' and update the 'pom.xml' file -->
                    <plugin>
                        <groupId>com.bew.mojo</groupId>
                        <artifactId>simple-buildnumber-maven-plugin</artifactId>
                        <executions>
                            <execution>
                                <id>Increment Release</id>
                                <goals>
                                    <goal>create-xml</goal>
                                </goals>
                                <configuration>
                                    <release>true</release>
                                </configuration>
                            </execution>
                        </executions>
                    </plugin>
                </plugins>
            </build>
        </profile>
       <profile>
           <!-- Keep current {buildNumber}, remove '-SNAPSHOT' and update the 'pom.xml' file -->
           <id>release</id>
           <build>
               <plugins>
                   <plugin>
                       <groupId>com.bew.mojo</groupId>
                       <artifactId>simple-buildnumber-maven-plugin</artifactId>
                       <version>1.0</version>
                       <executions>
                           <execution>
                               <id>Release</id>
                               <goals>
                                   <goal>create-xml</goal>
                               </goals>
                               <configuration>
                                   <release>true</release>
                                   <keepNumber>true</keepNumber>
                               </configuration>
                           </execution>
                       </executions>
                   </plugin>
               </plugins>
           </build>
       </profile>
       ...
   </profiles>
...
</project>
```
(_By the way. You could be really lazy, and just copy the relevant sections from above and paste them_
_directly into your `pom.xml` file.  The "..." are just place holders for the rest of your file's text._)

When you run a profile other than one of the above, then nothing happens to the `<project.version>` setting.
So, to increment the build number, select the profile: `increment-build`.

Then go back to your normal development profile.

When you are ready to release a version:

- with a new build number, either:
    - increment the build number (as above) then run the profile: `release`, or
    - run the profile: `increment-release`, which does both in one shot.
- with the current build number:
    - run the profile: `release`, only.

Then if necessary, run your normal release profile.

Of course, you can merge these profiles into your own as you see fit.  But remember, the `increment-build`
and `increment-release` profiles' version of the plugin __will__ increment the number __every time__ 
it is run.  So be aware of possible run-away build numbers.

### Properties
#### &lt;major.minor.version&gt;
{__Required__}<br>
The project's base version number as: _major.minor_ (eg: 1.0).  The text you place here is __not__
validated by this plugin.  However, incorrectly setting this might affect other parts of the build 
system/IDE environment.  Also, if you want to use a longer version number, such as: "1.1.2.3.4", etc.,
then again this plugin won't check.

The build number (and if not _release_: "-SNAPSHOT") will be appended to this string.
For example: "1.0.1-SNAPSHOT", "1.1.2.3.4.1-SNAPSHOT".

#### &lt;simple.buildNumber.indentSpaces&gt;
Alternative to &lt;indentSpaces&gt;.  Use either one, not both.

#### &lt;simple.buildNumber.propertiesFilename&gt;
{default: "`${project.basedir}/buildNumber.properties`"}<br>
The properties file to be created/used.

#### &lt;simple.buildNumber.propertyName&gt;
{default: "`buildNumber`"}<br>
Sets the build number property name.  This is used in the properties file, and is available to other
plugins during the current run. (Access by other plugins has __not__ been tested)

### Configuration
The following settings are available for the goal: `create-xml`.

#### &lt;keepNumber&gt;
{default: `false`}<br>
Set this to "true" to just keep using the current number in the external properties file.
Incrementing of the number will be disabled for this run.

#### &lt;indentSpaces&gt;
{default: `4`}<br>
The number of spaces used to indent text.  This is important, as it is used to find the &lt;project.version&gt;
setting in the `pom.xml` file.

#### &lt;release&gt;
{default: `false`}<br>
If not set to <i>true</i>, then "-SNAPSHOT" will be appended to the value to be set in &lt;project.version&gt;
in the `pom.xml` file.

#### &lt;skip&gt;
{default: `false`}<br>
Whether or not to skip this execution.  A little superfluous when you set things up as above.  This may be
removed in a later release.
