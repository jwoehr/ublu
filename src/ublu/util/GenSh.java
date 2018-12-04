/*
 * Copyright (c) 2015, Absolute Performance, Inc. http://www.absolute-performance.com
 * Copyright (c) 2017, Jack J. Woehr jwoehr@softwoehr.com 
 * SoftWoehr LLC PO Box 51, Golden CO 80402-0051 http://www.softwoehr.com
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 * * Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package ublu.util;

import ublu.Ublu;
import java.util.ArrayList;
import java.util.Calendar;
import ublu.util.Generics.StringArrayList;

/**
 * Shell launch script generator. Scripts have been tested with bash and ksh.
 *
 * @author jwoehr
 */
public class GenSh {

    /**
     * Default path to find the system jar to load. Its libs should be
     * underneath in the ./lib dir.
     */
    public final static String DEFAULT_FQJARPATH = "./ublu.jar";
    private OptionArrayList optionArrayList;
    private String fqJarPath;
    private String functionInvocation;
    private String scriptName;
    private String includeName;
    private String includePath;
    private StringBuffer accumulatedCommand = new StringBuffer("gensh");
    private String dateGenerated = Calendar.getInstance().getTime().toString();
    private String generatingUser = Ublu.getUser();
    private boolean strictPosix;
    private StringArrayList preludeCommandList;

    /**
     * Get the array of prelude commands
     *
     * @return array of prelude commands
     */
    public StringArrayList getPreludeCommandList() {
        return preludeCommandList;
    }

    /**
     * Get the value of strictPosix
     *
     * @return the value of strictPosix
     */
    public boolean isStrictPosix() {
        return strictPosix;
    }

    /**
     * Set the value of strictPosix
     *
     * @param strictPosix the value of strictPosix
     */
    public void setStrictPosix(boolean strictPosix) {
        this.strictPosix = strictPosix;
    }

    /**
     * Accumulate an option during interpretation so we can document for other
     * users the (complicated) command lines put to gensh.
     *
     * @param o the option to accumulate
     */
    public void accumulateOption(Option o) {
        accumulateCommand(o.getOptionChar());
        accumulateCommand(o.getAssignedName());
        if (!(o instanceof OptScriptOnly)) {
            accumulateCommand(o.getTupleName());
        }
        accumulateCommandQuoted(o.getDescription());
    }

    /**
     * Accumulate parts of a command (during interpretation) so we can document
     * for other users the (complicated) command lines put to gensh.
     *
     * @param s string to accumulate
     */
    public void accumulateCommand(String s) {
        accumulatedCommand.append(" ");
        accumulatedCommand.append(s);
    }

    /**
     * Accumulate a string which will be placed in the gensh output header but
     * make sure it ends in a blank space for quoted string purposes. Plainword
     * arguments to gensh need this because they'll otherwise appear incorrectly
     * quoted in the header which serves as documention of the (complicated)
     * command lines put to gensh.
     *
     * @param s string to accumulate
     */
    public void accumulateCommandQuoted(String s) {
        s = "${ " + (s.endsWith(" ") ? s : s + " ") + "}$";
        accumulateCommand(s);
    }

    /**
     * Accumulate parts of a command (during interpretation) so we can document
     * for other users the (complicated) command lines put to gensh.
     *
     * @param c char to accumulate
     */
    public void accumulateCommand(char c) {
        accumulatedCommand.append(" ");
        accumulatedCommand.append(c);
    }

    /**
     * Add a shell command string to the array of prelude commands
     *
     * @param cmdString shell command string
     */
    public void addPreludeCommand(String cmdString) {
        preludeCommandList.add(cmdString);
    }

    /**
     * Instance with a new options array and a default fq jar path of
     * DEFAULT_FQJARPATH
     */
    public GenSh() {
        this.optionArrayList = new OptionArrayList();
        this.preludeCommandList = new StringArrayList();
        fqJarPath = DEFAULT_FQJARPATH;
        includePath = "";
    }

    /**
     * Get the fully qualified jar path
     *
     * @return the fully qualified jar path
     */
    public String getFqJarPath() {
        return fqJarPath;
    }

    /**
     * Set the fully qualified jar path
     *
     * @param fqJarPath
     */
    public void setFqJarPath(String fqJarPath) {
        this.fqJarPath = fqJarPath;
    }

    /**
     * Get the function invocation
     *
     * @return the function invocation
     *
     */
    public String getFunctionInvocation() {
        return functionInvocation;
    }

    /**
     * Set the function invocation
     *
     * @param functionInvocation the function invocation
     *
     */
    public void setFunctionInvocation(String functionInvocation) {
        this.functionInvocation = functionInvocation;
    }

    /**
     * Get the name of the script to generate. This is not the filename. The
     * script is put to the destination datasink. This is just an aribitrary
     * name which appears in the comments generated in the script.
     *
     * @return the name of the script to generate
     */
    public String getScriptName() {
        return scriptName;
    }

    /**
     * Set the name of the script to generate. This is not the filename. The
     * script is put to the destination datasink. This is just an aribitrary
     * name which appears in the comments generated in the script.
     *
     * @param scriptName the name of the script to generate
     */
    public void setScriptName(String scriptName) {
        this.scriptName = scriptName;
    }

    /**
     * Get the name of the file of source to include upon system invocation
     *
     * @return the name of the file of source to include upon system invocation
     */
    public String getIncludeName() {
        return includeName;
    }

    /**
     * Set the name of the file of source to include upon system invocation
     *
     * @param includeName the name of the file of source to include upon system
     * invocation
     */
    public void setIncludeName(String includeName) {
        this.includeName = includeName;
    }

    /**
     * Get the include path for the gensh script, for relative imports
     *
     * @return the include path to find the script
     */
    public String getIncludePath() {
        return includePath;
    }

    /**
     * Set the include path for the gensh script, for relative imports
     *
     * @param includePath the include path, to find the script
     */
    public void setIncludePath(String includePath) {
        this.includePath = includePath;
    }

    /**
     * Add an option to be processed in script option generation
     *
     * @param option an option to be processed in script option generation
     */
    public void addOption(Option option) {
        optionArrayList.add(option);
    }

    private String genUsageOptions() {
        StringBuilder sb = new StringBuilder();
        sb.append("[-h] [-X...] [-Dprop=val]");
        for (Option option : optionArrayList) {
            sb.append(' ');
            if (!option.isRequired()) {
                sb.append("[");
            }
            sb.append(option.optionAndParam());
            if (!option.isRequired()) {
                sb.append("]");
            }
        }
        return sb.toString();
    }

    private String genUsageDescriptions() {
        StringBuilder sb = new StringBuilder();
        sb.append("echo \"\t").append("-h").append("\t\t").append("display this help message and exit 0").append("\"\n");
        sb.append("echo \"\t").append("-X xOpt").append("\t\t").append("pass a -X option to the JVM (can be used many times)").append("\"\n");
        sb.append("echo \"\t").append("-D some.property=\\\"some value\\\"").append("\t").append("pass a property to the JVM (can be used many times)").append("\"\n");
        for (Option option : optionArrayList) {
            sb.append("echo \"\t")
                    .append(option.optionAndParam())
                    .append("\t")
                    .append(option.getDescription());
            if (option.isRequired()) {
                sb.append(" (required option)");
            }
            sb.append("\"\n");
        }
        return sb.toString();
    }

    private String genUsage() {
        StringBuilder sb = new StringBuilder();
        sb.append("# Usage message\n")
                .append("function usage");
        if (isStrictPosix()) {
            sb.append("()");
        }
        sb.append(" {\n")
                .append("echo \"").append(getScriptName()).append("\"\n")
                .append("echo \"").append("Ublu gensh autogenerated this shell script ")
                .append(dateGenerated)
                .append(" for ")
                .append(generatingUser)
                .append(".\"\n")
                .append("echo \"Usage: $0 [glob] [silent] ")
                .append(genUsageOptions())
                .append("\"\n")
                .append("echo \"\twhere\"\n")
                .append(genUsageDescriptions())
                .append("echo \"---\"\n")
                .append("echo \"If the keyword 'glob' appears ahead of all other options and arguments, only then will arguments be globbed by the executing shell (noglob default).\"\n")
                .append("echo \"If the keyword 'silent' appears ahead of all options (except 'glob' if the latter is present), then included files will not echo and prompting is suppressed.\"\n")
                .append("echo \"Exit code is the result of execution, or 0 for -h or 2 if there is an error in processing options.\"\n")
                .append("echo \"This script sets \\$SCRIPTDIR to the script's directory prior to executing prelude commands and Ublu invocation.\"\n")
                .append("}");
        return sb.toString();
    }

    private String genGlobSection() {
        StringBuilder sb = new StringBuilder();
        sb.append("#Test if user wants arguments globbed - default noglob\n")
                .append("if [ \"$1\" == \"glob\" ]\n")
                .append("then\n")
                .append("\tset +o noglob # POSIX\n")
                .append("\tshift\n")
                .append("else\n")
                .append("\tset -o noglob # POSIX\n")
                .append("fi");
        return sb.toString();
    }

    private String genSilentSection() {
        StringBuilder sb = new StringBuilder();
        sb.append("#Test if user wants silent includes\n")
                .append("if [ \"$1\" == \"silent\" ]\n")
                .append("then\n")
                .append("\tSILENT=\"-silent \"\n")
                .append("\tshift\n")
                .append("else\n")
                .append("\tSILENT=\"\"\n") // this is the space between the .jar and the first command
                .append("fi");
        return sb.toString();
    }

    private String getOptsDeclaration() {
        StringBuilder sb = new StringBuilder();
        sb.append("while getopts ");
        for (Option option : optionArrayList) {
            sb.append(option.getOptionChar())
                    .append(':');
        }
        sb.append("D:X:h");
        sb.append(" the_opt");
        return sb.toString();
    }

    private String genOptsSection() {
        StringBuilder sb = new StringBuilder();
        sb.append("# Process options").append("\n")
                .append(getOptsDeclaration())
                .append("\n")
                .append("do\n").append("\tcase \"$the_opt\" in\n");
        for (Option option : optionArrayList) {
            sb.append(option.getKshopt()).append('\n');
        }
        sb.append("\t\th)\tusage;exit 0;;\n");
        sb.append("\t\tD)\tJVMPROPS=\"${JVMPROPS} -D${OPTARG}\";;\n");
        sb.append("\t\tX)\tJVMOPTS=\"${JVMOPTS} -X${OPTARG}\";;\n");
        sb.append("\t\t[?])\tusage;exit 2;;\n");
        return sb.toString();
    }

    private String genSuperfluousArgumentWarning() {
        StringBuilder sb = new StringBuilder();
        sb.append("\tesac\ndone\nshift `expr ${OPTIND} - 1`\n")
                .append("if [ $# -ne 0 ]\n")
                .append("then\n")
                .append("\techo \"Superfluous argument(s) $*\"\n")
                .append("\tusage\n")
                .append("\texit 2\n")
                .append("fi\n");
        return sb.toString();
    }

    private String genScriptDir() {
        // SCRIPTDIR variable generation, for use in ublu.includepath; the cd
        // && pwd pair is used because a simple dirname fails on relative calls
        return "SCRIPTDIR=$(CDPATH= cd \"$(dirname \"$0\")\" && pwd)\n";
    }

    private String genPreludeCommands() {
        StringBuilder sb = new StringBuilder("# Prelude commands to execute before invocation\n");
        if (preludeCommandList.isEmpty()) {
            sb.append("# No prelude commands\n");
        } else {
            for (String cmd : preludeCommandList) {
                sb.append(cmd).append('\n');
            }
        }
        return sb.toString();
    }

    private String genOptionsString() {
        StringBuilder sb = new StringBuilder();
        sb.append("# Translate options to tuple assignments").append("\n");
        for (Option option : optionArrayList) {
            if (!(option instanceof OptScriptOnly)) {
                sb.append("if [ \"${").append(option.getAssignedName()).append("}\" != \"\" ]\n");
                sb.append("then\n");
                sb.append("\tgensh_runtime_opts=\"${gensh_runtime_opts}").append(option.putting()).append(" \"\n");
                if (option instanceof OptMulti) {
                    sb.append("else\n");
                    sb.append("\tgensh_runtime_opts=\"${gensh_runtime_opts}").append("tuple -null ").append(option.getTupleName()).append(" \"\n");
                } else if (option.isRequired()) {
                    sb.append("else\n");
                    sb.append("\techo \"Option ")
                            .append(option.optionAndParam())
                            .append(" is a required option but is not present.\"\n");
                    sb.append("\tusage\n");
                    sb.append("\texit 2\n");
                }
                sb.append("fi\n");
            }
        }
        return sb.toString();
    }

    private String genInvocation() {
        StringBuilder sb = new StringBuilder("# Invocation\n");
        sb.append("java${JVMOPTS}${JVMPROPS} ")
                .append("-Dublu.includepath=\"")
                .append(includePath)
                .append("\" -jar ")
                .append(fqJarPath)
                .append(' ')
                .append("${gensh_runtime_opts} ")
                .append("include ").append("${SILENT}").append(includeName).append(" ")
                .append(getFunctionInvocation().replace("(", "\\(").replace(")", "\\)"));
        return sb.toString();
    }

    /**
     * Generate the script
     *
     * @return the script
     */
    public String genSh() {
        StringBuilder sb = new StringBuilder();
        sb.append("# ").append(getScriptName()).append("\n")
                .append("# Ublu ").append(Ublu.ubluVersion()).append(" gensh autogenerated this shell script ").append(dateGenerated)
                .append(" for ").append(generatingUser)
                .append(" using command:\n")
                .append("# ").append(accumulatedCommand).append("\n\n")
                .append(genUsage())
                .append("\n\n")
                .append(genGlobSection())
                .append("\n\n")
                .append(genSilentSection())
                .append("\n\n")
                .append(genOptsSection())
                .append("\n")
                .append(genSuperfluousArgumentWarning())
                .append("\n")
                .append(genOptionsString()).append("\n")
                .append(genScriptDir()).append("\n")
                .append(genPreludeCommands()).append('\n')
                .append(genInvocation())
                .append("\nexit $?\n");
        return sb.toString();
    }

    /**
     * Shared by options
     */
    public interface Option {

        /**
         * Get the dash-decorated option
         *
         * @return the dash-decorated option
         */
        public String option();

        /**
         * Get the dash-decorated option and its parameter
         *
         * @return the dash-decorated option and its parameter
         */
        public String optionAndParam();

        /**
         * Generate the code for the individual case in the shell script options
         * processing case statement.
         *
         * @return the code for the individual case in the bash shell script
         * options processing case statement.
         */
        public String getopt();

        /**
         * Generate the code for the individual case in the shell script options
         * processing case statement.
         *
         * @return the code for the individual case in the ksh shell script
         * options processing case statement.
         */
        public String getKshopt();

        /**
         * Generate the usage documentation for this option for the script usage
         * () function
         *
         * @return the usage documentation for the script usage () function
         */
        public String usage();

        /**
         * Generate the tuple assignment statement for this option
         *
         * @return the tuple assignment statement for this option
         */
        public String putting();

        /**
         * Get the undecorated option char
         *
         * @return the undecorated option char
         */
        public char getOptionChar();

        /**
         * Get the shell variable assignment name for this option
         *
         * @return the shell variable assignment name for this option
         */
        public String getAssignedName();

        /**
         * Get the name of the tuple to which this option's runtime value should
         * be assigned
         *
         * @return the name of the tuple to which this option's runtime value
         * should be assigned
         */
        public String getTupleName();

        /**
         * Get option description
         *
         * @return option description
         */
        public String getDescription();

        /**
         * Indicate if this option is required
         *
         * @return true if this option is required, false otherwise
         */
        public boolean isRequired();

        /**
         * Set if this option is required
         *
         * @param required true if this option is required, false otherwise
         */
        public void setRequired(boolean required);
    }

    /**
     * Individual option that will be scripted to assign one value via a tuple.
     */
    public static class Opt implements Option {

        private char optChar;
        private String assignedName;
        private String tupleName;
        private String description;
        private Boolean required;
        /**
         * Used to indicate to the Opt ctor that an option is a required option
         */
        public static boolean REQUIRED = true;

        /**
         * Ctor instances all factors
         *
         * @param optChar option character
         * @param assignedName name for shell var assignment
         * @param tupleName name for tuple assignment
         * @param description text description for usage message
         */
        public Opt(char optChar, String assignedName, String tupleName, String description) {
            this(optChar, assignedName, tupleName, description, false);
        }

        /**
         * Ctor instances all factors
         *
         * @param optChar option character
         * @param assignedName name for shell var assignment
         * @param tupleName name for tuple assignment
         * @param description text description for usage message
         * @param required true if this is a required option
         */
        public Opt(char optChar, String assignedName, String tupleName, String description, boolean required) {
            this.optChar = optChar;
            this.description = description;
            this.assignedName = assignedName;
            this.tupleName = tupleName;
            setRequired(required);
        }

        @Override
        public String usage() {
            StringBuilder sb = new StringBuilder();
            sb.append(option()).append(" ").append(assignedName).append("\t...\t").append(description);
            return sb.toString();
        }

        @Override
        public String option() {
            StringBuilder sb = new StringBuilder();
            sb.append("-").append(optChar);
            return sb.toString();
        }

        @Override
        public String optionAndParam() {
            StringBuilder sb = new StringBuilder();
            sb.append(option())
                    .append(" ")
                    .append(getAssignedName());
            return sb.toString();
        }

        @Override
        public String putting() {
            StringBuilder sb = new StringBuilder();
            sb.append("string -to ").append(getTupleName()).append(" -trim ").append("\\${ ${").append(getAssignedName()).append("} }$");
            return sb.toString();
        }

        @Override
        public String getopt() {
            StringBuilder sb = new StringBuilder();
            sb.append("\t\t")
                    .append(option()).append(")")
                    .append("\n\t\t\t")
                    .append(getAssignedName())
                    .append("=\"$2\";shift;\n\t\t\tshift;;");
            return sb.toString();
        }

        @Override
        public String getKshopt() {
            StringBuilder sb = new StringBuilder();
            sb.append("\t\t")
                    .append(getOptionChar()).append(")\t")
                    .append(getAssignedName())
                    .append("=\"$OPTARG\";;");
            return sb.toString();
        }

        @Override
        public char getOptionChar() {
            return optChar;
        }

        @Override
        public String getAssignedName() {
            return assignedName;
        }

        @Override
        public String getTupleName() {
            return tupleName;
        }

        @Override
        public String getDescription() {
            return description;
        }

        @Override
        public boolean isRequired() {
            return this.required;
        }

        @Override
        public final void setRequired(boolean required) {
            this.required = required;
        }
    }

    /**
     * Single option that will be scripted to assign multiple values in a quoted
     * string via a tuple.
     */
    public static class OptMulti extends Opt {

        /**
         * Ctor instances all factors
         *
         * @param optChar option character
         * @param assignedName name for shell var assignment
         * @param tupleName name for tuple assignment
         * @param description text description for usage message
         */
        public OptMulti(char optChar, String assignedName, String tupleName, String description) {
            super(optChar, assignedName, tupleName, description);
        }

        @Override
        public String getopt() {
            StringBuilder sb = new StringBuilder();
            sb.append("\t\t")
                    .append(option()).append(")")
                    .append("\n\t\t\t")
                    .append(getAssignedName())
                    .append("=\"$2 ${")
                    .append(getAssignedName())
                    .append("}\";shift;\n\t\t\tshift;;");
            return sb.toString();
        }

        @Override
        public String getKshopt() {
            StringBuilder sb = new StringBuilder();
            sb.append("\t\t")
                    .append(getOptionChar()).append(")\t")
                    .append(getAssignedName())
                    .append("=\"$OPTARG ${")
                    .append(getAssignedName())
                    .append("}\";;");
            return sb.toString();
        }

        @Override
        public String optionAndParam() {
            StringBuilder sb = new StringBuilder();
            sb.append(option())
                    .append(" ")
                    .append(getAssignedName())
                    .append(" ")
                    .append("[")
                    .append(option())
                    .append(" ")
                    .append(getAssignedName())
                    .append(" ..]");
            return sb.toString();
        }
    }

    /**
     * Individual option that will be scripted to assign one value only for the
     * script, no tuple assignment in invocation.
     */
    public static class OptScriptOnly implements Option {

        private char optChar;
        private String assignedName;
        private String description;
        private Boolean required;

        /**
         * Ctor instances all factors
         *
         * @param optChar option character
         * @param assignedName name for shell var assignment
         * @param description text description for usage message
         */
        public OptScriptOnly(char optChar, String assignedName, String description) {
            this(optChar, assignedName, description, false);
        }

        /**
         * Ctor instances all factors
         *
         * @param optChar option character
         * @param assignedName name for shell var assignment
         * @param description text description for usage message
         * @param required true if this is a required option
         */
        public OptScriptOnly(char optChar, String assignedName, String description, boolean required) {
            this.optChar = optChar;
            this.description = description;
            this.assignedName = assignedName;
            setRequired(required);
        }

        @Override
        public String option() {
            StringBuilder sb = new StringBuilder();
            sb.append("-").append(optChar);
            return sb.toString();
        }

        @Override
        public String optionAndParam() {
            StringBuilder sb = new StringBuilder();
            sb.append(option())
                    .append(" ")
                    .append(getAssignedName());
            return sb.toString();
        }

        @Override
        public String getopt() {
            StringBuilder sb = new StringBuilder();
            sb.append("\t\t")
                    .append(option()).append(")")
                    .append("\n\t\t\t")
                    .append(getAssignedName())
                    .append("=\"$2\";shift;\n\t\t\tshift;;");
            return sb.toString();
        }

        @Override
        public String getKshopt() {
            StringBuilder sb = new StringBuilder();
            sb.append("\t\t")
                    .append(getOptionChar()).append(")\t")
                    .append(getAssignedName())
                    .append("=\"$OPTARG\";;");
            return sb.toString();
        }

        @Override
        public String usage() {
            StringBuilder sb = new StringBuilder();
            sb.append(option()).append(" ").append(assignedName).append("\t...\t").append(description);
            return sb.toString();
        }

        @Override
        public String putting() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public char getOptionChar() {
            return optChar;
        }

        @Override
        public String getAssignedName() {
            return assignedName;
        }

        @Override
        public String getTupleName() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public String getDescription() {
            return description;
        }

        @Override
        public boolean isRequired() {
            return this.required;
        }

        @Override
        public final void setRequired(boolean required) {
            this.required = required;
        }

    }

    /**
     * Class hold list of individual options each with its specification and
     * description.
     */
    public static class OptionArrayList extends ArrayList<Option> {
    }
}
