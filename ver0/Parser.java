package lava6.ver0;
import java.util.*;
/**
* The Parser converts the stream of symbols into a grammar tree.
*
* This does not match the formal PL/0 grammar.
* Some differences:
*	- does not have ODD operator
*	- does not allow multiple CONST or VAR items to be combined
*	- does not allows nested procedures
*	- does not consider CONST or VAR to be part of a block.  A block can be considered scope,
*	but we do not allow additional variables in the scope
*	- allows but does not require program to end in a period.  note that the period
*	is not used at all except for at this point
*	- Pl/0 has strict rules about semicolons, when they are required and when not. This
*	notes but does not require semicolons
*
* Note that the WRITE statement only works with variables, not with numbers.
*/
public class Parser {

	//program is the root of the grammar tree
	Grammar.Program program;
	Iterator<Symbol> symbols;
	/**
	* The program name comes from the file name
	*/
	public Parser(String programName,SymbolList syms) {
		program = new Grammar.Program(programName);
		symbols = syms.iterator();
	}

	public Grammar.Program getTree() {return program;}

	public void run() throws Grammar.GrammarException {
		//top level.  look for 4 things: Const Var Procedure MainBlock. Also period  Everything else is embedded
		Symbol sym = null;
		while (symbols.hasNext()) {
			sym = symbols.next();
			if (sym.getSymbolType() == Symbol.CONST) {
				doConst();
			} else if (sym.getSymbolType() == Symbol.VAR) {
				doVar(program);
			} else if (sym.getSymbolType() == Symbol.PROCEDURE) {
				doProcedure();
			} else if (sym.getSymbolType() == Symbol.BEGIN) {
				//main program block
				doMain();
			} else if (sym.getSymbolType() == Symbol.PERIOD || sym.getSymbolType() == Symbol.EOF ) {
				//we are done, end of program, nothing to do
				return;
			} else if (sym.getSymbolType() == Symbol.EOL) {
				//sometimes EOL will have meaning, but not here
				continue;
			} else if (sym.getSymbolType() == Symbol.SEMICOLON) {
				System.out.println("WARNING: unexpected semicolon in top lovel program definition");
				continue;
			} else {
				throw new Grammar.GrammarException("unexpected symbol "+sym.getSymbolTypeName()+" in program definition");
			}
		}
	}

	/**
	* A Const line begins with Const and ends with either EOL or semicolon
	* we don't allow multiple const on one line
	*/
	public void doConst() throws Grammar.GrammarException {
		Grammar.ConstDef k = new Grammar.ConstDef();
		//read in the name
		Symbol n = symbols.next();
		k.name = n.getName();
		if (k.name == null) throw new Grammar.GrammarException("name in CONST def is null");
		//read in the equal
		Symbol eq = symbols.next();
		Symbol val = symbols.next();
		k.value = val.getNumber();

		//now read in the semicolon
		Symbol e = symbols.next();
		if (e.getSymbolType() != Symbol.SEMICOLON && e.getSymbolType() != Symbol.EOL) {
			throw new Grammar.GrammarException("CONST def does not end in semicolon");
		}
		program.addChild(k);
	}

	//pass in the program unit, either the program or procedure
	public void doVar(Grammar.Node parent) throws Grammar.GrammarException {
		Grammar.VarDef v = new Grammar.VarDef();
		//read in the name
		Symbol n = symbols.next();
		v.name = n.getName();
		if (v.name == null) throw new Grammar.GrammarException("name in VAR def is null");

		//now read in the semicolon
		Symbol e = symbols.next();
		if (e.getSymbolType() != Symbol.SEMICOLON && e.getSymbolType() != Symbol.EOL) {
			throw new Grammar.GrammarException("VAR def does not end in semicolon");
		}
		parent.addChild(v);
	}

	//a Procedure starts with the keyword PROCEDURE and ends with END;
	public void doProcedure() throws Grammar.GrammarException {
		Grammar.Procedure p = new Grammar.Procedure(program);
		Symbol sym = null;

		//first get the name of the procedure
		sym = symbols.next();
		p.name = sym.getName();
		if (p.name == null) throw new Grammar.GrammarException("name in Procedure header is null");
		//now read in the semicolon
		Symbol e = symbols.next();
		if (e.getSymbolType() != Symbol.SEMICOLON && e.getSymbolType() != Symbol.EOL) {
			throw new Grammar.GrammarException("VAR def does not end in semicolon");
		}

		//look for optional VAR def
		while (symbols.hasNext()) {
			sym = symbols.next();
			if (sym.getSymbolType() == Symbol.VAR) {
				doVar(p);
			} else if (sym.getSymbolType() == Symbol.BEGIN) {
				Grammar.Block b = getBlock(p);
				p.addChild(b);
				//once the block has ended, the procedure is done
				break;
			} else if (sym.getSymbolType() == Symbol.EOL) {
				//sometimes EOL will have meaning, but not here
				continue;
			} else if (sym.getSymbolType() == Symbol.SEMICOLON) {
				System.out.println("WARNING: unexpected semicolon in procedure definition");
				continue;
			} else {
				throw new Grammar.GrammarException("unexpected symbol "+sym.getSymbolTypeName()+" in procedure definition");
			}
		}
		program.addChild(p);
	}

	//the Begin symbol has already been read in at this point
	public Grammar.Block getBlock(Grammar.BlockHolder parent) throws Grammar.GrammarException {
		Grammar.Block b = new Grammar.Block(parent);
		Symbol sym = null;
		while (symbols.hasNext()) {
			sym = symbols.next();
			//there are 5 types of statements in a block:
			//	Assignment
			//	CallStatement
			//	WriteStatement
			//	IfStatement
			//	WhileStatement
			//	also look for END
			if (sym.getSymbolType() == Symbol.IDENT) {
				doAssignment(b,sym);
			} else if (sym.getSymbolType() == Symbol.CALL) {
				doCall(b);
			} else if (sym.getSymbolType() == Symbol.WRITE) {
				doWrite(b);
			} else if (sym.getSymbolType() == Symbol.IF) {
				doIf(b);
			} else if (sym.getSymbolType() == Symbol.WHILE) {
				doWhile(b);
			} else if (sym.getSymbolType() == Symbol.END) {
				break;
			} else if (sym.getSymbolType() == Symbol.EOL) {
				//sometimes EOL will have meaning, but not here
				continue;
			} else if (sym.getSymbolType() == Symbol.SEMICOLON) {
				System.out.println("WARNING: unexpected semicolon in block");
				continue;
			} else {
				throw new Grammar.GrammarException("unexpected symbol '"+sym.getSymbolTypeName()+"' in block");
			}
		}
		return b;
	}

	//this is actually quite complicated
	public void doAssignment(Grammar.Block parent,Symbol ident) throws Grammar.GrammarException {
		Grammar.Assignment a = new Grammar.Assignment();
		a.varName = ident.getName();

		//get the assignment symbol
		Symbol sym = symbols.next();
		if (sym.getSymbolType() != Symbol.BECOMES) {
			throw new Grammar.GrammarException("expecting BECOMES (:=) operator in assignment statement");
		}
		//now promptly throw it away
		a.body = getExpression(a);
		parent.addChild(a);
	}

	/**
	* An Expression can be a lot of stuff - numbers, variables, operators, parenthesis
	* We don't strictly check it except to exclude the obvious
	*/
	public Grammar.Expression getExpression(Grammar.Assignment parent)  throws Grammar.GrammarException {
		Grammar.Expression x = new Grammar.Expression(parent);
		Symbol sym = null;
		while (symbols.hasNext()) {
			sym = symbols.next();
			if (sym.getSymbolType() == Symbol.EOL) {
				break;
			} else if (sym.getSymbolType() > 18) {
				throw new Grammar.GrammarException("invalid symbol "+sym.getSymbolTypeName()+" in expression");
			} else {
				x.addSymbol(sym);
			}
		}
		return x;
	}

	public void doCall(Grammar.Block parent) throws Grammar.GrammarException {
		Grammar.CallStatement c = new Grammar.CallStatement();
		Symbol sym = symbols.next();
		c.procName = sym.getName();
		if (c.procName == null) throw new Grammar.GrammarException("procedure name in call statement is null");
		parent.addChild(c);
	}

	//the value of Write must be a variable
	public void doWrite(Grammar.Block parent) throws Grammar.GrammarException {
		Grammar.WriteStatement w = new Grammar.WriteStatement();
		Symbol sym = symbols.next();
		w.varName = sym.getName();
		if (w.varName == null) throw new Grammar.GrammarException("variable name in write statement is null");
		parent.addChild(w);
	}

	//the condition doesn't contain parentheses, but we should be able to handle them
	public void doIf(Grammar.Block parent) throws Grammar.GrammarException {
		Grammar.IfStatement gif = new Grammar.IfStatement(parent);
		Grammar.Condition cond = getCondition();
		gif.condition = cond;
		//now then and body
		Symbol t = symbols.next();
		if (t.getSymbolType() != Symbol.THEN) {
			throw new Grammar.GrammarException("expecting THEN in IF statement");
		}
		Symbol e = symbols.next();
		if (e.getSymbolType() != Symbol.EOL) {
			throw new Grammar.GrammarException("expecting EOL after THEN in IF statement");
		}
		//get block
		Symbol beg = symbols.next();
		if (beg.getSymbolType() != Symbol.BEGIN) {
			throw new Grammar.GrammarException("expecting BEGIN in IF statement");
		}
		gif.body = getBlock(gif);
		parent.addChild(gif);
	}

	public Grammar.Condition getCondition() throws Grammar.GrammarException {
		Grammar.Condition c = new Grammar.Condition();
		Symbol var = symbols.next();
		if (var.getSymbolType() != Symbol.IDENT) {
			throw new Grammar.GrammarException("expecting variable name in condition");
		}
		c.varName = var.getName();
		//get comparison
		c.comparison = symbols.next();
		//get value
		c.value = symbols.next();
		if (c.value.getSymbolType() != Symbol.IDENT && c.value.getSymbolType() != Symbol.NUMBER) {
			throw new Grammar.GrammarException("unexpected value "+c.value.getSymbolTypeName()+" in condition");
		}
		return c;
	}

	public void doWhile(Grammar.Block parent) throws Grammar.GrammarException {
		Grammar.WhileStatement w = new Grammar.WhileStatement(parent);
		Grammar.Condition cond = getCondition();
		w.condition = cond;
		//now then and body
		Symbol t = symbols.next();
		if (t.getSymbolType() != Symbol.DO) {
			throw new Grammar.GrammarException("expecting DO in WHILE statement");
		}
		//get block
		Symbol e = symbols.next();
		if (e.getSymbolType() != Symbol.EOL) {
			throw new Grammar.GrammarException("expecting EOL after DO in WHILE statement");
		}
		Symbol beg = symbols.next();
		if (beg.getSymbolType() != Symbol.BEGIN) {
			throw new Grammar.GrammarException("expecting BEGIN in WHILE statement");
		}
		w.body = getBlock(w);
		parent.addChild(w);
	}

	//in pl/0, the main block is unnamed.  We turn this into a procedure named MAIN.  This procedure
	//doesn't have any vars, because they are defined in the parent program
	public void doMain() throws Grammar.GrammarException {
		Grammar.Procedure m = new Grammar.Procedure(program);
		m.name = "MAIN";
		Grammar.Block b = getBlock(m);
		m.addChild(b);
		program.addChild(m);
	}

}