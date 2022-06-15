package lava6.l6b;

/**
* Memory starts with 256, but you can't use exactly 256 because that means nil.
*/
public class Memory implements Constants {
	public static final int BASE = 256;
	public final static long K64 = 64000L;

	char[] mem;
	//ptr points to the next address to be assigned.  Start with 1
	int ptr = 1;
	//readOnlyMark shows where the read only code ends and where temp memory begins
	int readOnlyMark;

	/**
	* Create a Memory with the given size.  Limited to 65536
	*/
	public Memory(int size) {
		mem = new char[size];
	}

	public char getType(int ref) {
		return mem[ref - BASE];
	}

	//----------------------------------------------
	// Store and retrieve Strings

	/**
	* Get the char array from a String with toCharArray.
	* This creates the new string and returns the reference.
	*/
	public char newString(char[] ca) {
		return storeArray(STRG,ca);
	}

	//in my model, a Class is just a string with a different tag
	public char newClass(char[] ca) {
		return storeArray(CLAS,ca);
	}

	public char[] readString(char r) {
		int p = (int)r - BASE;
		int slen = (int)mem[p+1];
		char[] ca = new char[slen];
		System.arraycopy(mem,p+2,ca,0,slen);
		return ca;
	}

	//same as arrayLength
	public int stringLength(char r) {
		return mem[r - BASE+1];
	}

	//---------------------------------------------
	//create ints and floats
	/**
	* Given an int in fixed format, store it and return the reference.
	* We store the ident INTG and 3 chars, followed by a zero, so 5 chars in all.
	* The length is not stored because it is always 3
	*/
	public char newInt(long iv) {
		int addr = ptr;
		//the size allocated is 2 greater than the length because we save the word "INTG", and add a 0 to the end
		ptr = addr + 5;
		mem[addr] = INTG;
		char[] ca = Number.toCharArray(iv);
		mem[addr+1]=ca[0];
		mem[addr+2]=ca[1];
		mem[addr+3]=ca[2];	//should always be zero, but store it anyway
		return (char)(addr + BASE);
	}

	//same as createInt, except for the FLOT label
	public char newFloat(long fv) {
		int addr = ptr;
		//the size allocated is 2 greater than the length because we save the word "INTG", and add a 0 to the end
		ptr = addr + 5;
		mem[addr] = FLOT;
		char[] ca = Number.toCharArray(fv);
		mem[addr+1]=ca[0];
		mem[addr+2]=ca[1];
		mem[addr+3]=ca[2];
		return (char)(addr + BASE);
	}

	public long readInt(char ref) {
		int p = (int)ref - BASE;
		//see Num.fromCharArray
		return mem[p+1]*K64*K64 + mem[p+2]*K64 + (long)mem[p+3];
	}

	public long readFloat(char ref) {
		return readInt(ref);
	}

	//updates the int to the new value
	//returns false if the ref is invalid
	public boolean updateInt(char iref,long iv) {
		int addr = (int)iref-BASE;
		char name = mem[addr];
		if (name==Constants.INTG) {
			System.out.println("Memory.updateInt changing value of "+(int)iref+" to "+iv);
			char[] ca = Number.toCharArray(iv);
			mem[addr+1]=ca[0];
			mem[addr+2]=ca[1];
			mem[addr+3]=ca[2];
			return true;
		} else {
			return false;
		}
	}

	//--------------------------------------------
	//array
	/**
	* Create a new array of the given type
	*/
	public char newArray(char type,int length) {
		//this is arbitrary
		if (length<0 || length>1023) {throw new IllegalArgumentException("array is too long "+length);}
		int addr = ptr;
		//the actual location will contain the type
		mem[addr]=type;
		//the next location will have the length
		mem[addr+1]=(char)length;
		//this has a trailing zero for spacing
		ptr=ptr+length+3;
		return (char)(addr+BASE);
	}

	/**
	* Store an existing array.  Used for storing strings
	* This is a confusing name because we also have arrayStore
	*/
	public char storeArray(char type,char[] ca) {
		if (ca.length > 1023) throw new IllegalArgumentException("array length is too big: "+ca.length);
		int addr = ptr;
		//the size allocated is 3 greater than the length because we save the type, length and add a 0 to the end
		mem[addr]=type;
		mem[addr+1] = (char)ca.length;
		System.arraycopy(ca,0,mem,addr+2,ca.length);
		ptr = addr + ca.length + 3;
		return (char)(addr + BASE);
	}

	//see stringLength
	public int arrayLength(char aref) {
		return mem[aref-BASE+1];
	}

	public void arrayStore(char aref,int index,char val) {
		mem[aref-BASE+index+2]=val;
	}

	public char arrayLoad(char aref,int index) {
		return mem[aref-BASE+index+2];
	}

	/**
	* Create a new table.  The type is usually CLASS or OBJECT or TABLE but it could be something else.
	* Specify the max number of rows needed.  The table can't be resized.
	*/
	public char newTable(String type,int rows) {
		if (rows<2 || rows>127) {throw new IllegalArgumentException("table is too big "+rows);}
		//increase the size for efficiency.  I add 1 to round up
		rows = (int)(rows * 1.3)+1;
		//make it an odd number
		if ((rows % 2) == 0) {
			rows++;
		}
		//System.out.println("DEBUG: Memory.createTable creating table with "+rows+" rows");
		int addr = ptr;
		char tid = Ident.toIdent(type);
		//System.out.println("debug: Memory.newTable type="+type+", tid = "+(int)tid);
		mem[addr]=tid;
		//the next location will have the rows
		mem[addr+1]=(char)rows;
		//I add 4 because we need 2 slots for the type/rows header, and a blank row at the end
		ptr=ptr+(rows*2)+4;
		//System.out.println("DEBUG: Memory.createTable ptr is now at "+ptr);
		return (char)(addr+BASE);
	}

	public int tableRows(char aref) {
		return mem[aref-BASE+1];
	}

	public void put(char tref,char key,char val) {
		//I call it code but it is the number of rows, always an odd number
		char code = mem[tref-BASE+1];
		//System.out.println("DEBUG: Memory.put, code = "+(int)code);
		int k = (int)key;
		int hash = k % (int)code;
		//System.out.println("DEBUG: Memory.put, hash = "+hash);
		int slot = tref-BASE+(hash*2)+2;
		//System.out.println("DEBUG: Memory.put, slot = "+slot);
		int k2 = mem[slot];	//k2 is the key where we are thinking of putting the value
		boolean looking=true;
		int misses = 0;
		while (looking) {
			if (k2==0) {
				//found an empty slot, use it
				mem[slot]=key;
				mem[slot+1]=val;
				looking=false;
				//System.out.println("DEBUG: Memory.put found an empty slot at "+slot+"; filling it");
			} else if (k2==key) {
				//it already exists, replace the value
				mem[slot+1]=val;
				looking=false;
				//System.out.println("DEBUG: Memory.put found the same slot at "+slot+"; replacing it");
			} else {
				misses++;
				if (misses>2) throw new IllegalStateException("Memory.put: too many slot misses, please increase table size");
				//taken by another slot
				//System.out.println("DEBUG: Memory.put; the slot at "+slot+" is used by "+k2+"; looking further");
				slot = slot + 2;
				if (slot > (tref-BASE+code*2)) {
					slot = slot - (code*2);
					k2 = mem[slot];
					//System.out.println("DEBUG: Memory.put is looking for the next slot at "+slot+"; wrapping");
				} else {
					k2 = mem[slot];
					//System.out.println("DEBUG: Memory.put is looking for the next slot at "+slot);
				}
			}
		}
	}

	/**
	* Retrieve a value from the table.  The value will be NIL (256)
	* if it doesn't exist
	*/
	public char get(char tref,char key) {
		int misses = 0;
		int p = tref - BASE;
		int code = mem[p+1];
		int k0 = key;
		int hash = k0 % code;
		int slot = p+(hash*2)+2;
		//System.out.println("DEBUG: Memory.get, slot = "+slot);
		int k2 = mem[slot];
		boolean looking=true;
		while (looking) {
			if (k2==0) {
				//not found
				looking = false;
				return NIL;
			} else if (k2==key) {
				//found
				looking = false;
				return mem[slot+1];
			} else {
				misses++;
				if (misses>2) throw new IllegalStateException("Memory.get: too many slot misses, please increase table size");
				slot = slot + 2;
				if (slot > code*2) {
					slot = slot - (code*2);
					k2 = mem[slot];
					//System.out.println("DEBUG: Memory.get is looking for the next slot at "+slot+"; wrapping");
				} else {
					k2 = mem[slot];
					//System.out.println("DEBUG: Memory.get is looking for the next slot at "+slot);
				}
			}
		}
		return NIL;
	}

}