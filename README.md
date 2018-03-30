# NMS-Unlocked
<h2>NMS Unlocked makes other minecraft plugins version interoperable</h2>

This is a minecraft plugin that starts a java agent that reassembles other classes class code as they are being loaded to match the current version of minecraft!


You can find the download for the jar as well as a more detailed description at https://www.spigotmc.org/resources/nms-unlocked.54314/

<h3>Building</h3>

You can simply build the project using maven with mvn clean package.
Note that the following jars should be present for the plugin to work without being loaded as a java agent:
{project dir}/tools/linux/tools.jar
{project dir}/tools/osx/tools.jar
{project dir}/tools/windows/tools.jar
The tools.jar file can be taken from {jdk home}/lib/tools.jar. It does not seem to be included in java 9.



If you have any issues or questions, you can create an "issue" :)

Cheers!
