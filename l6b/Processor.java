package lava6.l6b;

/**
* The Processor runs the program, which is stored in memory.
*/
public class Processor implements Constants {
	Memory memory;
	char tref;	//reference of the constant pool in memory
	//level is the depth of the method call, at this point limited to 8, so it will have a value of 0..7
	int level = 0;
	//stackPointer points to the next spot to be filled.  This will have a value of 0..7
	int stackPointer = 0;

	//the stack is a long, even though most of the things in it will be chars,
	//because we want to handle fixed numbers
	long[] stack = new long[8];

	//register bank
	//each level has 2 chars holding:
	//	method name (ident) for debugging purposes
	//	method address
	char[] rbank = new char[16];

	//	ip - instruction pointer
	//this is an int so we don't have to keep converting it
	int[] iptr = new int[8];

	//we allow 4 local variables per method, up to 8 deep.
	long[] lvar = new long[32];

	//for use with StringBuilder
	char[] stringBuilder = new char[256];
	int sbPointer = 0;

	boolean running=false;
	//-----------------------------------------
	public Processor(Memory m,char tref) {
		memory = m;
		this.tref=tref;
	}

	public void log(String s) {
		System.out.println(s);
	}

	//--------------------------------------------------------
	//methods which interact with the registers
	/**
	* The level will be from 0..7.  This is the nesting level of the function call.
	*/
	public int getLevel() {return level;}

	//clear the current level.  used only by return
	//does not decrement the level
	public void clearLevel() {
		rbank[level*2]=(char)0;
		rbank[level*2+1]=(char)0;
		iptr[level]=0;
		lvar[level*4]=0L;
		lvar[level*4+1]=0L;
		lvar[level*4+2]=0L;
		lvar[level*4+3]=0L;
	}

	/**
	* This is the method name, as an ident.  Decipher it with Ident.fromIdent();
	* This is in register 0
	*/
	public char getMethodName() {
		return rbank[level*2];
	}

	/**
	* get the memory address of the method
	*/
	public char getAddress() {
		return rbank[level*2+1];
	}


	/**
	* Return the instruction pointer, which is relative to the start of the address.
	*/
	public int IP() {
		return iptr[level];
	}

	/**
	* Jump in the code.  This could be a negative.
	* This adjusts it by 3 - test it.
	*/
	public void JUMP(boolean b,short rel) {
		if (b) iptr[level]=iptr[level]+rel-3;
	}

	/**
	* Read instruction and increment IP
	*/
	public char NEXT() {
		return memory.arrayLoad(getAddress(),iptr[level]++);
	}

	/**
	* Store a local variable.  Var will be a number from 0..3 corresponding to the local variable number.
	*/
	public void localStore(int var,long val) {
		if (var<0 || var>3) throw new IllegalArgumentException("var must be in the range 0..3");
		lvar[level*4+var]=val;
	}

	/**
	* Var is a number from 0..3
	*/
	public long localLoad(int var) {
		if (var<0 || var>3) throw new IllegalArgumentException("var must be in the range 0..3");
		return lvar[level*4+var];
	}

	public void PUSH(long lv) {
		//log("PUSH "+lv+" on the stack");
		if (stackPointer<7) {
			stack[stackPointer]=lv;
			stackPointer++;
			//log("stackPointer is now "+stackPointer);
		} else {
			//System.out.println("WARNING: stack is full - the capacity is 8");
		}
	}

	public long POP() {
		//log("POP requested, stackPointer is now at "+stackPointer);
		if (stackPointer>0) {
			long v = stack[stackPointer-1];
			stackPointer--;
			return v;
		} else {
			throw new IllegalStateException("POP: stack is empty");
			//System.out.println("WARNING: stack is empty");
			//return 0;
		}
	}

	public long PEEK() {
		if (stackPointer>0) {
			return stack[stackPointer-1];
		} else {
			//System.out.println("WARNING: stack is empty");
			return 0;
		}
	}
	//-------------------------------------
	//call the subroutine
	public void CALL(char mname) {
		log ("CALL "+Ident.fromIdent(mname,0));
		char mref = memory.get(tref,mname);
		if (mref==NIL) {
			System.out.println("ERROR: CALL "+Ident.fromIdent(mname,0)+"("+(int)mname+") not found");
		} else {
			//set up the new frame
			rbank[(level+1)*2]=mname;
			rbank[(level+1)*2+1]=mref;

			//just for fun get the method size
			int mlen = memory.arrayLength(mref);
			//get the number of params
			int params = memory.arrayLoad(mref,1);
			log("CALL params = "+params);
			//push params to local variables
			for (int i=0;i<params;i++) {
				long data = POP();
				lvar[(level+1)*4+i]=data;
			}
			//set instruction pointer. It starts on 2
			iptr[level+1]=2;
			//increment the level
			level++;
		}
	}

	//--------------------------------------------------------
	public void start(String[] args) {
		//what about the class initializer? do that later
		//note that our level is 0, and Main will be on the 1 level

		//fill in the parms
		//create a String array
		char sary = NIL;
		if (args!=null && args.length>0) {
			sary = memory.newArray(Ident.toIdent("SARY"),args.length);
			for (int i=0;i<args.length;i++) {
				char sref=memory.newString(args[i].toCharArray());
				//log("storing string to "+(int)sref);
				memory.arrayStore(sary,i,sref);
			}
		}
		//this seems superfluous, but store it on the stack
		PUSH((long)sary);

		//set up the frame
		CALL(MAIN);
		//now run it
		running=true;
		run();
	}

	public void run() {
		char op = (char)0;
		char arg = (char)0;
		char arg2 = (char)0;
		int temp = 0;
		char type;
		char val = (char)0;
		long a = 0L;
		long b = 0L;

		while (running) {
			//fetch the next byte
			op = NEXT();
			switch(op) {
				//load numbers on to stack
				case BIPUSH:
					//push a byte onto the stack as an integer value
					temp=(int)NEXT();
					if (temp>127 && temp<256) { temp=temp-256;}
					PUSH(Num.toNumber(temp));
					break;
				case SIPUSH:
					//push a short onto the stack as an integer value
					arg=NEXT(); arg2=NEXT();
					temp = (int)arg*256 + (int)arg2;	//this is in 2s complement format
					if (temp>32767 && temp<65536) {temp=temp-65536;}
					PUSH(Num.toNumber(temp));
					break;
				case LDC:
					//push a constant #index from a constant pool onto the stack
					arg=NEXT();
					arg2=memory.get(tref,arg);
					//lookup the type
					type=memory.getType(arg2);
					if (type==INTG || type==FLOT) {
						//convert to a num
						PUSH(memory.readInt(arg2));
					} else {
						//just push the ref
						PUSH(arg2);
					}
					break;
				case ICONST_M1: PUSH(Num.toNumber(-1)); break;
				case ICONST_0: PUSH(Num.toNumber(0)); break;
				case ICONST_1: PUSH(Num.toNumber(1)); break;
				case ICONST_2: PUSH(Num.toNumber(2)); break;
				case ICONST_3: PUSH(Num.toNumber(3)); break;
				case ICONST_4: PUSH(Num.toNumber(4)); break;
				case ICONST_5: PUSH(Num.toNumber(5)); break;

				//stack commands
				case DUP:
					//duplicate the value on top of the stack
					PUSH(PEEK());
					break;

				//minimal set
				case ALOAD_0:
					//load a reference onto the stack from local variable 0
					PUSH(localLoad(0));
					break;
				case INVOKESPECIAL:
					//invoke instance method on object objectref and puts the result on the stack (might be void); the method is identified by
					//method reference index in constant pool (indexbyte1 << 8 | indexbyte2)
					//the compile already decoded this
					arg=NEXT();arg2=NEXT();
					if (arg==OBJINIT || arg==SB_INIT) {
						CALL_NATIVE(arg);
					} else {
						log("ERROR: in Processor.run INVOKESPECIAL: unknown arg "+(int)arg);
					}
					break;
				//case INVOKESTATIC:
				//	arg=NEXT(); arg2=NEXT();

				case INVOKEVIRTUAL:
					arg=NEXT();arg2=NEXT();	//arg2 is not used
					switch (arg) {
						case PRNS:
						case PRNI:
						case PRNF:
						case SB_APPEND_STR:
						case SB_APPEND_I:
						case SB_TOSTR:
							CALL_NATIVE(arg);
							break;
						default:
							//more work to do
							log("ERROR in Processor.run INVOKEVIRTUAL: unknown arg  "+(int)arg);
					}
					break;
				case INVOKESTATIC:
					arg=NEXT();arg2=NEXT();	//arg2 is not used
					if (arg==PARSEINT) {
						CALL_NATIVE(arg);
					} else {
						//see if it is a method in our class
						arg2=memory.get(tref,arg);
						if (arg2!=NIL) {
							CALL(arg);
						} else {
							//more work to do
							log("ERROR in Processor.run INVOKESTATIC: unknown arg  "+(int)arg);
						}
					}
					break;
				case GETSTATIC:
					//this is similar to LDC except that takes 1 arg
					//get a static field value of a class,
					arg=NEXT();arg2=NEXT();
					if (arg==SYSOUT) {
						PUSH(SYSOUT);
					} else {
						//the arg is the key. the value is the ref. if int put that on stack
						arg2=memory.get(tref,arg);	//overloading arg2 here
						type=memory.getType(arg2);
						if (type==INTG || type==FLOT) {
							//convert to a num
							PUSH(memory.readInt(arg2));
						} else {
							//just push the ref
							PUSH(arg2);
						}
					}
					break;
				case RETURNV:
					//this is amazingly easy to do. The return value is already on the stack
					log("RETURNV returning from "+Ident.fromIdent(getMethodName(),0));
					clearLevel();
					level--;
					if (level==0) {
						running=false;
						log("program complete");
					}
					//log("after returning, stackPointer is at "+stackPointer);
					break;
				case IRETURN:
					//same as RETURNV except for the program complete line
					log("IRETURN returning from "+Ident.fromIdent(getMethodName(),0));
					clearLevel();
					level--;
					//log("after returning, stackPointer is at "+stackPointer);
					break;
				case NEWOBJ:
					//create new object of type identified by class reference in constant pool index
					arg=NEXT();arg2=NEXT();
					if (arg==CLASS_SB) PUSH(SB_OBJ);
					else {
						//System.out.println("NEWOBJ, class = "+(int)arg+" ( "+Ident.fromIdent(arg,0)+" ) ");
						log("ERROR: Processor NEWOBJ not implemented");
						PUSH(NIL);
					}
					break;
				case ILOAD_0:
					PUSH(localLoad(0));
					break;
				case ILOAD_1:
					PUSH(localLoad(1));
					break;
				case ILOAD_2:
					PUSH(localLoad(2));
					break;
				case ILOAD_3:
					log("ERROR: Processor ILOAD_3 not implemented");
					break;
				case ISTORE_0:
					localStore(0,POP());
					break;
				case ISTORE_1:
					//store int value into variable 1
					localStore(1,POP());
					break;
				case ISTORE_2:
					localStore(2,POP());
					break;
				case ISTORE_3:
					log("ERROR: Processor ISTORE_3 not implemented");
					break;
				case AALOAD:
					temp = (int)POP();	//index of array
					arg2=(char)POP();	//ref of array
					val = memory.arrayLoad(arg2,temp);
					PUSH((long)val);
					break;
				case JMP: arg=NEXT(); arg2=NEXT(); JUMP(true, shortIndex(arg,arg2) ); break;
				case IF_ICMPEQ: arg=NEXT(); arg2=NEXT(); b=POP(); a=POP();
					JUMP(ICMP(a,Compare.EQ,b),shortIndex(arg,arg2)); break;
				case IF_ICMPGE: arg=NEXT(); arg2=NEXT(); b=POP(); a=POP();
					JUMP(ICMP(a,Compare.GTE,b),shortIndex(arg,arg2)); break;
				case IF_ICMPGT: arg=NEXT(); arg2=NEXT(); b=POP(); a=POP();
					JUMP(ICMP(a,Compare.GT,b),shortIndex(arg,arg2)); break;
				case IF_ICMPLE: arg=NEXT(); arg2=NEXT(); b=POP(); a=POP();
					JUMP(ICMP(a,Compare.LTE,b),shortIndex(arg,arg2)); break;
				case IF_ICMPLT: arg=NEXT(); arg2=NEXT();  b=POP(); a=POP();
					JUMP(ICMP(a,Compare.LT,b),shortIndex(arg,arg2)); break;
				case IF_ICMPNE: arg=NEXT(); arg2=NEXT();  b=POP(); a=POP();
					JUMP(ICMP(a,Compare.NEQ,b),shortIndex(arg,arg2)); break;
				case IFEQ: arg=NEXT(); arg2=NEXT(); a=POP(); JUMP(ICMP(a,Compare.EQ,0),shortIndex(arg,arg2)); break;
				case IFGE: arg=NEXT(); arg2=NEXT(); a=POP(); JUMP(ICMP(a,Compare.GTE,0),shortIndex(arg,arg2)); break;
				case IFGT: arg=NEXT(); arg2=NEXT(); a=POP(); JUMP(ICMP(a,Compare.GT,0),shortIndex(arg,arg2)); break;
				case IFLE: arg=NEXT(); arg2=NEXT(); a=POP(); JUMP(ICMP(a,Compare.LTE,0),shortIndex(arg,arg2)); break;
				case IFLT: arg=NEXT(); arg2=NEXT(); a=POP(); JUMP(ICMP(a,Compare.LT,0),shortIndex(arg,arg2)); break;
				case IFNE: arg=NEXT(); arg2=NEXT(); a=POP(); JUMP(ICMP(a,Compare.NEQ,0),shortIndex(arg,arg2)); break;
				case IFNULL: arg=NEXT(); arg2=NEXT(); a=POP(); JUMP(IFNULL((char)a),shortIndex(arg,arg2)); break;

				case IADD: b=POP(); a=POP(); PUSH(Num.ADD(a,b)); break;
				case ISUB: b=POP(); a=POP(); PUSH(Num.SUB(a,b)); break;
				default:
					log("unknown op "+op+" (0x"+Integer.toHexString((int)op)+")");
			} //end switch
		} //end while
	} //end run

	/**
	* So here is the deal - we have 2 chars, but they are really encoding unsigned byte values.
	* I expect the range to be -128..127
	* If the result is positive, then the first char will be 0 and the 2nd will be 0..127
	* If the result is negative, then the second char will be 255 counting down to 128, and the first char will be 255
	* So -1 will be 255,255
	*/
	public static short shortIndex(char b1,char b2) {
		return (short)(b1 * 256 + b2);
	}

	//--------------------------------------------
	//This invokes the builtin commands.  Some of these put return values on the stack and some don't
	public void CALL_NATIVE(char mref) {
		char oref=(char)0;	//object ref
		char sref=(char)0;	//string ref
		char[] str=null;
		long num=0L;
		int iv=0;
		float fv=0.0F;
		String s=null;

		switch (mref) {
			case OBJINIT:
				//there is nothing profound to do but we need to simulate this
				oref=(char)POP();	//pop the object off the stack
				//get the class of the object.  At this point I don't care - done
				break;
			case PRNS:
				//call the println:(Ljava/lang/String;)V method on the object System.out
				//which is a java/io/PrintStream
				sref = (char)POP();
				oref = (char)POP();
				str = memory.readString(sref);
				if (oref==SYSOUT) {
					//this actually calls the println(char[] x)
					System.out.println(str);
				} else {
					log("ERROR: CALL_NATIVE trying to do println on object "+(int)oref);
				}
				break;
			case PRNI:
				//call println:(I)V
				num=POP();
				iv=Num.toInt(num);
				oref = (char)POP();
				if (oref==SYSOUT) {
					System.out.println(iv);
				} else {
					log("ERROR: CALL_NATIVE trying to do println on object "+(int)oref);
				}
				break;
			case PRNF:
				//call println:(F)V
				num=POP();
				fv=Num.toFloat(num);
				oref = (char)POP();
				if (oref==SYSOUT) {
					System.out.println(fv);
				} else {
					log("ERROR: CALL_NATIVE trying to do println on object "+(int)oref);
				}
				break;
			case PARSEINT:
				//parse a string into an int
				sref = (char)POP();
				//log("PARSEINT sref = "+(int)sref);
				str = memory.readString(sref);
				iv = Integer.parseInt(String.valueOf(str));
				PUSH(Num.toNumber(iv));
				break;
			case SB_INIT:
				oref=(char)POP();
				//oref should be SB_OBJ but no need to check
				//clear stringbuilder
				for (int i=0;i<sbPointer;i++) {
					stringBuilder[i]=(char)0;
				}
				//reset SB pointer
				sbPointer = 0;
				break;
			case SB_APPEND_STR:
				sref = (char)POP();
				str = memory.readString(sref);
				oref= (char)POP();
				System.arraycopy(str,0,stringBuilder,sbPointer,str.length);
				sbPointer = sbPointer + str.length;
				PUSH((long)oref);
				break;
			case SB_APPEND_I:
				num=POP();
				oref= (char)POP();
				//convert num to String
				iv=Num.toInt(num);
				s=String.valueOf(iv);
				str=s.toCharArray();
				System.arraycopy(str,0,stringBuilder,sbPointer,str.length);
				sbPointer = sbPointer + str.length;
				PUSH((long)oref);
				break;
			case SB_TOSTR:
				oref= (char)POP();
				str=new char[sbPointer];
				System.arraycopy(stringBuilder,0,str,0,sbPointer);
				//create a new String
				sref=memory.newString(str);
				PUSH((long)sref);
				break;
			default:
				log("ERROR: in CALL_NATIVE unknown arg "+(int)mref);
		}	//end switch
	}

	//========================================
	public static enum Compare {
		NIL,
		GT,		//1 >
		EQ,		//2 ==
		GTE,	//3 >=
		LT,		//4 <
		NEQ,	//5 != <>
		LTE		//6 <=

		//call ordinal() to get the numeric value
	}


	/**
	* input 2 numbers and a compare code
	* return true if matches, else false
	* If comparing to 0, set b to 0
	*/
	public static boolean ICMP(long a, Compare cmp,long b) {
			//System.out.println("ICMP: a="+a+", code="+code+", b="+b);
			int code = cmp.ordinal();
			long c = a - b;
			//t will either be 0,1,2 or 4
			int t = 0;
			if (c>0) t=1;
			else if (c==0) t=2;
			//c must be less than 0, but no need to check
			else t=4;
			int d = t & code;
			return d > 0;
	}

	/**
	* Given a ref, jump if null.
	* NIL is defined internally as 256
	*/
	public static boolean IFNULL(char ref) {
		return ref==NIL;
	}
}