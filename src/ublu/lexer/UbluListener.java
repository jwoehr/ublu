// Generated from ./src/ublu/lexer/Ublu.g4 by ANTLR 4.5.3

package ublu.lexer;

import org.antlr.v4.runtime.tree.ParseTreeListener;

/**
 * This interface defines a complete listener for a parse tree produced by
 * {@link UbluParser}.
 */
public interface UbluListener extends ParseTreeListener {
	/**
	 * Enter a parse tree produced by {@link UbluParser#prog}.
	 * @param ctx the parse tree
	 */
	void enterProg(UbluParser.ProgContext ctx);
	/**
	 * Exit a parse tree produced by {@link UbluParser#prog}.
	 * @param ctx the parse tree
	 */
	void exitProg(UbluParser.ProgContext ctx);
	/**
	 * Enter a parse tree produced by {@link UbluParser#string}.
	 * @param ctx the parse tree
	 */
	void enterString(UbluParser.StringContext ctx);
	/**
	 * Exit a parse tree produced by {@link UbluParser#string}.
	 * @param ctx the parse tree
	 */
	void exitString(UbluParser.StringContext ctx);
	/**
	 * Enter a parse tree produced by {@link UbluParser#word}.
	 * @param ctx the parse tree
	 */
	void enterWord(UbluParser.WordContext ctx);
	/**
	 * Exit a parse tree produced by {@link UbluParser#word}.
	 * @param ctx the parse tree
	 */
	void exitWord(UbluParser.WordContext ctx);
}