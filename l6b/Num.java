package lava6.l6b;

/**
* This is a 48-bit number that can handle either ints or floats.
* This can handle integers in the range: -2,047,999,999 .. 2,047,999,999
* This can handle floats in the range -2,047,999,967.0 .. 2,047,999,967.99998
*
* I am doing it like this because:
*	1) I dislike floating point numbers
*	2) I want an exact representation of decimal fractions
*	3) I want to simplify this by using the same storage format for ints and floats
*
* This uses a long for internal storage.
* I can't call this Number because that conflicts with java.lang.Number
*/

public class Num {
	public final static long K64 = 64000L;
	public final static int MAX_INT = (32000*64000)-1; //2,047,999,999
	public final static long FULL48= 64000L * 64000L * 64000L ;
	public final static long NEG_POINT = 32000L * 64000L * 64000L;
	//MAX_FLOAT is slightly small than MAX_INT because of how it represents values
	public final static float MAX_FLOAT = (float)MAX_INT - 32.0F;
	public final static float K64F = 64000.0F;

	public static long toNumber(int i) {
		if (i> MAX_INT || i < (0 - MAX_INT)) {
			throw new IllegalArgumentException(i+ " is out of bounds");
		}
		if (i<0) {
			i = 0 - i;
			return FULL48 - (i * K64);
		} else {
			return (long)(i * K64);
		}
	}

	public static long toNumber(float f) {
		if (f> MAX_FLOAT || f < (0 - MAX_FLOAT)) {
			throw new IllegalArgumentException(f+ " is out of bounds");
		}
		if (f < 0.0F) {
			f = 0.0F - f;
			return FULL48 - (long)(f * K64);
		} else {
			return (long)(f * K64);
		}
	}

	public static int toInt(long value) {
		if (value >= NEG_POINT) {
			value = value - FULL48;
		}
		return (int)(value / K64);
	}

	public static float toFloat(long value) {
		//System.out.println("toFloat value passed in = "+value);
		if (value >= NEG_POINT) {
			value = value - FULL48;
		}
		return (float)( (double)value / K64F);
	}

	public static long ADD(long a, long b) {
		long c = a + b;
		if (c >= FULL48) {
			c = c - FULL48;
		}
		return c;
	}

	public static long SUB(long a, long b) {
		long c = a - b;
		if (c < 0) {
			c = c + FULL48;
		}
		return c;
	}

	//Negate
	public static long NEG(long a) {
		if (a >= NEG_POINT ) {
			//its a negative number, change to positive
			a = 0 - (a - FULL48);
		} else {
			//its a postive number, change to negative
			a = (0 - a) + FULL48;
		}
		return a;
	}

	public static long MUL(long a, long b) {
		//step 1 - fix the signs
		boolean diff = false;
		if (a >= NEG_POINT) {
			a = 0 - (a - FULL48);
			diff = true;
		}
		if (b >= NEG_POINT) {
			b = 0 - (b - FULL48);
			if (diff==false) {
				diff = true;
			} else {
				diff = false;
			}
		}

		//step 2 - do the multiplication
		long c = a * b / K64;

		//step 3 - fix the signs again
		if (diff) {
			c = (0 - c) + FULL48;
		}
		return c;
	}

	/**
	* DIV - Divide a by b
	* If you divide by 0, this only gives a warning and returns zero.
	*
	* Note that this does the equivalent of floating point division.
	*/
	public static long DIV(long a, long b) {
		if (b==0L) {
			System.out.println("ERROR: in DIV trying to divide by zero");
			return 0L;
		}
		long result = 0L;
		//step 1 - look at the signs
		//diff is true if and only if the inputs have different signs
		//usually this is when the dividend is negative
		boolean diff = false;
		if (a >= NEG_POINT) {
			a = 0 - (a - FULL48);
			diff = true;
		}
		if (b >= NEG_POINT) {
			b = 0 - (b - FULL48);
			if (diff==false) {
				diff = true;
			} else {
				diff = false;
			}
		}

		//step 2 - do the integer division
		long c = a / b;

		//step 3 - also do more work on the remainder
		long d = a % b;
		long e = (d * K64) / b;

		//step 4 - combine them
		result = c * K64 + e;

		//step 5 - fix the signs again
		if (diff) {
			result = (0 - result) + FULL48;
		}
		return result;
	}

	/**
	* IDIV - Divide a by b
	* If you divide by 0, this only gives a warning and returns zero.
	*
	* Note that this does the equivalent of int division.
	*/
	public static long IDIV(long a, long b) {
		if (b==0L) {
			System.out.println("ERROR: in DIV trying to divide by zero");
			return 0L;
		}
		long result = 0L;

		//step 1 - look at the signs
		//diff is true if and only if the inputs have different signs
		//usually this is when the dividend is negative
		boolean diff = false;
		if (a >= NEG_POINT) {
			a = 0 - (a - FULL48);
			diff = true;
		}
		if (b >= NEG_POINT) {
			b = 0 - (b - FULL48);
			if (diff==false) {
				diff = true;
			} else {
				diff = false;
			}
		}

		//step 2 - do the integer division
		long c = a / b;

		//step 4 - convert it
		result = c * K64;

		//step 5 - fix the signs again
		if (diff) {
			result = (0 - result) + FULL48;
		}
		return result;
	}

	//only for use with integer division
	//not tested with negative numbers
	public static long IREM(long a, long b) {
		long c = a % b;
		//note not multiplied by 64000
		return c;
	}

	/** It might be more efficient to save this directly to a char array that is passed in.
	* That will wait for a later version.
	*/
	public static char[] toCharArray(long lv) {
		if (lv > MAX_INT || lv < (0 - MAX_INT)) {
			throw new IllegalArgumentException(lv+ " is out of bounds");
		}
		char[] ca = new char[3];
		//do the fractional part
		int c2 = (int)(lv % K64);
		lv = lv / K64;
		int c1 = (int)(lv % K64);
		lv = lv / K64;
		if (lv > K64) throw new IllegalArgumentException(lv+" is too large");
		int c0 = (int)lv;
		ca[0]=(char)c0;
		ca[1]=(char)c1;
		ca[2]=(char)c2;
		return ca;
	}

	public static long fromCharArray(char[] ca) {
		if (ca.length>3) throw new IllegalArgumentException("char array is too big "+ca.length);
		return (long)ca[0]*K64*K64 + (long)ca[1]*K64 + (long)ca[2];
	}

	public static void main(String[] args) {
		float fa = Float.parseFloat(args[0]);
		float fb = Float.parseFloat(args[1]);
		long la = toNumber(fa);
		System.out.println("a = "+ la);
		long lb = toNumber(fb);
		System.out.println("b = "+ lb);
		//long d = DIV(la,lb);
		long d = ADD(la,lb);
		System.out.println("d = "+ d);
		float fc = toFloat(d);
		System.out.println(fc);
	}

}