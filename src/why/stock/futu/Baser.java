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
	
	public static String octal2String(String octal, String split){
		Vector<String> strs = Util.strSplit(octal, split, true, false, new Vector<String>());
		for(int i=1, n=strs.size(); i<n; i++){
			String str = strs.get(i);
			int off = str.indexOf('"');
			strs.set(i, transfromOctalToString("\\" + str.substring(0, off)) + str.substring(off+1));
		}
		return Util.join(strs, split.substring(0, split.length()-2));
	}
	
	/**将包含八进制数据的字符串转换为汉字字符串。
	* 例如将 dynamicMsg: \344\270\215\350\203\27510 转换为 dynamicMsg: 不能10*/
	public static String transfromOctalToString(String dataStr) {
//		if (! dataStr.contains("\\")) {
//			return dataStr;
//		}
		//正常字符
		StringBuilder buffer = new StringBuilder();
		//八进制的内容，转成十六进制缓存
		StringBuilder hexBuffer = new StringBuilder();
		for (int i = 0; i < dataStr.length(); i ++) {
			char c = dataStr.charAt(i);
			if (c != '\\') {
				buffer.append(c);
			}
			//反斜杠往后3个为一组，组成了一个八进制数。例如\20710,其实是207组成了一个八进制数
			else {
				//将八进制转换为十进制，再转换为十六进制
				String hex = Integer.toHexString((Integer.valueOf(dataStr.substring(i+1, i+4), 8)));
				i += 3;
				//先缓存住，直到凑够三个字节
				hexBuffer.append(hex);
				//utf8编码中，三个字节为一个汉字
				if (hexBuffer.length() == 6) {
					//凑够三个字节了，转成汉字后放入oldBuffer中
					buffer.append(hexStr2Str(hexBuffer.toString()));
					//凑够一个汉字了，清空缓存
					hexBuffer = new StringBuilder();
				}
			}
		}
		return buffer.toString();
	}
	/**
	* 十六进制转换字符串
	*/
	private static String hexStr2Str(String hexStr) {
		String str = "0123456789abcdef";
		char[] hexs = hexStr.toCharArray();
		byte[] bytes = new byte[hexStr.length() / 2];
		int n;
		for (int i = 0; i < bytes.length; i++) {
			n = str.indexOf(hexs[2 * i]) * 16;
			n += str.indexOf(hexs[2 * i + 1]);
			bytes[i] = (byte) (n & 0xff);
		}
		return new String(bytes, StandardCharsets.UTF_8);
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
