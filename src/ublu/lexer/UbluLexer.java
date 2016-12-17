// Generated from ./src/ublu/lexer/Ublu.g4 by ANTLR 4.5.3

package ublu.lexer;

import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.TokenStream;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.atn.*;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.misc.*;

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast"})
public class UbluLexer extends Lexer {
	static { RuntimeMetaData.checkVersion("4.5.3", RuntimeMetaData.VERSION); }

	protected static final DFA[] _decisionToDFA;
	protected static final PredictionContextCache _sharedContextCache =
		new PredictionContextCache();
	public static final int
		STRING=1, WS=2, COMMENT=3, WORD=4;
	public static String[] modeNames = {
		"DEFAULT_MODE"
	};

	public static final String[] ruleNames = {
		"STRING", "ESC", "UNICODE", "HEX", "WS", "COMMENT", "WORD"
	};

	private static final String[] _LITERAL_NAMES = {
	};
	private static final String[] _SYMBOLIC_NAMES = {
		null, "STRING", "WS", "COMMENT", "WORD"
	};
	public static final Vocabulary VOCABULARY = new VocabularyImpl(_LITERAL_NAMES, _SYMBOLIC_NAMES);

	/**
	 * @deprecated Use {@link #VOCABULARY} instead.
	 */
	@Deprecated
	public static final String[] tokenNames;
	static {
		tokenNames = new String[_SYMBOLIC_NAMES.length];
		for (int i = 0; i < tokenNames.length; i++) {
			tokenNames[i] = VOCABULARY.getLiteralName(i);
			if (tokenNames[i] == null) {
				tokenNames[i] = VOCABULARY.getSymbolicName(i);
			}

			if (tokenNames[i] == null) {
				tokenNames[i] = "<INVALID>";
			}
		}
	}

	@Override
	@Deprecated
	public String[] getTokenNames() {
		return tokenNames;
	}

	@Override

	public Vocabulary getVocabulary() {
		return VOCABULARY;
	}


	public UbluLexer(CharStream input) {
		super(input);
		_interp = new LexerATNSimulator(this,_ATN,_decisionToDFA,_sharedContextCache);
	}

	@Override
	public String getGrammarFileName() { return "Ublu.g4"; }

	@Override
	public String[] getRuleNames() { return ruleNames; }

	@Override
	public String getSerializedATN() { return _serializedATN; }

	@Override
	public String[] getModeNames() { return modeNames; }

	@Override
	public ATN getATN() { return _ATN; }

	@Override
	public void action(RuleContext _localctx, int ruleIndex, int actionIndex) {
		switch (ruleIndex) {
		case 0:
			STRING_action((RuleContext)_localctx, actionIndex);
			break;
		}
	}
	private void STRING_action(RuleContext _localctx, int actionIndex) {
		switch (actionIndex) {
		case 0:

			    String str = getText();
			    StringBuilder output = new StringBuilder();
			    str = str.substring(1, str.length() - 1);
			    while (!str.isEmpty()) {
			        int index = str.indexOf("\\");
			        if (index == -1) {
			            output.append(str);
			            break;
			        } else {
			            int skip = 2;
			            output.append(str.substring(0, index));
			            switch (str.charAt(index + 1)) {
			                case '\\':
			                    output.append('\\');
			                    break;
			                case '"':
			                    output.append('"');
			                    break;
			                case 'b':
			                    output.append('\b');
			                    break;
			                case 'f':
			                    output.append('\f');
			                    break;
			                case 'n':
			                    output.append('\n');
			                    break;
			                case 'r':
			                    output.append('\r');
			                    break;
			                case 't':
			                    output.append('\t');
			                    break;
			                case 'u':
			                    skip = 6;
			                    String chars = str.substring(index + 2, index + 6);
			                    int codepoint = Integer.parseInt(chars, 16);
			                    output.append(Character.toChars(codepoint));
			                    break;
			            }
			            str = str.substring(index + skip);
			        }
			    }
			    setText(output.toString());
			    
			break;
		}
	}

	public static final String _serializedATN =
		"\3\u0430\ud6d1\u8206\uad2d\u4417\uaef1\u8d80\uaadd\2\6I\b\1\4\2\t\2\4"+
		"\3\t\3\4\4\t\4\4\5\t\5\4\6\t\6\4\7\t\7\4\b\t\b\3\2\3\2\3\2\7\2\25\n\2"+
		"\f\2\16\2\30\13\2\3\2\3\2\3\2\3\3\3\3\3\3\5\3 \n\3\3\4\3\4\3\4\3\4\3\4"+
		"\3\4\3\5\3\5\3\6\6\6+\n\6\r\6\16\6,\3\6\3\6\3\7\3\7\7\7\63\n\7\f\7\16"+
		"\7\66\13\7\3\7\3\7\3\b\3\b\3\b\3\b\3\b\3\b\3\b\3\b\3\b\3\b\6\bD\n\b\r"+
		"\b\16\bE\5\bH\n\b\2\2\t\3\3\5\2\7\2\t\2\13\4\r\5\17\6\3\2\b\6\2\n\f\16"+
		"\17$$^^\t\2$$^^ddhhppttvv\5\2\62;CHch\5\2\13\f\17\17\"\"\4\2\f\f\17\17"+
		"\13\2\13\f\17\17\"\"$$&&*+]_}}\177\177P\2\3\3\2\2\2\2\13\3\2\2\2\2\r\3"+
		"\2\2\2\2\17\3\2\2\2\3\21\3\2\2\2\5\34\3\2\2\2\7!\3\2\2\2\t\'\3\2\2\2\13"+
		"*\3\2\2\2\r\60\3\2\2\2\17G\3\2\2\2\21\26\7$\2\2\22\25\5\5\3\2\23\25\n"+
		"\2\2\2\24\22\3\2\2\2\24\23\3\2\2\2\25\30\3\2\2\2\26\24\3\2\2\2\26\27\3"+
		"\2\2\2\27\31\3\2\2\2\30\26\3\2\2\2\31\32\7$\2\2\32\33\b\2\2\2\33\4\3\2"+
		"\2\2\34\37\7^\2\2\35 \t\3\2\2\36 \5\7\4\2\37\35\3\2\2\2\37\36\3\2\2\2"+
		" \6\3\2\2\2!\"\7w\2\2\"#\5\t\5\2#$\5\t\5\2$%\5\t\5\2%&\5\t\5\2&\b\3\2"+
		"\2\2\'(\t\4\2\2(\n\3\2\2\2)+\t\5\2\2*)\3\2\2\2+,\3\2\2\2,*\3\2\2\2,-\3"+
		"\2\2\2-.\3\2\2\2./\b\6\3\2/\f\3\2\2\2\60\64\7%\2\2\61\63\n\6\2\2\62\61"+
		"\3\2\2\2\63\66\3\2\2\2\64\62\3\2\2\2\64\65\3\2\2\2\65\67\3\2\2\2\66\64"+
		"\3\2\2\2\678\b\7\3\28\16\3\2\2\29H\4*+\2:;\7&\2\2;H\7]\2\2<=\7_\2\2=H"+
		"\7&\2\2>?\7&\2\2?H\7}\2\2@A\7\177\2\2AH\7&\2\2BD\n\7\2\2CB\3\2\2\2DE\3"+
		"\2\2\2EC\3\2\2\2EF\3\2\2\2FH\3\2\2\2G9\3\2\2\2G:\3\2\2\2G<\3\2\2\2G>\3"+
		"\2\2\2G@\3\2\2\2GC\3\2\2\2H\20\3\2\2\2\n\2\24\26\37,\64EG\4\3\2\2\b\2"+
		"\2";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}