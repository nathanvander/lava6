package lava6.ver0;
import java.io.*;

/**
* This transpiles Pl/0 source code to Java source code
*/

public class Transpiler {
	public static void main(String[] args) throws Exception {
		Transpiler t = new Transpiler(args[0]);
		t.run();
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

	public Transpiler(String f) {
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
		//open output file
		PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(programName+".java")));
		prog.write(pw,0);
		pw.close();
	}
}