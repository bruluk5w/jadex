package jadex.commons.security.random;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.security.SecureRandom;

import org.spongycastle.crypto.engines.ChaChaEngine;
import org.spongycastle.util.Arrays;
import org.spongycastle.util.Pack;

import jadex.commons.SUtil;
import jadex.commons.security.SSecurity;

public class ChaCha20Random extends SecureRandom
{
	/** ID */
	private static final long serialVersionUID = 0xBD611DFD65C5ABB2L;
	
	/** Seeding source, use SSecurity. */
	protected SecureRandom seedrandom;
	
	/** ChaCha state */
	protected int[] state = new int[16];
	
	protected byte[] outputblock = new byte[64];
	
	/** Pointer to unused output. */
	protected int outptr = 64;
	
	public ChaCha20Random()
	{
		seedrandom = SSecurity.getSeedRandom();
		reseed();
	}
	
	public ChaCha20Random(SecureRandom seedrandom)
	{
		this.seedrandom = seedrandom;
		reseed();
	}
	
	public long nextLong()
	{
		byte[] bytes = new byte[8];
		nextBytes(bytes);
		
		return SUtil.bytesToLong(bytes);
	}
	
	public int nextInt()
	{
		byte[] bytes = new byte[4];
		nextBytes(bytes);
		
		return SUtil.bytesToInt(bytes);
	}
	
	public void nextBytes(byte[] bytes)
	{
		int filled = 0;
		while (filled < bytes.length)
		{
			if (outptr >= outputblock.length)
				nextBlock();
			int len = Math.min(bytes.length - filled, outputblock.length - outptr);
			System.arraycopy(outputblock, outptr, bytes, filled, len);
			filled += len;
			outptr += len;
		}
	}
	
	protected void nextBlock()
	{
		if (state[12] < 0)
			reseed();
		++state[12];
		int[] output = new int[16];
		System.arraycopy(state, 0, output, 0, state.length);
		ChaChaEngine.chachaCore(20, output, output);
		Pack.intToLittleEndian(output, outputblock, 0);
		outptr = 0;
	}
	
	public void reseed()
	{
		int i = 0;
		state[i]   = 0x61707865;
		state[++i] = 0x3320646e;
		state[++i] = 0x79622d32;
		state[++i] = 0x6b206574;
		
//		 = ctr;
		byte[] seedstate = new byte[48];
		seedrandom.nextBytes(seedstate);
		IntBuffer buf = (ByteBuffer.wrap(seedstate)).asIntBuffer();
		while (buf.hasRemaining())
			state[++i] = buf.get();
		
		state[12] = 0;
		
//		RFC 7539 Test Vector
//		state[++i] = 0x03020100;
//		state[++i] = 0x07060504;
//		state[++i] = 0x0b0a0908;
//		state[++i] = 0x0f0e0d0c;
//		state[++i] = 0x13121110;
//		state[++i] = 0x17161514;
//		state[++i] = 0x1b1a1918;
//		state[++i] = 0x1f1e1d1c;
//		state[++i] = 0x00000001;
//		state[++i] = 0x09000000;
//		state[++i] = 0x4a000000;
//		state[++i] = 0x00000000;
		
		// Vector 2
//		state[13] = 0x00000000;
	}
	
//	public static void main(String[] args)
//	{
//		byte[] out = new byte[64];
//		ChaCha20Random r = new ChaCha20Random();
//		r.nextBytes(out);
//		
//		char[] hexArray = "0123456789ABCDEF".toCharArray();
//		char[] hexChars = new char[out.length * 2];
//	    for ( int j = 0; j < out.length; j++ ) {
//	        int v = out[j] & 0xFF;
//	        hexChars[j * 2] = hexArray[v >>> 4];
//	        hexChars[j * 2 + 1] = hexArray[v & 0x0F];
//	    }
//		
//	    System.out.println(new String(hexChars));
//	}
}
