package lava6.ver0;
import java.io.*;


public class Dumper {
	public static void main(String[] args) throws Exception {
		Dumper r = new Dumper(args[0]);
		r.run();
	}

	public static String getBaseName(String f) {
		int i = f.indexOf('.');
		if (i==-1) {
			return f;
		} else {
			return f.substring(0,i);
		}
	}

	String fn;

	public Dumper(String f) {
		fn = f;
	}

	public void run() throws Exception {
		Lexer lex = new Lexer(fn);
		lex.run();
		SymbolList list = lex.getList();
		String programName = getBaseName(fn);
		Parser p = new Parser(programName,list);
		p.run();
		Grammar.Program prog = p.getTree();
		prog.dump(0);
	}
}