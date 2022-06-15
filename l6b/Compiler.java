package lava6.l6b;
import java.io.*;
import org.apache.bcel.classfile.*;
import org.apache.bcel.generic.Type;

/**
* I call this Compiler.  It actually decompiles the Java class file and converts it into the format we need.
* The output is saved in Memory.
*
* The only change we make to the code is to change the lookup names to idents.
* The memory begins with a pool that is similar to the constant pool. It holds references to the methods and fields
* and it also holds the constants.
*/
public class Compiler implements Constants {
	Memory memory;
	String classFileName;
	//this is the table ref of the constant pool
	char tref;

	/**
	* If the class file is in a given directory, pass in the -Ddir property
	*/
	public Compiler(Memory m,String className) {
		memory = m;
		String dir = System.getProperty("dir");
		if (dir!=null) {
			classFileName = dir + canonicalName(className);
		} else {
			classFileName = canonicalName(className);
		}
		if (!classFileName.endsWith(".class")) {
			classFileName = classFileName +".class";
		}
	}

	//replace the periods with slashes
	public static String canonicalName(String className) {
		return className.replace('.', '/');
	}

	public void run() throws ClassFormatException,IOException {
		ClassParser classp = new ClassParser(classFileName);
		JavaClass jclass = classp.parse();

		char ctable=createClassTable(jclass);
		tref=ctable;
		loadConstants(jclass,ctable);
		loadFields(jclass,ctable);
		//load methods
		loadMethods(jclass,ctable);
	}

	public char getClassTableRef() {return tref;}

	private char createClassTable(JavaClass jclass) {
		ConstantPool cpool = jclass.getConstantPool();
		int len = cpool.getLength();
		//the rule of thumb is that we want the cpool length / 2 + 3;
		//we add 1 for rounding, 1 for cname, and 1 for main
		len = (len / 2) + 3;
		//System.out.println("creating class table with "+len+" rows");
		char cref = memory.newTable("CLAS",len);
		return cref;
	}

	/**
	* We only load Strings and Integers and floats here.
	* Also Classes, which are treated like Strings.
	* Constants are started with a key starting in the 9000 range.  This is arbitrary, but makes it easy
	* to make a 4 digit character
	*/
	private void loadConstants(JavaClass jclass,char ctable) {
		ConstantPool cpool = jclass.getConstantPool();
		for (int i=1;i<cpool.getLength();i++) {
			Constant k = cpool.getConstant(i);
			if (k!=null) {
				if (k instanceof ConstantString) {
					ConstantString cs = (ConstantString)k;
					//get the value
					String str = cs.getBytes(cpool);
					char sref = memory.newString(str.toCharArray());
					String key = Integer.toString(9000+i);
					char idk = Ident.toIdent(key);
					//System.out.println("Compiler.loadConstants, loading String into "+(int)idk);
					//System.out.println("storing "+key+" (with id "+idk+") and value "+sref);
					memory.put(ctable,idk,sref);
				} else if (k instanceof ConstantInteger) {
					ConstantInteger ci = (ConstantInteger)k;
					int ival = ci.getBytes();
					long num = Number.toNumber(ival);
					char iref = memory.newInt(num);
					String key = Integer.toString(9000+i);
					char idk = Ident.toIdent(key);
					//System.out.println("Compiler.loadConstants, loading Integer into "+(int)idk);
					memory.put(ctable,idk,iref);
				} else if (k instanceof ConstantFloat) {
					ConstantFloat cf = (ConstantFloat)k;
					float fval = cf.getBytes();
					long num = Number.toNumber(fval);
					char fref = memory.newFloat(num);
					String key = Integer.toString(9000+i);
					char idk = Ident.toIdent(key);
					//System.out.println("Compiler.loadConstants, loading Float into "+(int)idk);
					memory.put(ctable,idk,fref);
				} else if (k instanceof ConstantClass) {
					//treat this almost exactly like a string.  We store it in the constant pool
					//but it loses its type information
					ConstantClass cc = (ConstantClass)k;
					String className = cc.getBytes(cpool);
					if (className.equals("java/lang/StringBuilder")) {
						//ignore, don't put it in the constant pool
						//System.out.println("Compiler.loadConstants, ignoring Class "+className);
					} else {
						char cref = memory.newClass(className.toCharArray());
						String key = Integer.toString(9000+i);
						char idk = Ident.toIdent(key);
						//System.out.println("Compiler.loadConstants, loading Class into "+(int)idk);
						memory.put(ctable,idk,cref);
					}
				}
			}
		}
	}

	/**
	* Only load static fields of this class here.
	* Non-static fields are stored in the object.
	* At this point, we only care about constant Strings and Integers from above
	*/
	private void loadFields(JavaClass jclass,char ctable) {
		Field[] fa = jclass.getFields();
		for (int i=0;i<fa.length;i++) {
			Field f = fa[i];
			if (f.isStatic()) {
				String fname = f.getName();
				char idf = Ident.toIdent(fname);
				ConstantValue cv = f.getConstantValue();
				if (cv == null) {
					memory.put(ctable,idf,NIL);
				} else {
					int cvx = cv.getConstantValueIndex();
					//we should have this
					String key = Integer.toString(9000+i);
					char v = memory.get(ctable,Ident.toIdent(key));
					if (v==NIL) {
						//throw exception?
						System.out.println("warning, setting constant to "+key+" but this does not exist in classpool");
						memory.put(ctable,idf,NIL);
					} else {
						memory.put(ctable,idf,v);
					}
				}
			}
		}
	}

	/**
	* load all methods.  External references must be hard-coded
	*/
	private void loadMethods(JavaClass jclass,char ctable) {
		Method[] ma = jclass.getMethods();
		for (int i=0;i<ma.length;i++) {
			Method m = ma[i];
			String mname = m.getName();
			if (mname.startsWith("<")) {mname = mname.substring(1,mname.length()-1);}
			char idm = Ident.toIdent(mname);
			//String rev = Ident.fromIdent(idm);
			int params = m.getArgumentTypes().length;
			if (params > 3) {
				throw new IllegalStateException("Compiler error: method "+mname+" has "+params+" parameters. I can only handle 3");
			}
			byte[] mcode = m.getCode().getCode();
			//System.out.println("analyzing code for "+mname);
			char[] ccode = translateCode(jclass,idm,params,mcode);

			//store the method in a char array
			char mref = memory.storeArray(METH,ccode);
			//now store this in the class table
			//should we check to see if the methodname already exists?
			//this only way this would happen is if you overload a method name
			//which could happen
			memory.put(ctable,idm,mref);
		}
	}


	/**
	* Translate the java byte code to my format.  The only change is the constant pool lookup
	* The translated code is 2 more than the input because:
	*	it has the method name and it has the number of params
	*/
	private char[] translateCode(JavaClass jclass,char mname,int params,byte[] code) {
		ConstantPool cpool = jclass.getConstantPool();
		String thisName = jclass.getClassName();
		char[] out = new char[code.length+2];
		out[0]=mname;
		out[1]=(char)params;

		char bytecode = (char)0;
		byte indexbyte1 = (byte)0;
		byte indexbyte2 = (byte)0;
		int index=0;
		char key=(char)0;

		for (int i=0;i<code.length;i++) {

			bytecode = (char)(code[i] & 0xFF);
			//System.out.println("analyzing bytecode for "+(int)bytecode);

			//only change the code that uses the constant pool
			//which is:
			//	anewarray
			//	checkcast
			//	getfield
			//	getstatic
			//	instanceof
			//	invokespecial
			//	invokestatic
			//	invokevirtual
			//	ldc
			//	multianewarray - skip this
			//	newobj
			//	putfield
			//	putstatic

			switch(bytecode) {

				case LDC:
					//LDC takes one argument, which is the index
					index = (int)code[i+1];
					key=lookupConstant(cpool,index,thisName);
					out[i+2]=bytecode;
					out[i+3]=key;
					//advance counter by 1
					i=i+1;
					break;
				case ANEWARRAY:
				case CHECKCAST:
				case GETFIELD:
				case GETSTATIC:
				case INSTANCEOF:
				case INVOKESPECIAL:
				case INVOKESTATIC:
				case INVOKEVIRTUAL:
				case NEWOBJ:
				case PUTFIELD:
				case PUTSTATIC:
					indexbyte1 = code[i+1];
					//System.out.println("indexbyte1="+(int)indexbyte1);
					indexbyte2 = code[i+2];
					//System.out.println("indexbyte2="+(int)indexbyte2);
					index = indexbyte1 << 8 | indexbyte2;
					//System.out.println("debug: Compiler.translateCode bytecode="+(int)bytecode+",index="+index);
					key=lookupConstant(cpool,index,thisName);
					out[i+2]=bytecode;
					out[i+3]=key;
					out[i+4]=NOP;	//0
					//advance counter by 2
					i=i+2;
					break;
				default: out[i+2]=bytecode;
			}	//end switch
		} //end for
		return out;
	} //end translate code

	/**
	* We are helping a bytecode that is referring to something in the constant pool.
	* What we do is lookup the constant pool, and then translate it to our numbering system.
	* We return the u16 that has the name, which is either the method or field name, index + 9000,
	* or special name
	*/
	private char lookupConstant(ConstantPool cpool,int index,String thisClassName) {
		Constant k = cpool.getConstant(index);
		char name = (char)0;
		if (k instanceof ConstantClass) {
			//we treat this almost exactly like a string
			ConstantClass cc = (ConstantClass)k;
			String className = cc.getBytes(cpool);
			if (className.equals("java/lang/StringBuilder")) {
				//System.out.println("looking up class "+className+" putting "+(int)CLASS_SB);
				name = CLASS_SB;
			} else {
				String key = Integer.toString(9000+index);
				name = Ident.toIdent(key);
			}
		} else if (k instanceof ConstantFieldref) {
			ConstantFieldref cfr = (ConstantFieldref)k;
			int class_index = cfr.getClassIndex();
			ConstantClass cc2=(ConstantClass)cpool.getConstant(class_index);
			String fcname=cc2.getBytes(cpool);
			int natx = cfr.getNameAndTypeIndex();
			ConstantNameAndType cnat = (ConstantNameAndType)cpool.getConstant(natx);
			String fname = cnat.getName(cpool);
			//so we are looking for a fieldref. If it is the same class, then just lookup by name
			if (fcname.equals(thisClassName)) {
				name = Ident.toIdent(fname);
			} else {
				//special cases
				if (fcname.equals("java/lang/System") && fname.equals("out")) {
					name = Constants.SYSOUT;
				} else {
					//not found - this is bad
					System.out.println("error: Compiler.lookupConstant fieldref, class is "+fcname+" field is "+fname);
				}
			}
		} else if (k instanceof ConstantMethodref) {
			ConstantMethodref cmr = (ConstantMethodref)k;
			int class_index = cmr.getClassIndex();
			ConstantClass cc2=(ConstantClass)cpool.getConstant(class_index);
			String mcname=cc2.getBytes(cpool);
			int natx = cmr.getNameAndTypeIndex();
			ConstantNameAndType cnat = (ConstantNameAndType)cpool.getConstant(natx);
			String mname = cnat.getName(cpool);
			String type = cnat.getSignature(cpool);
			if (mcname.equals(thisClassName)) {
				name = Ident.toIdent(mname);
			} else {
				//special cases
				if (mcname.equals("java/lang/Object") && mname.equals("<init>")) {
					name = OBJINIT;
				} else if (mcname.equals("java/io/PrintStream") && mname.equals("println") && type.equals("(Ljava/lang/String;)V")) {
					name = PRNS;
				} else if (mcname.equals("java/io/PrintStream") && mname.equals("println") && type.equals("(I)V")) {
					name = PRNI;
				} else if (mcname.equals("java/io/PrintStream") && mname.equals("println") && type.equals("(F)V")) {
					name = PRNF;
				} else if (mcname.equals("java/lang/Integer") && mname.equals("parseInt")) {
					name = PARSEINT;
				} else if (mcname.equals("java/lang/StringBuilder") && mname.equals("<init>")) {
					name = SB_INIT;
				} else if (mcname.equals("java/lang/StringBuilder") && mname.equals("append") && type.equals("(Ljava/lang/String;)Ljava/lang/StringBuilder;")) {
					name = SB_APPEND_STR;
				} else if (mcname.equals("java/lang/StringBuilder") && mname.equals("append") && type.equals("(I)Ljava/lang/StringBuilder;")) {
					name = SB_APPEND_I;
				} else if (mcname.equals("java/lang/StringBuilder") && mname.equals("toString")) {
					name = SB_TOSTR;
				} else {
					System.out.println("error: Compiler.lookupConstant methodref, class is "+mcname+" method is "+mname+"; type is "+type);
				}
			}
		} else if (k instanceof ConstantString) {
			//this is easy, just lookup the k value
			String key = Integer.toString(9000+index);
			name = Ident.toIdent(key);
		} else if (k instanceof ConstantInteger) {
			String key = Integer.toString(9000+index);
			name = Ident.toIdent(key);
		} else if (k instanceof ConstantFloat) {
			String key = Integer.toString(9000+index);
			name = Ident.toIdent(key);
		} else {
			String c = k.getClass().getName();
			//this is certainly unexpected
			System.out.println("error: in Compiler.lookupConstant, Constant is type "+c);
		}
		return name;
	}

	public static void main(String[] args) throws IOException {
		String testClassName=args[0];
		Memory m = new Memory(1024);
		Compiler c = new Compiler(m,testClassName);
		c.run();
	}
}