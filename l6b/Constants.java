package lava6.l6b;

//codes in the range 0..255 must match exactly the Java equivalent
public interface Constants {
	public static final char NOP = (char)0;
	public static final char NIL = (char)256;

	public static final char CLAS = (char)50344;	//for class
	public static final char CLIN = (char)50229;	//for class init
	public static final char CNAM = (char)50597;	//for class name, a string
	public static final char FLOT = (char)0xF46D;	//62573 - for floats
	public static final char INIT = (char)13629;
	public static final char INTG = (char)13777;	//for integers
	public static final char MAIN = (char)23093;
	public static final char METH = (char)24274;	//for method
	public static final char STRG = (char)36209;	//for Strings

	//other built-in objects
	//these will be changed in future versions
	//java/lang/Object."<init>":()V
	public static final char OBJINIT = (char)0xFE01;
	//java/lang/System field is out.  This is an object of class PrintStream
	public static final char SYSOUT = (char)0xFE02;
	//java/io/PrintStream method is println; type is (Ljava/lang/String;)V
	public static final char PRNS = (char)0xFE03;
	//java/io/PrintStream method println, type (I)V
	public final static char PRNI = (char)0xFE04;
	//java/io/PrintStream method println, type (F)V
	public final static char PRNF = (char)0xFE05;
	//java/lang/Integer static method is parseInt; type is (Ljava/lang/String;)I
	public final static char PARSEINT = (char)0xFE06;
	//java/lang/StringBuilder method is <init>; type is ()V
	//there is also an initializer that accepts a string, but I don't handle that
	public final static char SB_INIT = (char)0xFE07;
	//java/lang/StringBuilder method is append; type is (Ljava/lang/String;)Ljava/lang/StringBuilder;
	public final static char SB_APPEND_STR = (char)0xFE08;
	//java/lang/StringBuilder method is append; type is (I)Ljava/lang/StringBuilder;
	public final static char SB_APPEND_I = (char)0xFE09;
	//java/lang/StringBuilder method is toString; type is ()Ljava/lang/String;
	public final static char SB_TOSTR = (char)0xFE0A;
	//this symbol represents the class java/lang/StringBuilder which is handled specially
	public final static char CLASS_SB = (char)0xFE0B;
	public final static char SB_OBJ = (char)0xFE0C;


	//to do
	//public final static String PARSEINT="java/lang/Integer.parseInt:(Ljava/lang/String;)I";

	//these lookup the constant pool
	public static final char ANEWARRAY = (char)0x00BD;
	public static final char CHECKCAST = (char)0x00C0;
	public static final char GETFIELD = (char)0x00B4;
	public static final char GETSTATIC = (char)0x00B2;
	public static final char INSTANCEOF = (char)0x00C1;
	public static final char INVOKEVIRTUAL = (char)0x00B6;
	public static final char INVOKESPECIAL = (char)0x00B7;
	public static final char INVOKESTATIC = (char)0x00B8;

	public static final char LDC = (char)0x0012;
	public static final char NEWOBJ = (char)0x00BB;
	public static final char PUTFIELD = (char)0x00B5;
	public static final char PUTSTATIC = (char)0x00B3;

	//return from subroutine
	public final static char RETURNV = (char)0x00B1;		//177 aka RETURN
	public final static char IRETURN = (char)0x00AC;		//172 return an int from method
	public final static char ARETURN = (char)0x00B0;		//return object from a method

	//now the regular byte code
	public final static char BIPUSH = (char)0x0010; 		//decimal 16
	public final static char SIPUSH = (char)0x0011;			//17
	public final static char ICONST_M1 = (char)0x0002;
	public final static char ICONST_0 = (char)0x0003;
	public final static char ICONST_1 = (char)0x0004;
	public final static char ICONST_2 = (char)0x0005;
	public final static char ICONST_3 = (char)0x0006;
	public final static char ICONST_4 = (char)0x0007;
	public final static char ICONST_5 = (char)0x0008;

	public final static char DUP = (char)0x0059;
	public final static char POP = 		(char)0x0057;			//87

	//these complete the minimal set
	public final static char ALOAD_0 = (char)0x002A;		//42
	public final static char AALOAD = (char)0x0032;
	public final static char ILOAD = 	(char)0x0015;		//26
	public final static char ILOAD_0 = (char)0x001A;		//26
	public final static char ILOAD_1 = (char)0x001B;		//27
	public final static char ILOAD_2 = (char)0x001C;		//28
	public final static char ILOAD_3 = (char)0x001D;		//29
	public final static char ISTORE_0 = (char)0x003B;		//59
	public final static char ISTORE_1 = (char)0x003C;		//60
	public final static char ISTORE_2 = (char)0x003D;		//61
	public final static char ISTORE_3 = (char)0x003E;		//62

	public final static char JMP = (char)0x00A7;			//167 same as GOTO
	public final static char IF_ACMPEQ = (char)0x00A5;
	public final static char IF_ICMPEQ = (char)0x009F;	//159
	public final static char IF_ICMPGE = (char)0x00A2; 	//162
	public final static char IF_ICMPGT = (char)0x00A3; 	//163
	public final static char IF_ICMPLE = (char)0x00A4; 	//164
	public final static char IF_ICMPLT = (char)0x00A1; 	//165
	public final static char IF_ICMPNE = (char)0x00A0; 	//160
	public final static char IFEQ = (char)0x0099;			//153
	public final static char IFGE = (char)0x009C;			//156
	public final static char IFGT = (char)0x009D;			//157
	public final static char IFLE = (char)0x009E;			//158
	public final static char IFLT = (char)0x009B;			//155
	public final static char IFNE = (char)0x009A;			//154
	public final static char IFNONNULL = (char)0x00C7;
	public final static char IFNULL = (char)0x00C6;

	public final static char IADD = (byte)0x0060;			//96
	public final static char ISUB = (byte)0x0064;			//100
}