/*
  Generates the <jarDependencies> XML block for .impl files.
  The groovy-maven-plugin runs this at generate-resources phase.
  The resulting XML is injected into .impl files via Maven resource filtering
  (${connector-dependencies} placeholder).

  Scope rule: the POM declares external SDK libraries with scope "provided" to
  keep Bonita Studio's import analyzer happy. This script collects compile,
  runtime AND provided scoped artifacts (except Bonita runtime libs already on
  the engine classpath) so they are correctly registered for runtime loading.
*/
import groovy.xml.MarkupBuilder

// GroupIds already on the Bonita runtime classpath - must NOT be listed in jarDependencies
def bonitaRuntime = ['org.bonitasoft.engine', 'org.bonitasoft.runtime', 'org.projectlombok', 'org.slf4j'] as Set

def xml = new StringWriter()
def builder = new MarkupBuilder(xml)
builder.jarDependencies {
    jarDependency("${project.artifactId}-${project.version}.${project.packaging}")
    project.artifacts
            .findAll { artifact ->
                (artifact.scope == "compile" || artifact.scope == "runtime" || artifact.scope == "provided") \
                && !bonitaRuntime.contains(artifact.groupId)
            }
            .sort { artifact -> artifact.artifactId }
            .each { artifact ->
                jarDependency("${artifact.artifactId}-${artifact.version}.${artifact.type}")
            }
}
def deps = xml.toString()
project.properties.setProperty("connector-dependencies", deps)
