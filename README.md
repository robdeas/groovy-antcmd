# groovy-antcmd (Groovy Ant tasks)
A way of invoking a series of file manipulation commands via groovy and ant

The Groovy script accomplishes the following tasks:

* Parse a Custom .antcmd File: The script reads a custom .antcmd file and parses the commands.
* Generate Groovy Script: It converts the parsed commands into a Groovy script using AntBuilder.
* Execute the Generated Script: Optionally, it can execute the generated Groovy script.
* It can also execute a single Antbuilder command on the commandline.

Sample: Basic File Operations : An example ANTCMD file is below 

ANTCMD
copy src="source.txt" dest="build/destination.txt"
delete file="build/oldfile.txt"
mkdir dir="build/newdir"

See my website https://robd.tech/exploring-antbuilder-with-groovy/ for full details
