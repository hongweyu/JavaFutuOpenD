package why.stock.futu;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import com.sun.jna.Pointer;

/**
 * @author why
 * 
 * @date 2019/04/13 10:55 created
 */
public class Structure extends com.sun.jna.Structure {

	public Structure() {
		super(ALIGN_NONE);
	}

	/**
	 * @param p
	 */
	public Structure(Pointer p) {
		super(p, ALIGN_NONE);
	}

	/* (non-Javadoc)
	 * @see com.sun.jna.Structure#getFieldOrder()
	 */
	@Override
	protected List getFieldOrder() {
		Field[] fields = getClass().getDeclaredFields(); 
		List<String> names = new ArrayList<String>(fields.length);
		for(Field field : fields)
			names.add(field.getName());
		return names;
	}
	
	public void reverseEndian(boolean bigEndian) throws IllegalArgumentException, ReflectiveOperationException{
		byte[] bytes = {0, 0, 0, 0, 0, 0, 0, 0};
		Field[] fields = getClass().getDeclaredFields(); 
		for(Field field : fields){
			Class cls = field.getType();
			if(cls==int.class){
				field.setInt(this, Baser.reverseEndian(field.getInt(this), bytes, 0, bigEndian));
			}else if(cls==long.class){
				field.setLong(this, Baser.reverseEndian(field.getLong(this), bytes, bigEndian));
			}else if(cls==double.class){
				field.setDouble(this, Baser.reverseEndian(field.getDouble(this), bytes, bigEndian));
			}else if(Structure.class.isAssignableFrom(cls)){ //cls.getSuperclass() == Structure.class
				((Structure) field.get(this)).reverseEndian(bigEndian);
			}
		}
	}

}
