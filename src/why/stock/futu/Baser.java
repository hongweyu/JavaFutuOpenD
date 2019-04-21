package why.stock.futu;

import java.util.List;

import Qot_Common.QotCommon.QotMarket;
import Trd_Common.TrdCommon.TrdAcc;
import Trd_Common.TrdCommon.TrdHeader;
import Trd_Common.TrdCommon.TrdMarket;

/**
 * @author why
 * @date 2019/04/13 09:13 created
 */
public abstract class Baser {
	
	final protected static char[] hexArray = "0123456789abcdef".toCharArray();
	public static String bytesToHex(byte[] bytes) {
	    char[] hexChars = new char[bytes.length * 2];
	    for ( int j = 0; j < bytes.length; j++ ) {
	        int v = bytes[j] & 0xFF;
	        int index = j * 2;
	        hexChars[index] = hexArray[v >>> 4];
	        hexChars[index + 1] = hexArray[v & 0x0F];
	    }
	    return new String(hexChars);
	}

	public static void toBytes(int i, byte[] bytes, int offset, boolean bigEndian){
		if(bigEndian){
			bytes[offset] = (byte)(i>>>24); 
			bytes[offset+1] = (byte)(i>>>16 & 0xFF);
			bytes[offset+2] = (byte)(i>>>8 & 0xFF); 
			bytes[offset+3] = (byte) (i & 0xFF);
		}
		else{
			bytes[offset+3] = (byte)(i>>>24); 
			bytes[offset+2] = (byte)(i>>>16 & 0xFF);
			bytes[offset+1] = (byte)(i>>>8 & 0xFF); 
			bytes[offset] = (byte) (i & 0xFF);
		}
	}
	
	public static void toBytes(long i, byte[] bytes, boolean bigEndian){
		if(bigEndian){
			toBytes((int)(i>>>32), bytes, 0, true);
			toBytes((int)(i&0xFFFFFFFFL), bytes, 4, true);
		}else{
			toBytes((int)(i>>>32), bytes, 4, false);
			toBytes((int)(i&0xFFFFFFFFL), bytes, 0, false);
		}
	}
	
	public static int toInt(byte[] bytes, int offset, boolean bigEndian){
		if(bigEndian)
			return (uint8(bytes[offset])<<24) + (uint8(bytes[offset+1])<<16)
				+ (uint8(bytes[offset+2])<<8) + uint8(bytes[offset+3]);
		else
			return uint8(bytes[offset]) + (uint8(bytes[offset+1])<<8)
				+ (uint8(bytes[offset+2])<<16) + (uint8(bytes[offset+3])<<24);
	}
	
	public static long toLong(byte[] bytes, boolean bigEndian){
		return 0;
	}
	
	public static short uint8(int v){
		return (short)((v<0)?(v+256):v);
	}
	
	public static int reverseEndian(int i, byte[] bytes, int off, boolean bigEndian){
		toBytes(i, bytes, off, bigEndian);
		return toInt(bytes, off, !bigEndian);
	}
	
	public static long reverseEndian(long l, byte[] bytes, boolean bigEndian){
		toBytes(l, bytes, bigEndian);
		return toLong(bytes, !bigEndian);
	}
	
	public static double reverseEndian(double d, byte[] bytes, boolean bigEndian){
		long l = Double.doubleToRawLongBits(d);
		toBytes(l, bytes, bigEndian);
		return Double.longBitsToDouble(toLong(bytes, !bigEndian));
	}
	
	public static TrdMarket trdMktConvert(int mkt){
		TrdMarket mkts[] = {TrdMarket.TrdMarket_CN, TrdMarket.TrdMarket_US, TrdMarket.TrdMarket_HK, TrdMarket.TrdMarket_HKCC};
		return mkts[mkt];
	}
	public static QotMarket qotMktConvert(int mkt){
		QotMarket mkts[] = {QotMarket.QotMarket_CNSZ_Security, QotMarket.QotMarket_US_Security, QotMarket.QotMarket_HK_Security, QotMarket.QotMarket_CNSH_Security};
		return mkts[mkt];
	}
	
	public static TrdAcc firstAcc(List<TrdAcc> trdAccs, TrdMarket market, boolean simulate){
		int env = simulate ? 0 : 1;
		int mkt = market.getNumber();
		for(TrdAcc acc : trdAccs)
			if(acc.getTrdMarketAuthListList().contains(mkt) && acc.getTrdEnv()==env)
				return acc;
		return null;
	}
	public static TrdHeader trdAcc2Header(TrdAcc trdAcc){
		TrdHeader.Builder tb = TrdHeader.newBuilder();
		tb.setAccID(trdAcc.getAccID());
		tb.setTrdEnv(trdAcc.getTrdEnv());
		tb.setTrdMarket(trdAcc.getTrdMarketAuthList(0));
		return tb.build();
	}
}
