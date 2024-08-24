# Inspired By Project Book

## Tweeting from CLI
[reference](https://projectbook.code.brettchalupa.com/command-line-interfaces/tweet-composer.html)

I am using Picocli to create this cli application, and using GraalVM to compile the Java application into executable.
with is the `./tweeit`.

## Goal
People said Java is bad for CLI, but I want to challenge myself to try doing everything in Java, and CLI application is one of them.

To explore the world of CLI, this is the first simple cli I am doing 
and I will explore more about capability of picocli including ASCII art, colors and more.
Hopefully I can create text based adventure games with picocli or some dev tools with picocli.

## Setup: How to use it?
Set up your project on X.com and get all the keys and secrets.

Guide: https://developer.x.com/en/docs/tutorials/step-by-step-guide-to-making-your-first-request-to-the-twitter-api-v2


Go to `src/main/resources` create a `.env` file.
```.env
API_KEY=get_from_x
API_SECRET_KEY=get_from_x
ACCESS_TOKEN=get_from_x
TOKEN_SECRET=get_from_x
```
## Using: TWEETING 
`./tweetit your_tweet_goes_to_here`

Let me know if it doesn't work ^^;
### Build it again?
`mvn clean install`

Make sure you have graalvm installed, or you can install it with SDKMAN or go to this guide https://www.graalvm.org/latest/docs/getting-started/
then run

`native-image -cp  picocli-4.7.6.jar  -jar target/Tweeit-0.1-SNAPSHOT-jar-with-dependencies.jar tweeit`

## Challenge
Setting up the right `pom.xml` is a bit tricky, because we will need to compile to native image we will need to use the maven assembly module

For the process code gen please go to this link https://picocli.info/#_add_as_external_dependency and https://picocli.info/#_annotation_processor

maven-assembly-plugin go to this link https://maven.apache.org/plugins/maven-assembly-plugin/usage.html#creating-an-executable-jar

creating graalvm image go to this link https://picocli.info/#_how_do_i_create_a_native_image_for_my_application 

```xml
  <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <!-- annotationProcessorPaths requires maven-compiler-plugin version 3.5 or higher -->
                <version>3.8.1</version>
                <configuration>
                    <annotationProcessorPaths>
                        <path>
                            <groupId>info.picocli</groupId>
                            <artifactId>picocli-codegen</artifactId>
                            <version>4.7.6</version>
                        </path>
                    </annotationProcessorPaths>
                    <compilerArgs>
                        <arg>-Aproject=${project.groupId}/${project.artifactId}</arg>
                    </compilerArgs>
                </configuration>
            </plugin>
            <plugin>
                <artifactId>maven-assembly-plugin</artifactId>
                <version>3.7.1</version>
                <configuration>
                    <descriptorRefs>
                        <descriptorRef>jar-with-dependencies</descriptorRef>
                    </descriptorRefs>
                    <archive>
                        <manifest>
                            <mainClass>co.loyyee.Tweeit</mainClass>
                        </manifest>
                    </archive>
                </configuration>
                <executions>
                    <execution>
                        <id>make-assembly</id> <!-- this is used for inheritance merges -->
                        <phase>package</phase> <!-- bind to the packaging phase -->
                        <goals>
                            <goal>single</goal>
                        </goals>
                    </execution>
                </executions>
            </plugin>
        </plugins>
    </build>
```