/**
Three-Quarter Float is based on HalfFloat.  Three-quarter is another way of saying 24-bit.
I care a lot more about the precision than I do the size
HalfFloat has 5 bits for the exponent and 10 for the significand (plus 1 more being implicit)
	and 1 for the sign bit.
ThreeQuarterFloat (this version) has 6 bits for the exponent and 17 for the significand (plus an implicit)
	and 1 for the sign bit.

My requirements are that it hold numbers up to 1 million, and up to 1/1000 with minimal rounding.
*/
public class ThreeQuarterFloat {

	//see Float.floatToIntBits()
	public static int floatToIntBits(float fval) {
		int sign = 0;
		int exp = 0; 	//this is 6 bits (0..5), so from -32 to 31
		int mant = 0;	//this is 18 bits, but the leading bit is discarded

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
		System.out.println("floatToIntBits: fval = "+fval);
		mant = (int)( (fval - 1.0f) * 131072); // 2^17
		System.out.println("floatToIntBits: mant = "+mant);
		//calculated without the leading 1

		//assemble it
		int sign2 = sign * 8388608;	// 2^24 / 2
		//add 31 to bias the exponent
		int exp2 = (exp + 31) * 131072;
		return sign2 + exp2 + mant;
	}

	//see Float.intBitsToFloat()
	public static float intBitsToFloat(int b) {
		if (b==0) {
			return 0.0f;
		}

		int sign = 0;	//sign is either 0 for positive or 1 for negative
		if (b > 8388607) {
			sign = 1;
			b = b - 8388608;	// 2^24 / 2
		}

		//get the biased exponent
		int exp = b / 131072;
		exp = exp - 31;	//remove bias

		//get the mantissa
		int mant = b % 131072;	//remainder after extracting the exponent
		System.out.println("intBitsToFloat: mant = "+mant);
		final float divisor = 131072.0F;	//same as 0x800000
		float fval = ((float)mant / divisor) + 1.0f;
		System.out.println("intBitsToFloat: fval = "+fval);
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
		int i2 = floatToIntBits(f);
		System.out.println(i2);
		//=========
		float f3 = intBitsToFloat(i2);
		System.out.println(f3);
		System.out.println((int)f3);
	}
}
