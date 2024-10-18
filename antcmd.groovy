/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import groovy.util.AntBuilder
import groovy.cli.commons.CliBuilder
import java.text.SimpleDateFormat
import java.util.Date

// List of supported Ant commands
def supportedCommands = [
        'copy', 'delete', 'move', 'mkdir', 'touch', 'zip', 'unzip', 'tar', 'untar',
        'jar', 'war', 'java', 'exec', 'ftp', 'scp', 'sshexec', 'xmlvalidate', 'xslt',
        'echo', 'chmod', 'chown'
]

// Function to parse the custom file
def parseCommands(file) {
    def commands = []
    def currentCommand = null
    file.eachLine { line, lineNumber ->
        line = line.trim()
        if (line.isEmpty() || line.startsWith('#')) {
            // Skip empty lines and comments
            return
        }
        if (lineNumber == 0) {
            if (!line.toUpperCase().startsWith('ANTCMD')) {
                throw new IllegalArgumentException("First line must start with 'ANTCMD'.")
            }
            commands << [name: 'ANTCMD', attributes: line]
        } else if (line.endsWith('\\')) {
            // Multiline command part
            def linePart = line[0..-2] // Remove trailing '\'
            if (currentCommand == null) {
                currentCommand = linePart
            } else {
                currentCommand += ' ' + linePart
            }
        } else {
            // Normal command or last line of a multiline command
            def completeLine = currentCommand ? currentCommand + ' ' + line : line
            def parts = completeLine.split(/\s+/, 2) // Split into command and the rest
            if (parts.length > 1) {
                commands << [name: parts[0], attributes: parts[1]]
            } else {
                commands << [name: parts[0], attributes: ""]
            }
            currentCommand = null
        }
    }
    return commands
}

// Function to convert command to AntBuilder code
def commandToGroovy(command, supportedCommands) {
    def builder = new StringBuilder()
    if (supportedCommands.contains(command.name)) {

        builder.append("ant.${command.name}(${attributesToMap(command.attributes)})\n")

    } else {
        // Treat as normal Groovy code

        builder.append("${command.name} ${command.attributes}\n")

    }
    return builder.toString()
}

// Function to convert attributes string to a map
def attributesToMap(attributes) {
    def map = [:]
    attributes.split(/\s+/).each { pair ->
        def (key, value) = pair.split('=', 2)
        map[key] = value
    }
    return map
}

// Function to display help message
def showHelp(cli, supportedCommands) {
    cli.usage()
    println """
Supported Ant commands:
${supportedCommands.join(', ')}

Other lines will be treated as normal Groovy code.

Example usage:
groovy antcmd.groovy --input commands.antcmd --output generatedScript.groovy --exec log
groovy antcmd.groovy -x copy src="source.txt" dest="build/destination.txt"
"""
}

// Parse command-line arguments
def cli = new CliBuilder(usage: 'groovy antcmd.groovy [options]')
cli.h(longOpt: 'help', 'Show usage information')
cli.i(longOpt: 'input', args: 1, argName: 'inputFile', 'Input .antcmd file')
cli.o(longOpt: 'output', args: 1, argName: 'outputFile', 'Output Groovy script file')
cli.e(longOpt: 'exec', args: 1, argName: 'execMode', 'Execution mode: off, log, on')
cli.x(longOpt: 'execute', args: 1, argName: 'command', 'Execute a single Ant command immediately')

def options = cli.parse(args)
if (!options) {
    showHelp(cli, supportedCommands)
    System.exit(1)
}
if (options.h) {
    showHelp(cli, supportedCommands)
    return
}

String execMode = options.e ? options.e.toLowerCase() : 'no'
def trueValues = ["yes", "true", "on", "1", "run"]
def generateOnly = !trueValues.contains(execMode.trim())
def inputFile = options.i ? new File(options.i) : new File('commands.antcmd')
def outputFile = options.o ? new File(options.o) : null

if (!inputFile.exists()) {
    println "Error: Input file not found."
    System.exit(1)
}

def commands
try {
    commands = parseCommands(inputFile)
} catch (Exception e) {
    println "Error parsing input file: ${e.message}"
    System.exit(1)
}

println "Parsed commands: ${commands}" // Debug output

// Default values if not set by command-line options
if (!outputFile) {
    def dateFormat = new SimpleDateFormat("yyyyMMdd-HHmm-ssSSS")
    def timestamp = dateFormat.format(new Date())
    outputFile = new File("cmds-${timestamp}.groovy")
}



def outputBuilder = new StringBuilder()
outputBuilder.append("import groovy.util.AntBuilder\n\n")
outputBuilder.append("def ant = new AntBuilder()\n\n")

commands[1..-1].each { command ->
    outputBuilder.append(commandToGroovy(command, supportedCommands))
}

try {
    outputFile.text = outputBuilder.toString()
    println "Groovy script has been generated at ${outputFile.absolutePath}"
    println "Output file: ${outputFile.absolutePath}" // Log the name of the output file
} catch (Exception e) {
    println "Error writing to output file: ${e.message}"
    System.exit(1)
}

if (!generateOnly) {
    println "Running the generated Groovy script..."
    try {
        def scriptText = outputFile.text
        new GroovyShell().evaluate(scriptText)
        println "Script execution complete."
    } catch (Exception e) {
        println "Error executing generated script: ${e.message}"
        e.printStackTrace()
        System.exit(1)
    }
}
