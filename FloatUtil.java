//this attempts to calculate float to int bits by hand
//
//known problems:
//	-0.0	floatToIntBits shows -2147483648, this shows it as 0
public class FloatUtil {

	//see Float.floatToIntBits()
	public static int floatToIntBits(float fval) {
		int sign = 0;
		int exp = 0; 	//this is 8 bits (0..7), so from -128 to 127
		int mant = 0;	//this is 24 bits, but the leading bit is discarded

		if (fval == 0.0f) {
			return 0;
		}

		//first get the sign
		if (fval < 0.0f) {
			sign = 1;
			fval = 0.0f - fval;
		}

		//normalize the number.
		//We want the mantissa between 1 and 2
		while (fval >= 2.0f) {
			exp += 1;
			fval = fval / 2.0f;
		}
		while (fval < 1.0f) {
			exp -=1;
			fval = fval * 2.0f;
		}

		//the number should now be normalized
		//print fval with leading 1
		//System.out.println("floatToIntBits: fval = "+fval);
		mant = (int)( (fval - 1.0f) * 0x800000); //8388608); //2^23
		//System.out.println("floatToIntBits: mant = "+mant);
		//calculated without the leading 1

		//assemble it
		// multiply sign by 2147483648, which is 2^24/2
		// and is the same as 0x80000000
		int sign2 = sign * 0x80000000;
		//add 127 to bias the exponent
		int exp2 = (exp + 127) * 0x800000;
		return sign2 + exp2 + mant;
	}

	//see Float.intBitsToFloat()
	public static float intBitsToFloat(int b) {
		if (b==0) {
			return 0.0f;
		}

		int sign = 0;	//sign is either 0 for positive or 1 for negative
		if (b < 0) {
			sign = 1;
			b = b + 0x80000000;	//2147483648
		}

		//get the biased exponent
		int exp = b / 0x800000;
		exp = exp - 127;	//remove bias

		//get the mantissa
		int mant = b % 0x800000;	//remainder after extracting the exponent
		//System.out.println("intBitsToFloat: mant = "+mant);
		final float divisor = 8388608.0F;	//same as 0x800000
		float fval = ((float)mant / divisor) + 1.0f;
		//System.out.println("intBitsToFloat: fval = "+fval);
		//-----------
		//System.out.println("intBitsToFloat: sign = "+sign);
		//System.out.println("intBitsToFloat: exp = "+exp);
		//System.out.println("intBitsToFloat: mantissa = "+fval);

		//recreate number
		float n = 0.0f;
		if (exp == 0) {
			n = fval;
		} else {
			float pow = (float)Math.pow(2, exp);
			n = pow * fval;
		}

		//fix sign
		if (sign == 0) {
			return n;
		} else {
			return 0.f - n;
		}
	}

	public static void main(String[] args) {
		float f = Float.parseFloat(args[0]);
		int i = Float.floatToIntBits(f);
		int i2 = floatToIntBits(f);
		System.out.println(i);
		System.out.println(i2);
		//=========
		float f2 = Float.intBitsToFloat(i);
		float f3 = intBitsToFloat(i2);
		System.out.println(f2);
		System.out.println(f3);
	}
}