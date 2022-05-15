package lava6.ver0;
import java.util.ArrayList;
import java.util.Iterator;
import java.io.PrintWriter;

/**
* This class contains the logical structure of a PlZero program.  It does not match exactly the
* EBNF grammar at https://en.wikipedia.org/wiki/PL/0
*
*/
public class Grammar {
	public static void say(String s) {System.out.println(s);}

	//add tabs for debugging
	public static void tab(int i) {
		for (int j=0;j<i;j++) {
			System.out.print("    ");
		}
	}

	public static void tab(PrintWriter w,int i) {
		for (int j=0;j<i;j++) {
			w.print("    ");
		}
	}

	//A Node has a parent.  Only the root node is null
	//The parent doesn't have to be a node.  While logically this may not make sense
	//remember a Node always allows for multiple children and the parent of a node may only
	//have one child
	public static class Node {
		Object parent;
		ArrayList children;
		public Node(Object p) {
			parent = p;
			children = new ArrayList();
		}
		public Object getParent() {return parent;}
		public void addChild(Object o) {
			children.add(o);
		}
		public Iterator iterator() {
			return children.iterator();
		}
	}

	//==========================
	//A BlockHolder is an element that holds a block
	public static interface BlockHolder {
		public Object getParent();
		public void setBlock(Block b);
		public Block getBlock();
	}
	//==============================

	public static interface Dumper {
		public void dump(int i);
	}

	public static interface JavaOutput {
		public void write(PrintWriter w,int level);
	}

	//================================
	//The program will have one or more procedures, plus a Main block
	//the name of the program is in the file name, not in the file itself
	public static class Program extends Node implements BlockHolder, Dumper, JavaOutput {
		//the name of the program is in the filename, not in the file itself
		public String name;

		public Program(String n) {
			//program is a root node with no parent
			super(null);
			name = n;
		}

		//not implemented, use addChild or iterator
		public void setBlock(Block b) {}
		public Block getBlock() {return null;}

		public void dump(int i) {
			say("PROGRAM: "+name);
			Iterator it = iterator();
			i++;
			while (it.hasNext()) {
				Dumper d = (Dumper)it.next();
				d.dump(i);
			}
		}

		//the second parameter here is always 0
		public void write(PrintWriter pw,int i) {
			pw.println("public class "+name);
			pw.println("{");
			Iterator it = iterator();
			i++;
			while (it.hasNext()) {
				JavaOutput d = (JavaOutput)it.next();
				d.write(pw,i);
			}
			i--;
			pw.println("}");
		}
	}
	//======================================
	//ConstDef starts with Const.  There is one of these for each constant
	//
	public static class ConstDef implements Dumper, JavaOutput {
		public String name;
		public int value;

		public void dump(int i) {
			tab(i);
			say("CONST: "+name+" = "+value);
		}

		public void write(PrintWriter pw,int i) {
			tab(pw,i);
			pw.println("public static final int "+name+" = "+value+";");
		}
	}
	//=======================================
	public static class VarDef implements Dumper, JavaOutput {
		public String name;
		public void dump(int i) {
			tab(i);
			say("VAR: "+name);
		}

		public void write(PrintWriter pw,int i) {
			tab(pw,i);
			pw.println("public static int "+name+";");
		}
	}
	//=======================================
	//a procedure can have Vars but not constants
	//it has a Block
	//A procedure does not return a value
	public static class Procedure extends Node implements BlockHolder,Dumper,JavaOutput {
		public String name;

		public Procedure(Node parent) {
			super(parent);
		}

		//note that it adds the block so there could potentially be more
		//than one block which doesn't make logical sense
		public void setBlock(Block b) {
			addChild(b);
		}

		//this will probably never be used
		public Block getBlock() {
			Iterator it = iterator();
			Object child = null;
			Block b = null;
			while (it.hasNext()) {
				child = it.next();
				if (child instanceof Block) {
					b = (Block)child;
				}
			}
			return b;
		}

		public void dump(int i) {
			tab(i);
			say("PROCEDURE: "+name);
			Iterator it = iterator();
			i++;
			while (it.hasNext()) {
				Dumper d = (Dumper)it.next();
				d.dump(i);
			}
			i--;
		}

		public void write(PrintWriter pw,int i) {
			pw.println("");
			tab(pw,i);
			if (name.equalsIgnoreCase("MAIN")) {
				pw.println("public static void main(String[] args)");
			} else {
				pw.println("public static void "+name+"()");
			}
			tab(pw,i);
			pw.println("{");
			Iterator it = iterator();
			i++;
			while (it.hasNext()) {
				JavaOutput d = (JavaOutput)it.next();
				d.write(pw,i);
			}
			i--;
			tab(pw,i);
			pw.println("}");
		}
	}
	//=======================================
	//Anytime you see Begin .. End, that is a block
	//a block can contain:
	//	Assignment
	//	CallStatement
	//	WriteStatement
	//	IfStatement
	//	WhileStatement
	public static class Block extends Node implements Dumper,JavaOutput {
		public Block(BlockHolder parent) {super(parent);}

		public void dump(int i) {
			tab(i);
			say("BLOCK");
			i++;
			Iterator it = iterator();
			while (it.hasNext()) {
				Dumper d = (Dumper)it.next();
				d.dump(i);
			}
			i--;
		}

		//don't put the brackets around the block.  The outer layer does that
		public void write(PrintWriter pw,int i) {
			Iterator it = iterator();
			while (it.hasNext()) {
				JavaOutput d = (JavaOutput)it.next();
				d.write(pw,i);
			}
		}

	}
	//======================================
	//an expression is a mathematical formula, like "a + 2"
	//In this simple grammar, we are not evaluating it, just passing it through
	public static class Assignment implements Dumper,JavaOutput {
		public String varName;
		public Expression body;

		public void dump(int i) {
			tab(i);
			say("ASSIGNMENT: "+varName+" :=" + body.toString());
		}

		public void write(PrintWriter pw,int i) {
			tab(pw,i);
			pw.println(varName+" = " + body.toJavaString()+";");
		}
	}
	//=======================================
	//The expression is just the Symbols in the original file.
	//It is typically a mathematical formula. It can contain parentheses
	//An expression is evaluated and returns a value.
	//All we are doing with it is recreating it from the symbols
	public static class Expression extends Node   {
		//the parent of an expression is its Assignment
		public Expression(Object parent) {super(parent);}

		public void addSymbol(Symbol s) {
			addChild(s);
		}

		public String toString() {
			StringBuilder sb = new StringBuilder();
			sb.append("(EXPRESSION) ");
			Iterator it = iterator();
			while (it.hasNext()) {
				Symbol sym = (Symbol)it.next();
				if (sym.getSymbolType() == Symbol.NUMBER) {
					sb.append(String.valueOf(sym.getNumber()));
				} else if (sym.getSymbolType() == Symbol.IDENT) {
					sb.append(sym.getName());
				} else if (sym.getSymbolType() == Symbol.SEMICOLON) {
					//we are done
					break;
				} else {
					sb.append(sym.getSymbolTypeName());
				}
				sb.append(" ");
			}
			return sb.toString();
		}

		public String toJavaString() {
			StringBuilder sb = new StringBuilder();
			//sb.append("(EXPRESSION) ");
			Iterator it = iterator();
			while (it.hasNext()) {
				Symbol sym = (Symbol)it.next();
				if (sym.getSymbolType() == Symbol.NUMBER) {
					sb.append(String.valueOf(sym.getNumber()));
				} else if (sym.getSymbolType() == Symbol.IDENT) {
					sb.append(sym.getName());
				} else if (sym.getSymbolType() == Symbol.SEMICOLON) {
					//we are done
					break;
				} else {
					sb.append(sym.getSymbolTypeName());
				}
				sb.append(" ");
			}
			return sb.toString();
		}
	}

	//=======================================
	//in this simple version, we don't pass parameters
	public static class CallStatement implements Dumper, JavaOutput {
		//the procedure to call
		public String procName;

		public void dump(int i) {
			tab(i);
			say("CALL: "+procName);
		}

		public void write(PrintWriter pw,int i) {
			tab(pw,i);
			pw.println(procName+"();");
		}
	}
	//=====================================
	//writing just means printing to standard out
	public static class WriteStatement implements Dumper, JavaOutput {
		public String varName;
		public void dump(int i) {
			tab(i);
			say("WRITE: "+varName);
		}
		public void write(PrintWriter pw,int i) {
			tab(pw,i);
			pw.println("System.out.println("+varName+");");
		}
	}
	//=====================================
	//An IfStatements looks like IF condition THEN block
	//an IfStatement has a parent block, and it contains a block
	//it itself is a blockholder
	public static class IfStatement implements BlockHolder, Dumper, JavaOutput {
		public Condition condition;
		public Block body;
		public Block parent;

		public IfStatement(Block b) {parent = b;}
		public Object getParent() {return parent;}
		public void setBlock(Block b) {body = b;}
		public Block getBlock() {return body;}

		public void dump(int i) {
			tab(i);
			say("IF: "+condition.toString()+ " THEN");
			i++;
			body.dump(i);
			i--;
		}

		public void write(PrintWriter pw,int i) {
			tab(pw,i);
			pw.println("if ("+condition.toString()+ ")");
			tab(pw,i);
			pw.println("{");
			i++;
			body.write(pw,i);
			i--;
			tab(pw,i);
			pw.println("}");
		}
	}
	//======================================
	public static class WhileStatement implements BlockHolder, Dumper, JavaOutput {
		public Condition condition;
		public Block body;
		public Block  parent;

		public WhileStatement(Block bh) {parent = bh;}
		public Object getParent() {return parent;}
		public void setBlock(Block b) {body = b;}
		public Block getBlock() {return body;}

		public void dump(int i) {
			tab(i);
			say("WHILE: "+condition.toDumpString()+ " DO");
			i++;
			body.dump(i);
			i--;
		}

		public void write(PrintWriter pw,int i) {
			tab(pw,i);
			pw.println("while ("+condition.toJavaString()+ ")");
			tab(pw,i);
			pw.println("{");
			i++;
			body.write(pw,i);
			i--;
			tab(pw,i);
			pw.println("}");
		}

	}
	//=======================
	//we don't allow AND or OR here
	public static class Condition {
		public String varName;
		//one of the symbols showing comparison, like equal, not equal etc
		public Symbol comparison;
		//the value can either be a number or a variable (or constant)
		public Symbol value;

		public String toDumpString() {
			return "(CONDITION) "+varName+" "+comparison.getSymbolTypeName()+" "+valueString()+"";
		}

		public String toJavaString() {
			return varName+" "+comparison.getSymbolTypeName()+" "+valueString();
		}

		private String valueString() {
			if (value.getSymbolType() == Symbol.NUMBER) {
				return String.valueOf(value.getNumber());
			} else if (value.getSymbolType() == Symbol.IDENT) {
				return value.getName();
			} else {
				return "IMPROPER SYMBOL "+value.getSymbolTypeName()+ " in condition value";
			}
		}
	}

	public static class GrammarException extends Exception {
		public GrammarException(String m) {
			super(m);
		}
	}
}