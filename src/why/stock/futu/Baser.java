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
	
	public static final int bigEndian2LittleEndian32(int x) {
		return (x & 0xFF) << 24 | (0xFF & x >> 8) << 16 | (0xFF & x >> 16) << 8 | (0xFF & x >> 24);
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
