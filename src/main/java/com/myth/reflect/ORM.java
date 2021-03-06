package com.myth.reflect;

import com.myth.mysql.Mysql;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 实现了一个查询记录，插入记录 
 *
 * @author  Myth On 2016年9月10日 下午8:20:12
 *  实现了ORM类似操作
 */
public class ORM {

	/**
	 * 将输入对象转换成SQL语句
	 * 不能有除了属性的get方法之外的get方法，不然这里的SQL拼接会失败
	 * 字段的类型暂时只支持 long int Integer String Date
	 * @param obj 输入对象
	 * @return boolean 是否成功 
	 */
	public static boolean save(Object obj){
		Class class1 = obj.getClass();
		StringBuilder sb = new StringBuilder("insert into ");
		StringBuilder va = new StringBuilder("values(");

		Method [] ms = class1.getMethods();
		String className = class1.getName();
		//通过正则表达式来截取类名，赋值给表名
		String tableName = className.split("\\.")[className.split("\\.").length-1];
		sb.append(tableName).append(" (");

		for(Method m:ms){
			String mName = m.getName();
			if(mName.startsWith("get") && !mName.startsWith("getClass")){//将所有get开头的方法取出来
				String colName = mName.substring(3,mName.length());
				sb.append(colName).append(",");
				Class returnType = m.getReturnType();
//				System.out.print(mName+"方法的返回值是"+returnType.getName()+" \n");

				try {
					if(returnType == String.class){
						String p=  (String)m.invoke(obj);
						if(p!=null) va.append("'").append(p).append("',");
					}else if(returnType==long.class || returnType==int.class ){
						long p = (Long)m.invoke(obj);
						va.append(p).append(",");
					}else if( returnType==Integer.class){
						Integer p = (Integer)m.invoke(obj);
						if(p!=null) va.append(p).append(",");
					}else if( returnType==Date.class){
						Date p = (Date)m.invoke(obj);
						StringBuilder pp = new StringBuilder(new SimpleDateFormat("YYYY-MM-dd HH:MM:SS").format(p));
						va.append("'").append(pp.delete(pp.length() - 9, pp.length())).append("',");
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
		sb.delete(sb.length()-1,sb.length());
		va.delete(va.length()-1,va.length());
		sb.append(")");
		va.append(")");

		String sql = sb.toString()+va.toString();
		System.out.println("保存对象的sql是:"+sql);
		return new Mysql().executeUpdateSQL(sql);
	}
	public void invokeValueByMethod(){

	}
	/**
	 * 根据类名字和一个属性名和值，来查询获取一行或多行记录对应的对象
	 * 	对象的类必须要有无参构造器
	 * @param className 类路径 .getclass.getName()即可
	 * @param value  属性值
	 * @return Object 对象
	 */
	public static List<Object> findByProperty(String className, String property, String value){
		Object obj;
		List list = new ArrayList();
		String tableName = className.split("\\.")[className.split("\\.").length-1];
//		System.out.println("表名 : "+tableName);

		Class class1 = null;
		try {
			class1 = Class.forName(className);
		} catch (Exception e) {
			e.printStackTrace();
		}

		//拼接SQL语句
		StringBuilder sb = new StringBuilder();
		sb.append("select * from ").append(tableName).append(" where ")
				.append(property).append("=");

		assert class1 != null;
		Field[] fs = class1.getDeclaredFields();
		for (Field f : fs) {
			if (property.equals(f.getName())) {
				try {
					Type types = f.getGenericType();
					if (types == int.class || types == long.class || types == Integer.class) {
						sb.append("").append(value).append("");
					} else {
						sb.append("'").append(value).append("'");
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
		String sql = sb.toString();
		System.out.println("查询的SQL语句是 ： "+sql);
		return resultSetTurnList(sql,className);
	}
	/**
	 * 获取指定的class对应的表全部的记录
	 * @param className 类名
	 * @return List 对象集合没有泛型
	 */
	public static List getRowsList(String className){
		//正则来切分字符串，返回String数组，取最后一个
		String tableName = className.split("\\.")[className.split("\\.").length-1];
		System.out.println("表名 : "+tableName);

		Mysql db = new Mysql();
		ResultSet rs = db.queryBySQL("select * from " + tableName);
		return  null;
	}

	private static List<Object>resultSetTurnList(String sql,String className){
		List list = new ArrayList();
		Object obj = null;
		Mysql db = null;
		try{
			Class class1 = Class.forName(className);
			db = new Mysql();
			ResultSet rs = db.queryBySQL(sql);
			Method ms[] = class1.getMethods();
			while(rs.next()) {
				obj = class1.newInstance();
				for (Method m : ms) {//获取所有方法
					String mName = m.getName();
					if (mName.startsWith("set")) {//将所有set开头的方法取出来
						//根据方法名字自动提取表中对应的列名
						String cname = mName.substring(3, mName.length());
						//打印所有 set 的方法名
//							System.out.print("从set方法名中取到的列名："+cname);
						//得到方法的参数类型
						Class[] params = m.getParameterTypes();
//							System.out.print(" : "+rs.getString(cname)+"\n");
						//根据相应的类型来给对象赋值
						if (params[0] == String.class) {
							m.invoke(obj, rs.getString(cname));
						} else if (params[0] == int.class) {
							m.invoke(obj, rs.getInt(cname));
						} else if (params[0] == long.class) {
							m.invoke(obj, rs.getLong(cname));
						} else if (params[0] == Date.class) {
							m.invoke(obj, rs.getDate(cname));
						}
					}
				}
			}
			list.add(obj);
		}catch(Exception e) {
			e.printStackTrace();
		}finally{
			db.closeAll();
		}


		return list;
	}
}