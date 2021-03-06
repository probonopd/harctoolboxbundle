/*
Copyright (C) 2013, 2014 Bengt Martensson.

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; either version 3 of the License, or (at
your option) any later version.

This program is distributed in the hope that it will be useful, but
WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
General Public License for more details.

You should have received a copy of the GNU General Public License along with
this program. If not, see http://www.gnu.org/licenses/.
 */

package org.harctoolbox.irscrutinizer;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.ParseException;
import javax.xml.parsers.ParserConfigurationException;
import org.harctoolbox.IrpMaster.IncompatibleArgumentException;
import org.harctoolbox.IrpMaster.IrpUtils;
import org.harctoolbox.guicomponents.GuiUtils;
import org.xml.sax.SAXException;

/**
 * This class decodes command line parameters and fires up the GUI.
 */
public class IrScrutinizer {

    /** Number indicating invalid value. */
    public final static long invalid = -1;

    private IrScrutinizer() {
    }

    private static void usage(int exitcode) {
        StringBuilder str = new StringBuilder();
        argumentParser.usage(str);

        (exitcode == IrpUtils.exitSuccess ? System.out : System.err).println(str);
        doExit(exitcode); // placifying FindBugs...
    }

    private static void doExit(int exitcode) {
        System.exit(exitcode);
    }

    private final static class CommandLineArgs {

        @Parameter(names = {"-D", "--debug"}, description = "Debug code")
        private int debug = 0;

        @Parameter(names = {"-h", "--help", "-?"}, description = "Display help message")
        private boolean helpRequested = false;

        @Parameter(names = {"-H", "--home", "--applicationhome", "--apphome"}, description = "Set application home (where files are located)")
        private String applicationHome = null;

        @Parameter(names = {"--nuke-properties"}, description = "Get rid of present properties file")
        private boolean nukeProperties = false;

        @Parameter(names = {"-p", "--properties"}, description = "Pathname of properties file")
        private String propertiesFilename = null;


        @Parameter(names = {"-V", "--version"}, description = "Display version information")
        private boolean versionRequested;

        @Parameter(names = {"-v", "--verbose"}, description = "Have some commands executed verbosely")
        private boolean verbose;

        @Parameter(names = {"-x", "--experimental"}, description = "Enable experimental features")
        private boolean experimental;
    }

    private static JCommander argumentParser;
    private static CommandLineArgs commandLineArgs = new CommandLineArgs();

    /**
     * @param args the command line arguments.
     */
    public static void main(String[] args) {
        int userLevel = 0; // presently not used
        argumentParser = new JCommander(commandLineArgs);
        argumentParser.setProgramName(Version.appName);

        try {
            argumentParser.parse(args);
        } catch (ParameterException ex) {
            System.err.println(ex.getMessage());
            usage(IrpUtils.exitUsageError);
        }

        if (commandLineArgs.helpRequested)
            usage(IrpUtils.exitSuccess);

        if (commandLineArgs.versionRequested) {
            System.out.println(Version.versionString);
            System.out.println("JVM: " + System.getProperty("java.vendor") + " " + System.getProperty("java.version") + " " + System.getProperty("os.name") + "-" + System.getProperty("os.arch"));
            System.out.println();
            System.out.println(Version.licenseString);
            System.exit(IrpUtils.exitSuccess);
        }

        if (commandLineArgs.nukeProperties) {
            Props properties = new Props(commandLineArgs.propertiesFilename, commandLineArgs.applicationHome);
            String filename = properties.getFilename();
            System.out.println("Deleting the properties file " + filename + ".");
            (new File(filename)).deleteOnExit();
            System.exit(IrpUtils.exitSuccess);
        }

        String applicationHome =  commandLineArgs.applicationHome != null ? commandLineArgs.applicationHome : System.getenv("IRSCRUTINIZERHOME");
        if (applicationHome == null) {
            URL url = IrScrutinizer.class.getProtectionDomain().getCodeSource().getLocation();
            File dir = null;
            try {
                dir = new File(url.toURI()).getParentFile();
            } catch (URISyntaxException ex) {
                System.err.println(ex.getMessage());
                System.exit(IrpUtils.exitFatalProgramFailure);
            }
            if (dir == null) {
                System.err.println("Error getting applicationHome");
                System.exit(IrpUtils.exitFatalProgramFailure);
            } else
                applicationHome = (dir.getName().equals("build") || dir.getName().equals("dist"))
                    ? dir.getParent() : dir.getPath();
        }
        if (applicationHome != null && !applicationHome.endsWith(File.separator))
            applicationHome += File.separator;

        if (commandLineArgs.debug > 0)
            System.err.println("applicationHome = " + applicationHome);

        userLevel = commandLineArgs.experimental ? 1 : 0;

        guiExecute(applicationHome, commandLineArgs.propertiesFilename, commandLineArgs.verbose, commandLineArgs.debug, userLevel);
    }

    private static void guiExecute(final String applicationHome, final String propsfilename, final boolean verbose, final int debug, final int userlevel) {
        java.awt.EventQueue.invokeLater(new Runnable() {

            @Override
            public void run() {
                try {
                    new GuiMain(applicationHome, propsfilename, verbose, debug, userlevel).setVisible(true);
                } catch (ParseException ex) {
                    GuiUtils.fatal(ex, IrpUtils.exitConfigReadError);
                } catch (ParserConfigurationException ex) {
                    GuiUtils.fatal(ex, IrpUtils.exitInternalFailure);
                } catch (SAXException ex) {
                    GuiUtils.fatal(ex, IrpUtils.exitXmlError);
                } catch (IOException ex) {
                    GuiUtils.fatal(ex, IrpUtils.exitConfigReadError);
                } catch (IncompatibleArgumentException ex) {
                    GuiUtils.fatal(ex, IrpUtils.exitConfigReadError);
                } catch (URISyntaxException ex) {
                    GuiUtils.fatal(ex, IrpUtils.exitConfigReadError);
                }
            }
        });
    }
}
