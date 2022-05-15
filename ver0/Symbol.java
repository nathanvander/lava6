package lava6.ver0;

/**
* Modified from https://raw.githubusercontent.com/vincentbel/PL0-Compiler/master/src/com/vincentbel/compiler/Symbol.java
* Original Author: VincentBel
* Original Date: 2015/1/10
*/

public class Symbol {
    //public static final int SYMBOL_NUMBER = 35;
    //public static final int NUMBER_MAX = 1000000;

    /**------------------------------------
     * Punctuation
     * . , ; = := + - ( ) * / <> < <= > > >=
     * -------------------------------------
     */
    public static final int NUL = 0;                  // NULL
    public static final int PLUS = 1;                 // +
    public static final int MINUS = 2;                // -
    public static final int TIMES = 3;             // *
    public static final int DIVIDE = 4;               // /
    public static final int EQUAL = 5;                // =(equal)
    public static final int NOT_EQUAL = 6;            // <>(not equal)
    public static final int LESS = 7;                 // <(less)
    public static final int LESS_OR_EQUAL = 8;        // <=(less or equal)
    public static final int GREATER = 9;              // >(greater)
    public static final int GREATER_OR_EQUAL = 10;    // >=(greater or equal)
    public static final int LEFT_PAREN = 11;    	// (
    public static final int RIGHT_PAREN = 12;   	// )
    public static final int COMMA = 13;               // ,
    public static final int SEMICOLON = 14;           // ;
    public static final int PERIOD = 15;              // .
    public static final int BECOMES = 16;             // :=


    /**------------------------------------
     * Ident
     * This could be a const, var or procedure name
     * -------------------------------------
     */
    public static final int IDENT = 17;


    /**------------------------------------
     * Number
     * -------------------------------------
     */
    public static final int NUMBER = 18;


    /**------------------------------------
     * Keyword
     * const, var, procedure, odd,
     * if, then, else, while,
     * do, call, begin, end,
     * repeat, until, read, write
     * -------------------------------------
     */
    public static final int CONST = 19;
    public static final int VAR = 20;
    public static final int PROCEDURE = 21;
    //public static final int ODD = 22;
    public static final int IF = 23;
    public static final int THEN = 24;
    public static final int ELSE = 25;
    public static final int WHILE = 26;
    public static final int DO = 27;
    public static final int CALL = 28;
    public static final int BEGIN = 29;
    public static final int END = 30;
    //public static final int REPEAT = 31;
    //public static final int UNTIL = 32;
    //public static final int READ = 33;
    public static final int WRITE = 34;

	//not part of standard PL/0, allows you to use double equals with an if clause
	public static final int BOOL_EQUAL = 35;	// ==

	//end of line
	public static final int EOL = 36;
	public static final int EOF = 37;

    private int symbolType;
    private int number = 0;
    private String name = "";

    public Symbol(int symbolType) {
        this.symbolType = symbolType;
    }


    /*------------------------------
     * Getter
     * -----------------------------
     */

	//only used with Ident
    public String getName() {
        return name;
    }

    public int getSymbolType() {
        return symbolType;
    }

	//only used with numbers
    public int getNumber() {
        return number;
    }


    /*------------------------------
     * Setter
     * -----------------------------
     */

    public void setNumber(int number) {
        this.number = number;
    }

    public void setName(String name) {
        this.name = name;
    }



    /*-------------------
     * Debug
     * ------------------
     */

    public static final String[] TYPE_NAME = {
            "NULL",
            "+",
            "-",
            "*",
            "/",
            "=",
            "<>",
            "<",
            "<=",
            ">",
            ">=",
            "(",
            ")",
            ",",
            ";",
            ".",
            ":=",
            "identifier",
            "number",
            "const",
            "var",
            "procedure",
            "odd",
            "if",
            "then",
            "else",
            "while",
            "do",
            "call",
            "begin",
            "end",
            "repeat",
            "until",
            "read",
            "write",
            "bool_equal",
            "eol",
            "eof"
    };

    /**
     * ??Symbol???
     *
     * @return Symbol???
     */
    public String getSymbolTypeName() {
        return TYPE_NAME[getSymbolType()];
    }
}