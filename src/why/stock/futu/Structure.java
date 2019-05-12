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
}
