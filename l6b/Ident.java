package lava6.l6b;

/**
* An Ident is an internal name for a class, field or method.
* It is 3 to 4 characters in base16.
* It will always be in the range 256..65407(0x100..0xFF7F).
* Here is the encoding table:
*	0 = 0,_,SPACE
*	1 = 1,G,J
*	2 = 2,H,X
*	3 = 3,I,Y
*	4 = 4,L
*	5 = 5,M,N
*	6 = 6,O
*	7 = 7,R
*	8 = 8,S,Z
*	9 = 9,U,W
*	10 = A
*	11 = B,P
*	12 = C,K,Q
*	13 = D,T
*	14 = E
*	15 = F,V
*/
public class Ident {

	/** Input, an int in the range 0..65535.
	* Output: the hex string with a leading 0x
	*/
    public static String hex(int w) {
		if (w < -128 || w > 65535) throw new IllegalArgumentException(w+" is out of range");
		if (w < 0) { w = w + 65536;}
		//this means, add a leading 0x, make it 4 digits long and pad it with zeros, and format it as hex
		return String.format("0x%04x",w);
	}

	/**
	* Given the ascii byte in the range 32..122, return the code, which is a number from 0..15
	*/
	public static int encode(byte b) {
		int d=(int)b;
		//the minimum is space (32) and the maximum is little z (122)
		if (d<32 || d>122)  {throw new IllegalArgumentException(d+" is out of range");}
		switch (d) {
			case 32: case 48: case 95: // space,0,underscore
				return 0;
			case 49: case 71: case 74: case 103: case 106:	//1 = 1,G,J
				return 1;
			case 50: case 72: case 88: case 104: case 120:	//2 = 2,H,X
				return 2;
			case 51: case 73: case 89: case 105: case 121:	//3 = 3,I,Y
				return 3;
			case 52: case 76: case 108:	//4 = 4,L
				return 4;
			case 53: case 77: case 78: case 109: case 110:	//5 = 5,M,N
				return 5;
			case 54: case 79: case 111:	//6 = 6,O
				return 6;
			case 55: case 82: case 114:	//7 = 7,R
				return 7;
			case 56: case 83: case 90: case 115: case 122:	//8 = 8,S,Z
				return 8;
			case 57: case 85: case 87: case 117: case 119:	//9 = 9,U,W
				return 9;
			case 65: case 97:	//10 = A
				return 10;
			case 66: case 80: case 98: case 112:	//11 = B,P
				return 11;
			case 67: case 75: case 81: case 99: case 107: case 113:	//12 = C,K,Q
				return 12;
			case 68: case 84: case 100: case 116:	//13 = D,T
				return 13;
			case 69: case 101:	//14 = E
				return 14;
			case 70: case 86: case 102: case 118:
				return 15;
			default:
				return 0;
		}
	}

	/**
	* Return the main encoded value
	*/
	public static byte decode(int i) {
		if (i<0 || i>15) {throw new IllegalArgumentException(i+" is out of range");}
		switch (i) {
			case 1: return (byte)'G';
			case 2: return (byte)'H';
			case 3: return (byte)'I';
			case 4: return (byte)'L';
			case 5: return (byte)'N';
			case 6: return (byte)'O';
			case 7: return (byte)'R';
			case 8: return (byte)'S';
			case 9: return (byte)'U';
			case 10: return (byte)'A';
			case 11: return (byte)'B';
			case 12: return (byte)'C';
			case 13: return (byte)'D';
			case 14: return (byte)'E';
			case 15: return (byte)'F';
			case 0:
			default:
				return (byte)'_';
		}
	}

	//alternative values
	public static byte altDecode(int i) {
		if (i<0 || i>15) {throw new IllegalArgumentException(i+" is out of range");}
		switch (i) {
			case 1: return (byte)'J';
			case 2: return (byte)'X';
			case 3: return (byte)'Y';
			case 4: return (byte)'L';
			case 5: return (byte)'M';
			case 6: return (byte)'O';	//the same
			case 7: return (byte)'R';
			case 8: return (byte)'Z';
			case 9: return (byte)'W';
			case 10: return (byte)'A';	//the same
			case 11: return (byte)'P';
			case 12: return (byte)'K';
			case 13: return (byte)'T';
			case 14: return (byte)'E';	//the same
			case 15: return (byte)'V';
			case 0:
			default:
				return (byte)'_';
		}
	}

	/**
	* Given a String, return its IDENT value which will be in the range 257..65407
	*/
	public static char toIdent(String s) {
		s = s.toUpperCase();
		if (s.length()>4) {
			s = s.substring(0,4);
		}
		if (s.length() == 1) {
			s = "0" + s + "00";
		} else if (s.length() == 2) {
			s = s + "0" + s + "0";
		} else if (s.length()==3) {
			s = s + "0" + s;
		}
		byte[] bb=s.getBytes();
		int iv = 0;
		int a = 0;
		for (int i=0;i<4;i++) {
			a=encode(bb[i]);
			iv = iv + a * (int)Math.pow(16,3-i);
		}
		return (char)iv;
	}

	//turn this into a string using the number and set.  The default set is 0, the alt set is 1
	//note that the first ident is 257 because 256 is nil
	public static String fromIdent(char c,int set) {
		int w = (int)c;
		if (w < 257 || w > 65407) throw new IllegalArgumentException(w+" is out of range");
		byte[] ba = new byte[4];
		for (int i=0;i<4;i++) {
			int pow = (int)Math.pow(16,3-i);
			int a = w / pow;
			int x = a * pow;
			w = w - x;
			if (set==0) {
				ba[i]= decode(a);
			} else {
				ba[i]= altDecode(a);
			}
		}
		return new String(ba);
	}

	//==============
	public static void main(String[] args) {
		//int i = Integer.parseInt(args[0]);
		//System.out.println(hex(i));
		char id = toIdent(args[0]);
		System.out.println((int)id);
		String s2 = fromIdent(id,0);
		String s3 = fromIdent(id,1);
		System.out.println(s2 + "("+s3+")");
	}

}