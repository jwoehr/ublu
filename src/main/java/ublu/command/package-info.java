/**
 * The elements of command execution in the {@link ublu.Ublu} interpreter.
 * All Cmd* classes extend {@link ublu.command.Command} and implement {@link ublu.command.CommandInterface}.
 * They all consume some or all of the {@link ublu.util.ArgArray} passed in and return
 * what's left of the ArgArray. If there's nothing left of the ArgArray the interpreter takes another
 * input loop. A command exits the current loop by discarding what's left of the ArgArray.
 */
package ublu.command;
