package why.stock.futu;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.protobuf.GeneratedMessageV3;
import com.google.protobuf.Parser;
import com.sun.jna.Memory;
import com.sun.jna.Pointer;

import Common.Common.PacketID;
import Qot_Common.QotCommon.*;
import Trd_Common.TrdCommon.*;

/**
 * @author why
 *  @date 2019/04/13 10:09 created
 */

@SuppressWarnings("deprecation")
public class Proto {
	
	private Socket socket;
	
	private long connID, userID, milliSeconds, keepAliveInterval;
	
	private int nSerialNo = 2, timeOut; //tenth of second

	private MessageDigest sha1MsgDigest;
	
	public static final int DefaultTimeOut = 3000;
	
	public static void println(GeneratedMessageV3 response){
		System.out.println(Baser.octal2String(response.toString(), ": \"\\"));
	}
	
	public static void main(final String... args) throws Exception{
		
		String code = "00700";
		GeneratedMessageV3 response;
		QotMarket market = QotMarket.QotMarket_HK_Security;
		List<String> codes = Arrays.asList(code);
		
		Proto ctx = OpenContext("127.0.0.1", 11111, "GetTester");
		
		Thread.sleep(3000);
		println(response = ctx.subscribe(market, codes, Arrays.asList(SubType.SubType_Basic, SubType.SubType_Ticker, SubType.SubType_OrderBook, SubType.SubType_Broker), true));
		Thread.sleep(3000);
		println(response = ctx.get_stock_quote(market, codes));
		Thread.sleep(3000);
		System.err.println(new Date((long)((Qot_GetBasicQot.QotGetBasicQot.Response)response).getS2C().getBasicQotList(0).getUpdateTimestamp()*1000));
		
		Thread.sleep(3000);
		response = ctx.get_rt_ticker(market, code, 1000);
		System.err.println("Ticker " + ((Qot_GetTicker.QotGetTicker.Response)response).getS2C().getTickerListCount());
		println(response = ctx.get_order_book(market, code, 10));
		println(response = ctx.get_broker_queue(market, code));
		
		println(response = ctx.get_acc_list());
		List<TrdAcc> trdAccs = ((Trd_GetAccList.TrdGetAccList.Response)response).getS2C().getAccListList();
		TrdAcc trdAcc = Baser.firstAcc(trdAccs, TrdMarket.TrdMarket_US, true);
		TrdHeader trdHeader = Baser.trdAcc2Header(trdAcc);
		
		println(response = ctx.unlock_trade("", true));
		println(response = ctx.accinfo_query(trdHeader));
		println(response = ctx.position_list_query(trdHeader));
		println(response = ctx.order_list_query(trdHeader));
//		println(response = ctx.place_order(trdHeader, 400, 100, code, TrdSide.TrdSide_Buy, OrderType.OrderType_Normal));
//		println(response = ctx.modify_order(trdHeader, ModifyOrderOp.ModifyOrderOp_Normal, ((Trd_PlaceOrder.TrdPlaceOrder.Response)response).getS2C().getOrderID(), 399, 100));
		
//		println(response = ctx.subscribe(market, SubType.SubType_Ticker, codes, false));		
		ctx.close();
		
		Proto proto = OpenContext("127.0.0.1", 11111, true, false, "PushTester", DefaultTimeOut);
		System.out.println(response = proto.subscribe(market, codes, Arrays.asList(SubType.SubType_Basic, SubType.SubType_OrderBook), true));
		proto.push_run(new ANotifyReveiver(){
			@Override
			protected void update(Qot_UpdateBasicQot.QotUpdateBasicQot.Response response) throws IOException{
				println(response);
			}
			@Override
			protected void update(Qot_UpdateOrderBook.QotUpdateOrderBook.Response response) throws IOException{
				println(response);
				proto.close();
				System.exit(0);
			}
		}, market, codes, Arrays.asList(SubType.SubType_Basic, SubType.SubType_OrderBook), Arrays.asList(), true, true);
	}
	
	private Proto(Socket socket, long connID, long userID, int keepAliveInterval, MessageDigest sha1MsgDigest, int timeOut){
		this.socket = socket;
		this.connID = connID;
		this.userID = userID;
		this.keepAliveInterval = keepAliveInterval * 1000;
		this.milliSeconds = System.currentTimeMillis();
		this.sha1MsgDigest = sha1MsgDigest;
		this.timeOut = timeOut/100;
	}
	
	public void close() throws IOException{
		socket.close();
	}
	
	public void setTimeOut(int timeOut) { //millisecond
		this.timeOut = timeOut/100;
	}
	
	public static Proto OpenContext(String host, int port, String client) throws IOException, InterruptedException, NoSuchAlgorithmException{
		return OpenContext(host, port, false, false, client, DefaultTimeOut);
	}
	public static Proto OpenContext(String host, int port, boolean recvNotify, boolean encrypt, String client, int timeOut) throws IOException, InterruptedException, NoSuchAlgorithmException{
		Socket s = new Socket(host, port);
		InitConnect.InitConnect.C2S.Builder cb = InitConnect.InitConnect.C2S.newBuilder();
		cb.setClientVer(300);
		cb.setClientID("Java" + client);
		cb.setRecvNotify(recvNotify);
		cb.setPacketEncAlgo(encrypt ? 4 : 0);
		
		MessageDigest sha1MsgDigest = MessageDigest.getInstance("SHA1");
		InitConnect.InitConnect.Response response = tcp(s, 1001, InitConnect.InitConnect.Request.newBuilder().setC2S(cb.build()).build(), InitConnect.InitConnect.Response.PARSER, 1, timeOut/100, sha1MsgDigest);
		if(response==null){
			s.close();
			return null;
		}else{
			InitConnect.InitConnect.S2C s2c = response.getS2C();
			return new Proto(s, s2c.getConnID(), s2c.getLoginUserID(), s2c.getKeepAliveInterval(), sha1MsgDigest, timeOut);
		} 
	}

	private KeepAlive.KeepAlive.Response keep_alive() throws IOException, InterruptedException{
		return tcp(socket, 1004, KeepAlive.KeepAlive.Request.newBuilder().setC2S(KeepAlive.KeepAlive.C2S.newBuilder().setTime(new Date().getTime()/1000).build()).build(), 
				KeepAlive.KeepAlive.Response.PARSER, nSerialNo++, timeOut, sha1MsgDigest);
	}
	
	protected Security getSecurity(QotMarket market, String code){
		return getSecurity(market == QotMarket.QotMarket_CNSZ_Security, market.getNumber(), code);
	}
	protected Security getSecurity(boolean sz, int mkt, String code){
		return Security.newBuilder().setMarket(sz && code.charAt(0)=='6' ? QotMarket.QotMarket_CNSH_Security_VALUE : mkt).setCode(code).build();
	}
	
	public Qot_Sub.QotSub.Response subscribe(QotMarket market, List<String> codes, List<SubType> subTypes, boolean subIfTrueElseUnscribe) throws IOException, InterruptedException{
		int mkt = market.getNumber();
		boolean sz = market == QotMarket.QotMarket_CNSZ_Security;
		Qot_Sub.QotSub.C2S.Builder cb = Qot_Sub.QotSub.C2S.newBuilder();
		cb.setIsSubOrUnSub(subIfTrueElseUnscribe);
		for(String code : codes)
			cb.addSecurityList(getSecurity(sz, mkt, code));
		for(SubType subType : subTypes)
			cb.addSubTypeList(subType.getNumber());
		
		return tcp(3001, Qot_Sub.QotSub.Request.newBuilder().setC2S(cb.build()).build(), Qot_Sub.QotSub.Response.PARSER);
	}
	
	public Qot_GetTicker.QotGetTicker.Response get_rt_ticker(QotMarket market, String code, int max) throws IOException, InterruptedException{
		return tcp(3010, Qot_GetTicker.QotGetTicker.Request.newBuilder().setC2S(Qot_GetTicker.QotGetTicker.C2S.newBuilder().setSecurity(getSecurity(market, code)).setMaxRetNum(max).build()).build(), 
				Qot_GetTicker.QotGetTicker.Response.PARSER);
	}
	
	public Qot_GetBasicQot.QotGetBasicQot.Response get_stock_quote(QotMarket market, List<String> codes) throws IOException, InterruptedException{
		int mkt = market.getNumber();
		boolean sz = market == QotMarket.QotMarket_CNSZ_Security;
		Qot_GetBasicQot.QotGetBasicQot.C2S.Builder cb = Qot_GetBasicQot.QotGetBasicQot.C2S.newBuilder();
		for(String code : codes)
			cb.addSecurityList(getSecurity(sz, mkt, code));
		return tcp(3004, Qot_GetBasicQot.QotGetBasicQot.Request.newBuilder().setC2S(cb.build()).build(), Qot_GetBasicQot.QotGetBasicQot.Response.PARSER);
	}
	
	public Qot_GetRT.QotGetRT.Response get_rt(QotMarket market, String code) throws IOException, InterruptedException{
		return tcp(3008, Qot_GetRT.QotGetRT.Request.newBuilder().setC2S(Qot_GetRT.QotGetRT.C2S.newBuilder().setSecurity(getSecurity(market, code)).build()).build(), Qot_GetRT.QotGetRT.Response.PARSER);
	}
	
	private Qot_GetOrderBook.QotGetOrderBook.Request get_order_book_request(QotMarket market, String code, int num){
		return Qot_GetOrderBook.QotGetOrderBook.Request.newBuilder().setC2S(Qot_GetOrderBook.QotGetOrderBook.C2S.newBuilder().setSecurity(getSecurity(market, code)).setNum(num).build()).build();
	}
	
	public Qot_GetOrderBook.QotGetOrderBook.Response get_order_book(QotMarket market, String code, int num) throws IOException, InterruptedException{
		return tcp(3012, get_order_book_request(market, code, num), Qot_GetOrderBook.QotGetOrderBook.Response.PARSER);
	}
	
	public Qot_GetBroker.QotGetBroker.Response get_broker_queue(QotMarket market, String code) throws IOException, InterruptedException{
		return tcp(3014, Qot_GetBroker.QotGetBroker.Request.newBuilder().setC2S(Qot_GetBroker.QotGetBroker.C2S.newBuilder().setSecurity(getSecurity(market, code)).build()).build(), 
				Qot_GetBroker.QotGetBroker.Response.PARSER);
	}
	
	public Qot_GetOrderDetail.QotGetOrderDetail.Response get_order_detail(QotMarket market, String code) throws IOException, InterruptedException{
		return tcp(3016, Qot_GetOrderDetail.QotGetOrderDetail.Request.newBuilder().setC2S(Qot_GetOrderDetail.QotGetOrderDetail.C2S.newBuilder().setSecurity(getSecurity(market, code)).build()).build(), 
				Qot_GetOrderDetail.QotGetOrderDetail.Response.PARSER);
	}
	
	public Trd_GetAccList.TrdGetAccList.Response get_acc_list() throws IOException, InterruptedException{
		return tcp(2001, Trd_GetAccList.TrdGetAccList.Request.newBuilder().setC2S(Trd_GetAccList.TrdGetAccList.C2S.newBuilder().setUserID(userID).build()).build(), Trd_GetAccList.TrdGetAccList.Response.PARSER);
	}
	
	public Trd_UnlockTrade.TrdUnlockTrade.Response unlock_trade(String password, boolean is_unlock) throws IOException, InterruptedException, NoSuchAlgorithmException{
		Trd_UnlockTrade.TrdUnlockTrade.C2S.Builder cb = Trd_UnlockTrade.TrdUnlockTrade.C2S.newBuilder().setUnlock(is_unlock);
		if(password!=null){
			MessageDigest md5MsgDigest = MessageDigest.getInstance("MD5");
			md5MsgDigest.update(password.getBytes());
			cb.setPwdMD5(Baser.bytesToHex(md5MsgDigest.digest()));
		}
		return tcp(2005, Trd_UnlockTrade.TrdUnlockTrade.Request.newBuilder().setC2S(cb.build()).build(), Trd_UnlockTrade.TrdUnlockTrade.Response.PARSER);
	}
	
	public Trd_GetFunds.TrdGetFunds.Response accinfo_query(TrdHeader trdHeader) throws IOException, InterruptedException{
		return tcp(2101, Trd_GetFunds.TrdGetFunds.Request.newBuilder().setC2S(Trd_GetFunds.TrdGetFunds.C2S.newBuilder().setHeader(trdHeader).build()).build(), Trd_GetFunds.TrdGetFunds.Response.PARSER);
	}
	
	public Trd_GetPositionList.TrdGetPositionList.Response position_list_query(TrdHeader trdHeader) throws IOException, InterruptedException{
		return tcp(2102, Trd_GetPositionList.TrdGetPositionList.Request.newBuilder().setC2S(Trd_GetPositionList.TrdGetPositionList.C2S.newBuilder().setHeader(trdHeader).build()).build(), 
				Trd_GetPositionList.TrdGetPositionList.Response.PARSER);
	}
	
	public Trd_GetOrderList.TrdGetOrderList.Response order_list_query(TrdHeader trdHeader) throws IOException, InterruptedException{
		return tcp(2201, Trd_GetOrderList.TrdGetOrderList.Request.newBuilder().setC2S(Trd_GetOrderList.TrdGetOrderList.C2S.newBuilder().setHeader(trdHeader).build()).build(), 
					Trd_GetOrderList.TrdGetOrderList.Response.PARSER);
	}
	
	public Trd_PlaceOrder.TrdPlaceOrder.Response place_order(TrdHeader trdHeader, double price, long qty, String code, TrdSide trd_side, OrderType order_type) throws IOException, InterruptedException{
		Trd_PlaceOrder.TrdPlaceOrder.C2S.Builder cb = Trd_PlaceOrder.TrdPlaceOrder.C2S.newBuilder();
		cb.setHeader(trdHeader);
		cb.setCode(code);
		cb.setOrderType(order_type.getNumber());
		PacketID.Builder pb = PacketID.newBuilder();
		pb.setConnID(connID);
		pb.setSerialNo(nSerialNo);
		cb.setPacketID(pb.build());
		cb.setPrice(price);
		cb.setQty(qty);
		cb.setTrdSide(trd_side.getNumber());
		
		return tcp(2202, Trd_PlaceOrder.TrdPlaceOrder.Request.newBuilder().setC2S(cb.build()).build(), Trd_PlaceOrder.TrdPlaceOrder.Response.PARSER);
	}
	
	public Trd_ModifyOrder.TrdModifyOrder.Response modify_order(TrdHeader trdHeader, ModifyOrderOp modify_order_op, long order_id, double price, long qty) throws IOException, InterruptedException{
		Trd_ModifyOrder.TrdModifyOrder.C2S.Builder cb = Trd_ModifyOrder.TrdModifyOrder.C2S.newBuilder();
		cb.setHeader(trdHeader);
		PacketID.Builder pb = PacketID.newBuilder();
		pb.setConnID(connID);
		pb.setSerialNo(nSerialNo);
		cb.setPacketID(pb.build());
		cb.setPrice(price);
		cb.setQty(qty);
		cb.setModifyOrderOp(modify_order_op.getNumber());
		cb.setOrderID(order_id);
		
		return tcp(2205, Trd_ModifyOrder.TrdModifyOrder.Request.newBuilder().setC2S(cb.build()).build(), Trd_ModifyOrder.TrdModifyOrder.Response.PARSER);
	}
	
	protected boolean check_keep_alive() throws IOException, InterruptedException{
		long curMilliSeconds = System.currentTimeMillis();
		if(curMilliSeconds-milliSeconds>keepAliveInterval){
			KeepAlive.KeepAlive.Response response = this.keep_alive();
			this.milliSeconds = System.currentTimeMillis();
			return response!=null;
		}
		return true;
	}
	
	protected synchronized <T extends GeneratedMessageV3> T tcp(int nProtoID, GeneratedMessageV3 req, Parser<T> parser) throws IOException, InterruptedException{
		check_keep_alive();
		return tcp(socket, nProtoID, req, parser, nSerialNo++, timeOut, sha1MsgDigest);
	}
	
	private static APIProtoHeader write(OutputStream os, int nProtoID, GeneratedMessageV3 req, int nSerialNo, MessageDigest sha1MsgDigest) throws IOException{
        byte[] to = req.toByteArray();
        APIProtoHeader header = new APIProtoHeader(nProtoID, nSerialNo, to, sha1MsgDigest);
        header.write();
        int hsize = header.size();   
        os.write(header.getPointer().getByteArray(0, hsize));
        os.write(to);
        return header;
	}
	
	private static void read(InputStream is, int timeOut, ByteArrayOutputStream baos) throws IOException, InterruptedException{
		int b, j=0;
        while((b=is.available())==0 && j++<timeOut)
        	Thread.sleep(100);
        do{
        	for(int i=0; i<b; i++)
        		baos.write(is.read());
        }while((b=is.available())>0);
	}
	
	private static <T extends GeneratedMessageV3> T read(InputStream is, APIProtoHeader header, Parser<T> parser, int timeOut, ByteArrayOutputStream baos, int off, int hsize) 
			throws IOException, InterruptedException{
    	if(baos.size() < off + hsize)
    		read(is, timeOut, baos);
    	byte[] datas = baos.toByteArray();
    	if(datas.length < off + hsize)
    		return null;
    
        Pointer p = new Memory(hsize);
        p.write(0, datas, off, hsize);
        APIProtoHeader head = new APIProtoHeader(p);
        head.read();
        if(head.nProtoID!=header.nProtoID|| head.nProtoVer!=header.nProtoVer || head.nProtoFmtType!=header.nProtoFmtType 
        		|| head.szHeaderFlag[0]!=header.szHeaderFlag[0] || head.szHeaderFlag[1]!=header.szHeaderFlag[1]) // || head.nSerialNo!=header.nSerialNo
        	return null;
        
        off += hsize;
        header.nBodyLen = head.nBodyLen;
    	return parser.parseFrom(Arrays.copyOfRange(datas, off, off + head.nBodyLen));
	}
	
	private static <T extends GeneratedMessageV3> T tcp(Socket socket, int nProtoID, GeneratedMessageV3 req, Parser<T> parser, int nSerialNo, int timeOut, MessageDigest sha1MsgDigest) 
			throws IOException, InterruptedException
	{   
		OutputStream os = socket.getOutputStream();
		APIProtoHeader header = write(os, nProtoID, req, nSerialNo, sha1MsgDigest);
        os.flush();
        return read(socket.getInputStream(), header, parser, timeOut, new ByteArrayOutputStream(), 0, header.size());
	}
	
	public static interface INotifyReveiver{
		void update(GeneratedMessageV3 response) throws IOException;
	}
	public static abstract class ANotifyReveiver implements INotifyReveiver{
		
		@Override
		final public void update(GeneratedMessageV3 response) throws IOException{
			if(response instanceof Notify.Notify.Response)
				update((Notify.Notify.Response)response);
			else if(response instanceof Trd_UpdateOrder.TrdUpdateOrder.Response)
				update((Trd_UpdateOrder.TrdUpdateOrder.Response)response);
			else if(response instanceof Trd_UpdateOrderFill.TrdUpdateOrderFill.Response)
				update((Trd_UpdateOrderFill.TrdUpdateOrderFill.Response)response);
			else if(response instanceof Qot_UpdateBasicQot.QotUpdateBasicQot.Response)
				update((Qot_UpdateBasicQot.QotUpdateBasicQot.Response)response);
			else if(response instanceof Qot_UpdateKL.QotUpdateKL.Response)
				update((Qot_UpdateKL.QotUpdateKL.Response)response);
			else if(response instanceof Qot_UpdateRT.QotUpdateRT.Response)
				update((Qot_UpdateRT.QotUpdateRT.Response)response);
			else if(response instanceof Qot_UpdateTicker.QotUpdateTicker.Response)
				update((Qot_UpdateTicker.QotUpdateTicker.Response)response);
			else if(response instanceof Qot_UpdateOrderBook.QotUpdateOrderBook.Response)
				update((Qot_UpdateOrderBook.QotUpdateOrderBook.Response)response);
			else if(response instanceof Qot_UpdateBroker.QotUpdateBroker.Response)
				update((Qot_UpdateBroker.QotUpdateBroker.Response)response);
			else if(response instanceof Qot_UpdateOrderDetail.QotUpdateOrderDetail.Response)
				update((Qot_UpdateOrderDetail.QotUpdateOrderDetail.Response)response);
		}
		
		protected void update(Notify.Notify.Response response) throws IOException{
		}
		protected void update(Trd_UpdateOrder.TrdUpdateOrder.Response response) throws IOException{
		}
		protected void update(Trd_UpdateOrderFill.TrdUpdateOrderFill.Response response) throws IOException{
		}
		protected void update(Qot_UpdateBasicQot.QotUpdateBasicQot.Response response) throws IOException{
		}
		protected void update(Qot_UpdateKL.QotUpdateKL.Response response) throws IOException{
		}
		protected void update(Qot_UpdateRT.QotUpdateRT.Response response) throws IOException{
		}
		protected void update(Qot_UpdateTicker.QotUpdateTicker.Response response) throws IOException{
		}
		protected void update(Qot_UpdateOrderBook.QotUpdateOrderBook.Response response) throws IOException{
		}
		protected void update(Qot_UpdateBroker.QotUpdateBroker.Response response) throws IOException{
		}
		protected void update(Qot_UpdateOrderDetail.QotUpdateOrderDetail.Response response) throws IOException{
		}
	}
	
	public boolean push_run(INotifyReveiver notifyReveiver, QotMarket market, List<String> codes, List<SubType> subTypes, List<RehabType> rehabTypes, boolean regIfTrueElseUnReg, boolean isFirstPush) 
			throws IOException, InterruptedException{
		
		Qot_RegQotPush.QotRegQotPush.C2S.Builder cb = Qot_RegQotPush.QotRegQotPush.C2S.newBuilder();
		for(String code : codes)
			cb.addSecurityList(getSecurity(market, code));
		for(SubType subTupe : subTypes)
			cb.addSubTypeList(subTupe.getNumber());
		for(RehabType rehabType : rehabTypes)
			cb.addRehabTypeList(rehabType.getNumber());
		cb.setIsRegOrUnReg(regIfTrueElseUnReg);
		cb.setIsFirstPush(isFirstPush);
				
		if(isFirstPush){
			OutputStream os = socket.getOutputStream();
			write(os, 3002, Qot_RegQotPush.QotRegQotPush.Request.newBuilder().setC2S(cb.build()).build(), nSerialNo++, sha1MsgDigest);
	        os.flush();
		}else{
			Qot_RegQotPush.QotRegQotPush.Response response = tcp(3002, Qot_RegQotPush.QotRegQotPush.Request.newBuilder().setC2S(cb.build()).build(), Qot_RegQotPush.QotRegQotPush.Response.PARSER);
			if(response==null || response.getRetType()!=0){
				System.err.println(response==null ? "注册推送错误" : response.getRetMsg());
				return false;
			}
		}
		
		int hsize = 44;
		InputStream is = socket.getInputStream();
		int keepAliveInterval = (int) (this.keepAliveInterval / 100);
		Map<Integer, Parser<? extends GeneratedMessageV3>> parserMap = new HashMap<>(16);
		parserMap.put(1003, Notify.Notify.Response.PARSER);
		parserMap.put(2208, Trd_UpdateOrder.TrdUpdateOrder.Response.PARSER);
		parserMap.put(2218, Trd_UpdateOrderFill.TrdUpdateOrderFill.Response.PARSER);
		parserMap.put(3005, Qot_UpdateBasicQot.QotUpdateBasicQot.Response.PARSER);
		parserMap.put(3007, Qot_UpdateKL.QotUpdateKL.Response.PARSER);
		parserMap.put(3009, Qot_UpdateRT.QotUpdateRT.Response.PARSER);
		parserMap.put(3011, Qot_UpdateTicker.QotUpdateTicker.Response.PARSER);
		parserMap.put(3013, Qot_UpdateOrderBook.QotUpdateOrderBook.Response.PARSER);
		parserMap.put(3015, Qot_UpdateBroker.QotUpdateBroker.Response.PARSER);
		parserMap.put(3017, Qot_UpdateOrderDetail.QotUpdateOrderDetail.Response.PARSER);
		
		while(regIfTrueElseUnReg){
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			read(is, keepAliveInterval, baos);
			byte[] datas = baos.toByteArray();
			int off = 0, size = baos.size();
			
			while(off < size - hsize){
				Pointer p = new Memory(hsize);
		        p.write(0, datas, off, hsize);
		        APIProtoHeader head = new APIProtoHeader(p);
		        head.read();
		        
		        off += hsize;
		        int end = off + head.nBodyLen;
		        if(head.szHeaderFlag[0]!='F' || head.szHeaderFlag[1]!='T' || size<end)
		        	break;
		        
		        Parser<? extends GeneratedMessageV3> parser = parserMap.get(head.nProtoID);
		        if(parser!=null)
		        	notifyReveiver.update(parser.parseFrom(Arrays.copyOfRange(datas, off, end)));
		        else
		        	System.err.println("Unknown update proto id " + head.nProtoID);
		        off = end;
			}
			if(!check_keep_alive())
				break;
		}
		
		return true;
	}
	
//	协议设计
//	协议数据包括协议头以及协议体，协议头固定字段，协议体根据具体协议决定

	//	协议头结构
//	szHeaderFlag	包头起始标志，固定为“FT”
//	nProtoID	协议ID
//	nProtoFmtType	协议格式类型，0为Protobuf格式，1为Json格式
//	nProtoVer	协议版本，用于迭代兼容, 目前填0
//	nSerialNo	包序列号，用于对应请求包和回包, 要求递增
//	nBodyLen	包体长度
//	arrBodySHA1	包体原始数据(解密后)的SHA1哈希值
//	arrReserved	保留8字节扩展
	public static class APIProtoHeader extends Structure
	{
		public byte szHeaderFlag[] = {'F', 'T'};
		public int nProtoID;
		public byte nProtoFmtType;
		public byte nProtoVer;
		public int nSerialNo;
		public int nBodyLen;
		public byte arrBodySHA1[] = new byte[20];
		public byte arrReserved[] = {0, 0, 0, 0, 0, 0, 0, 0};
	    
	    public APIProtoHeader(int nProtoID, int nSerialNo, byte[] bodys, MessageDigest sha1MsgDigest){
	    	this.nProtoID = nProtoID;
	    	this.nSerialNo = nSerialNo;
	    	this.nBodyLen = bodys.length;
	    	sha1MsgDigest.update(bodys);
	    	this.arrBodySHA1 = sha1MsgDigest.digest();
		
		if(ByteOrder.nativeOrder() == ByteOrder.BIG_ENDIAN){
		    	this.nProtoID = Baser.bigEndian2LittleEndian32(nProtoID);
		    	this.nSerialNo = Baser.bigEndian2LittleEndian32(nSerialNo);
		    	this.nBodyLen = Baser.bigEndian2LittleEndian32(nBodyLen);
	    	}    
	    }
	    
	    public APIProtoHeader(Pointer p){
			super(p);
		    
		    	if(ByteOrder.nativeOrder() == ByteOrder.BIG_ENDIAN){
		    		this.nProtoID = Baser.bigEndian2LittleEndian32(nProtoID);
		    		this.nSerialNo = Baser.bigEndian2LittleEndian32(nSerialNo);
		    		this.nBodyLen = Baser.bigEndian2LittleEndian32(nBodyLen);
	    		}
		}
	}
	
//	FutuOpenD内部处理使用Protobuf，因此协议格式建议使用Protobuf，减少Json转换开销
//	nProtoFmtType字段指定了包体的数据类型，回包会回对应类型的数据；推送协议数据类型由FutuOpenD配置文件指定
//	arrBodySHA1用于校验请求数据在网络传输前后的一致性，必须正确填入
//	协议头的二进制流使用的是小端字节序，即一般不需要使用ntohl等相关函数转换数据
	
//	协议体结构
//	Protobuf协议请求包体结构
//
//	message C2S
//	{
//	    required int64 req = 1;
//	}
//
//	message Request
//	{
//	    required C2S c2s = 1;
//	}
//	Protobuf协议回应包体结构
//
//	message S2C
//	{
//	    required int64 data = 1;
//	}
//
//	message Response
//	{
//	    required int32 retType = 1 [default = -400]; //RetType,返回结果
//	    optional string retMsg = 2;
//	    optional int32 errCode = 3;
//	    optional S2C s2c = 4;
//	}
	
//	Json协议请求包体结构
//
//	{
//	    "c2s":
//	    {
//	         "req": 0
//	    }
//	}
//	Json协议回应包体结构
//
//	{
//	    "retType" : 0
//	    "retMsg" : ""
//	    "errCode" : 0
//	    "s2c":
//	    {
//	        "data": 0
//	    }
//	}
//	字段	说明
//	c2s	请求参数结构
//	req	请求参数，实际根据协议定义
//	retType	请求结果
//	retMsg	若请求失败，说明失败原因
//	errCode	若请求失败对应错误码
//	s2c	回应数据结构，部分协议不返回数据则无该字段
//	data	回应数据，实际根据协议定义
}

