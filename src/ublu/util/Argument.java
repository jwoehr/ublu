/*
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

/**
 * Holds an argument parsed from a command line. If it's a plain argument,
 * records the argument string and position. If it is a dash-option, records the
 * option string (-a -b etc.) and the argument to the option, if any. In any
 * case, records the position in the command line that the arg or opt-arg pair
 * came in.
 *
 * @author jax
 */
public class Argument {

    /**
     * The "option", that is, dash-letter, e.g., -a -b etc.
     */
    public String option;

    /**
     * The argument to the option, e.g., "-o full" where "full" is the argument
     * to the option "-o".
     */
    public String argument;

    /**
     * The position among the options-and-arguments in which this
     * option-and-argument appears.
     */
    public int position;

    /**
     * Create an Argument from an option, argument and position.
     *
     * @param option The command-line option
     * @param argument The command line arg
     * @param position The nth-ity of the entity.
     */
    public Argument(String option, String argument, int position) {
        this.option = option;
        this.argument = argument;
        this.position = position;
    }

    /**
     * Return the option and argument as a String.
     *
     * @return The string representation of the option and argument
     */
    @Override
    public String toString() {
        return option + " " + argument;
    }

    /**
     * Return the option portion (if any) of the Argument.
     *
     * @return The option itself.
     */
    public String getOption() {
        return option;
    }

    /**
     * Return the argument portion (if any) of the Argument.
     *
     * @return The argument itself.
     */
    public String getArgument() {
        return argument;
    }
}
