package lava6.ver0;
import java.io.*;

/**
* Input: source code in PL/0 format
* Output: list of symbols
*/
public class Lexer {
	public static void main(String[] args) throws Exception {
		Lexer t = new Lexer(args[0]);
	}

	StreamTokenizer toker;
	SymbolList list;

	public Lexer(String fileName) throws FileNotFoundException {
		toker = new StreamTokenizer(new FileReader(fileName));
		toker.ordinaryChar(47); //make forward slash an ordinary char
		toker.slashSlashComments(true);	//allow double slash comments
		toker.eolIsSignificant(true);
		list = new SymbolList(128);
	}

	public SymbolList getList() {return list;}

	//swallow the exception
	private int nextToken() {
		try {
			return toker.nextToken();
		} catch (IOException x) {
			System.out.println(x.getMessage());
			return -32768;
		}
	}

	public void run() {
		int tok = 0;
		Symbol sym = null;
		String word = "";
		tok=nextToken();

		while (tok!= StreamTokenizer.TT_EOF) {
			//System.out.println("DEBUG: tok = "+tok);
			switch (tok) {
				case StreamTokenizer.TT_NUMBER:
					sym = new Symbol(Symbol.NUMBER);
					sym.setNumber((int)toker.nval);
					list.add(sym);
					break;
				case StreamTokenizer.TT_EOL:
					sym = new Symbol(Symbol.EOL);
					list.add(sym);
					break;
				case StreamTokenizer.TT_WORD:
					word = toker.sval;
					if (word.equalsIgnoreCase("CONST")) {
						sym = new Symbol(Symbol.CONST);
					} else if (word.equalsIgnoreCase("VAR")) {
						sym = new Symbol(Symbol.VAR);
					} else if (word.equalsIgnoreCase("PROCEDURE")) {
						sym = new Symbol(Symbol.PROCEDURE);
					} else if (word.equalsIgnoreCase("IF")) {
						sym = new Symbol(Symbol.IF);
					} else if (word.equalsIgnoreCase("THEN")) {
						sym = new Symbol(Symbol.THEN);
					} else if (word.equalsIgnoreCase("ELSE")) {
						sym = new Symbol(Symbol.ELSE);
					} else if (word.equalsIgnoreCase("WHILE")) {
						sym = new Symbol(Symbol.WHILE);
					} else if (word.equalsIgnoreCase("DO")) {
						sym = new Symbol(Symbol.DO);
					} else if (word.equalsIgnoreCase("CALL")) {
						sym = new Symbol(Symbol.CALL);
					} else if (word.equalsIgnoreCase("BEGIN")) {
						sym = new Symbol(Symbol.BEGIN);
					} else if (word.equalsIgnoreCase("END")) {
						sym = new Symbol(Symbol.END);
					} else if (word.equalsIgnoreCase("WRITE")) {
						sym = new Symbol(Symbol.WRITE);
					} else if (word.equalsIgnoreCase("END.")) {
						//store this as two tokens, END and EOF
						list.add(new Symbol(Symbol.END));
						sym = new Symbol(Symbol.EOF);
					} else {
						//assume it is an ident
						sym = new Symbol(Symbol.IDENT);
						sym.setName(word);
					}
					list.add(sym);
					break;
				case (int)'!':
					sym = new Symbol(Symbol.WRITE);
					list.add(sym);
					break;
				case (int)';':
					//even though semicolons are ignored most of the time
					//record them
					sym = new Symbol(Symbol.SEMICOLON);
					list.add(sym);
					break;
            	case (int)'+':
            		sym = new Symbol(Symbol.PLUS);
            		list.add(sym);
            		break;
            	case (int)'-':
            		sym = new Symbol(Symbol.MINUS);
            		list.add(sym);
            		break;
            	case (int)'*':
            		sym = new Symbol(Symbol.TIMES);
            		list.add(sym);
            		break;
            	case (int)'/':
            		sym = new Symbol(Symbol.DIVIDE);
            		list.add(sym);
            		break;
            	case (int)'=':
            		tok = nextToken();
            		if (tok == 61 ) {
						sym = new Symbol(Symbol.BOOL_EQUAL);
					} else {
						toker.pushBack();
            			sym = new Symbol(Symbol.EQUAL);
					}
            		list.add(sym);
            		break;
            	case (int)'<':
            		tok = nextToken();
            		if (tok == 62) {  // >
						sym = new Symbol(Symbol.NOT_EQUAL);
					} else if (tok == 61 ) { // =
						sym = new Symbol(Symbol.LESS_OR_EQUAL);
					} else {
						toker.pushBack();
						sym = new Symbol(Symbol.LESS);
					}
					list.add(sym);
					break;
            	case (int)'>':
            		tok = nextToken();
            		if (tok == 61 ) { // =
						sym = new Symbol(Symbol.GREATER_OR_EQUAL);
					} else {
						toker.pushBack();
						sym = new Symbol(Symbol.GREATER);
					}
					list.add(sym);
					break;
            	case (int)'(':
            		sym = new Symbol(Symbol.LEFT_PAREN);
					list.add(sym);
					break;
            	case (int)')':
            		sym = new Symbol(Symbol.RIGHT_PAREN);
					list.add(sym);
					break;
            	case (int)',':
            		sym = new Symbol(Symbol.COMMA);
					list.add(sym);
					break;
            	case (int)'.':
            		sym = new Symbol(Symbol.PERIOD);
					list.add(sym);
					break;
            	case (int)':':
            		tok = nextToken();
            		if (tok == 61 ) { // =
            			sym = new Symbol(Symbol.BECOMES);
						list.add(sym);
					} else {
						System.out.println("WARNING: use of ':' without '=', ignoring");
					}
					break;
            	default:
					System.out.println("WARNING: unrecognized character "+tok+"("+(char)tok+")");
			} //end switch
			tok = nextToken();
		} //end while
	} // end run

}