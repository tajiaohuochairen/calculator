package calculator;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Stack;

public class calcul {
	private Stack<String> optr, oprd; 		//存放操作符,操作数
	public String ans;								//默认结果存放每次结果
	private Map<String, Integer> trtable;		//存运算符优先级，顺便可以判断是否支持
	private String PS1;										//每次输出前的提示字符串
	private Map<String, String> vartable;		//存运算符优先级，顺便可以判断是否支持
	private String exp;									//本次要处理的表达式
	private String basetr = "+-*/%^(),[]#";	//分割之前操作符/数
	private function func;								//计算复杂函数
	private String pwd;										//	当前工作路径
	private String operator2 = " ,(^%*/[";        //优化去掉多余的运算符
	private String operator3 = ".0123456789"; //判断字符是否代表数字
	private String separator;					//多平台下的路径分隔符
	private String LF;							//多平台下的换行符
	private boolean PS1_lock = false; 	//是否改变PS1
	
	private String deal_path(String base, String s){
		s = s.replaceAll(" ", "").replaceAll("/s", " ");	//判断字符是否代表数字
		if( s.charAt(s.length()-1) != separator.charAt(0) )	//末尾添加分隔符
			s += separator;
		if( s.indexOf(":") > 0 )
			return s.substring(0, s.length()-1);
		if( s.charAt(0) == separator.charAt(0) )
			return s;
		String tmp = base;	
		while( s.indexOf(separator) > 0 ){
			switch( s.substring(0, s.indexOf(separator)) ){
			case ".":
				break;
			case "..":
				if( tmp.lastIndexOf(separator) > 0 )
					tmp = tmp.substring(0, tmp.lastIndexOf(separator));
				else if( tmp.lastIndexOf(separator) == 0 )
					tmp = tmp.substring(0, 1);
				break;
			default:
				if( s.substring(0, s.indexOf(separator)).matches(".*\\.{2,}.*") ) // 2个以上.不处理
					break;
				tmp += separator + s.substring(0, s.indexOf(separator));
				break;
			}
			s = s.substring(s.indexOf(separator)+1);
		}
		return tmp;
	}
	
	private boolean isExist(String path){
		File f = new File(path);
		if( f.exists() )
			return true;
		return false;
	}
	calcul(){
		trtable = new  HashMap<String, Integer>();		//规定运算优先级0最低
		trtable.put("#", 0); 
		trtable.put("+", 1);
		trtable.put("-", 1); 
		trtable.put("*", 2);	
		trtable.put("/", 2); 
		trtable.put("^", 3);	
		trtable.put("%", 3);	
		trtable.put("mod", 3);
		trtable.put("sin", 3);
		trtable.put("cos", 3);
		trtable.put("tan", 3);
		trtable.put("arcsin", 3);
		trtable.put("arccos", 3);
		trtable.put("arctan", 3);
		trtable.put("sinh", 3);
		trtable.put("cosh", 3);
		trtable.put("tanh", 3);
		trtable.put("log", 3);
		trtable.put("lg", 3);
		trtable.put("ln", 3);
		trtable.put("pow", 3);
		trtable.put("exp", 3);
		trtable.put("fact", 3);
		trtable.put("sqrt", 3);
		trtable.put("cuberoot", 3);
		trtable.put("root", 3);
		trtable.put("avg", 3);
		trtable.put("sum", 3);
		trtable.put("var", 3);
		trtable.put("varp", 3);
		trtable.put("stdev", 3);
		trtable.put("stdevp", 3);	
		
		func = new function();		
		
		if( System.getProperty("os.name").charAt(0) == 'W' ){
			separator = "\\";
			LF = "\r\n";
		}
		else{
			separator = "/";
			LF = "\n";
		}
		init();
	}
	public void println(String str){
		System.out.print(PS1 + str);
	}
	public void init(){					//重新初始化环境
		oprd = new Stack<String>();
		optr = new Stack<String>();

		ans = "0";
		
		pwd  = System.getProperty("user.dir");		//重新初始化环境
		PS1 = pwd.substring( pwd.lastIndexOf(separator) + 1 );
		PS1 = "[ " + PS1 + " ] ";
		
		vartable = new  HashMap<String, String>();		//申请存放变量表
		vartable.put("ans", ans);		
		Double tmp = new Double(Math.PI);
		vartable.put("pi", tmp.toString());	
		Double tmp1 = new Double(Math.E);
		vartable.put("e", tmp1.toString());			
	}
	private String vareplace(String tmp){
		Iterator<Entry<String, String>> it;
		Entry<String, String> entry;
		int i, len;
		
		//防止越界
		tmp = "     " + tmp + "     ";
		it = vartable.entrySet().iterator();		//遍历变量表
		while(it.hasNext()){
			entry = (Entry<String, String>)it.next();
			for( i = tmp.indexOf(entry.getKey()); i > 0; i = tmp.indexOf(entry.getKey()) ){
				len  = entry.getKey().length();
				if( ( (tmp.charAt(i-1) < 'a' || tmp.charAt(i-1) > 'z') && (tmp.charAt(i+len) < 'a' || tmp.charAt(i+len) > 'z') ) || 
						tmp.substring(i+len, i+len+4).equals("root") )
					tmp = tmp.substring(0, i) + entry.getValue() + tmp.substring(i+len);
				else{
					if( it.hasNext() )
						entry = (Entry<String, String>)it.next();
					else
						break;
				}
			}
		} 
		tmp = tmp.trim();		//还原加的空格
		return tmp;	
	}
	
	private boolean preprocess() {			//预处理 命令
		String tmp = exp.trim();
		tmp = tmp.substring(0, tmp.length()-1);
		tmp = tmp.replaceAll(" {2,}", " ");	//去掉多余空格
		boolean ret = false;
		Iterator<Entry<String, String>> it;
		Entry<String, String> entry;
	
		if( tmp.indexOf("=") != tmp.lastIndexOf("=") ){
			System.out.println("=号超过2个 !!");
			return true;
		}
		
		if( tmp.indexOf("=") > 0 ){
			tmp = tmp.replaceAll(" ", "");	//=号表达式 不需要空格
			ret = true;
			String var = "", varexp = "";
			int count = 0;
			boolean f_print = true;
			for(String i : tmp.split("=") ){
				if( count == 2 )
					break;
				if( count == 1 )
					varexp = i;
				else
					var = i;
				++count;
			}
			if( var.equals("") || varexp.equals("") )
				System.out.println("=号使用错误");
			else{
				if( trtable.get(var) != null || var.equals("ans") || var.matches(".*\\d+.*") )
					System.out.println("变量名不可用 !!");
				else{
					if( varexp.indexOf(";") > 0 )
						f_print = false;
					if( !f_print )
						varexp = varexp.substring(0, varexp.length()-1 );
					exp = vareplace(varexp) + "#";
					if( exp.equals("null#") )
						return true;
					optimize();
					if( iscorrect() ) {
						varexp = cal();
						vartable.put(var, varexp);
						if( f_print )
							System.out.println( var + " = " + varexp );
					}else
						return true;
				}
			}	
		}
		if( tmp.length() >= 2 ){
			switch( tmp.substring(0,2) ){
			case "ls":
				ret = true;
				File f = new File(pwd + separator);
				File list[] = f.listFiles();
				int i;
				for(i = 0; i < list.length; ){
					System.out.printf("%-16s", list[i].getName());
					if( ++i%4 == 0 )
						System.out.print("\n");
				}
				if( i%4 != 0)
					System.out.print("\n");
				break;
			case "cd":
				ret = true;
				if( tmp.charAt(2) != ' ' || tmp.length() < 4 ){
					System.out.println("cd命令参数错误 !! ");
					return true;
				}
				String s = tmp.substring(3);
				String tpwd = deal_path(pwd, s);		
				if( !isExist(tpwd) ){
					System.out.println("目录不存在 !!");
					break;
				}
				pwd = tpwd;
				if( !PS1_lock ){
					if( !pwd.substring( pwd.lastIndexOf(separator)+1).equals("") ){
						PS1 = pwd.substring( pwd.lastIndexOf(separator) + 1 );
						PS1 = "[ " + PS1 + " ] ";
					}
					else{
						PS1 = pwd.substring(0, 1);
						PS1 = "[ " + PS1 + " ] ";
					}
				}
				break;
			case "rm":
				ret = true;
				if( tmp.charAt(2) != ' ' || tmp.length() < 4 ){
					System.out.println("rm命令需要参数 !!");
					return true;
				}
				String[] vararr = tmp.substring(3).split(" ");
				for(i = 0; i < vararr.length; ++i){	
					String dpath = deal_path(pwd, vararr[i]);
					File df = new File(dpath);
					if( !df.exists() ){
						System.out.println("文件不存在 !!");
						break;
					}
					if( df.isDirectory() || !df.isFile() ){
						System.out.println("不能删除目录 !!");
						break;
					}
					if( !df.delete() ){
						System.out.println("删除文件错误 !!");
						break;
					}
				}
				break;
			}
		}
		if( tmp.length() >= 7 && tmp.substring(0, 3).equals("ps1") ){
			PS1( tmp.substring( 5, tmp.length()-2 ) );
			ret = true;
		}
		if( tmp.length() >= 4 ){
			switch(  tmp.substring(0, 4) ){
			case "show":
				ret = true;
				it = vartable.entrySet().iterator();
				System.out.println("变量表:");
				while(it.hasNext()){
					entry = (Entry<String, String>)it.next();
					System.out.println(entry.toString());
				} 
				break;
			case "save":
				ret = true;
				String[] vararr = tmp.split(" ");		//空格分割参数
				if( vararr.length < 3 ){
					System.out.println("save参数不全 !!");
					break;
				}
				String spath = vararr[1].replaceAll("/s", " ");
				String var = tmp.substring(4+1+spath.length()+1).replaceAll("/s", " ");
				save(spath, var);
				break;
			case "load":
				ret = true;
				if(tmp.length() < 6){
					System.out.println("load需要参数 !!");
					break;
				}
				String lpath = tmp.split(" ")[1].replaceAll("/s", " ");
				load(lpath);
				break;
			}
		}		
		if( !ret )
			exp  = vareplace(tmp);
		if( exp == null)
			return true;
		exp = exp + "#";
		
		return ret;
	}

	boolean iscorrect() {				//检测表达式
		exp = " " + exp + " ";
		if( exp.matches(".*\\.\\w+\\..*") ){
			System.out.println(".号错误 !!");
			return false;
		}
		if( exp.matches(".*[^\\d]+\\.[^\\d]+.*") ){
			System.out.println(".号错误 !!");
			return false;
		}
		if( exp.matches(".*[\\(\\[]+[\\)\\]]+.*") ){
			System.out.println("参数不能为空 !!");
			return false;
		}
		if( exp.matches(".*[a-zA-Z]+.*") && !exp.matches(".*[a-zA-Z]+\\(.*\\).*") ){
			System.out.println("函数参数或者名错误 !!");
			return false;
		}		
		if( exp.matches("[^\\(]+,[^\\)]+") ){
			System.out.println(",号错误 !!");
			return false;
		}	
		if( exp.matches(".*[^\\)\\d]+[+-/%^\\*]+.*") ){
			System.out.println("操作数错误 !!");
			return false;
		}		
		if( exp.matches(".*[+-/%^\\*]+[^a-zA-Z\\d\\(]+.*") ){
			System.out.println("操作数错误 !!");
			return false;
		}
		if( exp.indexOf("\\") > 0 || exp.matches(".*[`~!@$&'{}|]+.*") ){
			System.out.println("有非法字符 !!");
			return false;
		}	
		if(exp.matches(".*[^=].*;# +")){
			System.out.println("有非法字符！！");
			return false;
		}
		exp = exp.trim();
		Iterator<Entry<String, Integer>> it = trtable.entrySet().iterator();
		String etmp = exp;										//下面发现系统间有差异 WINDOWS不需要下面3个
		etmp = etmp.replaceAll("cuberoot", "");			//先替换ROOT导致错误 应该先替换cuberoot
		etmp = etmp.replaceAll("stdevp", "");			//先替换stdev导致错误 应该先替换stdevp
		etmp = etmp.replaceAll("varp", "");			//先替换var导致错误 应该先替换varp
		etmp = etmp.replaceAll("arcsin", "");			//先替换sin导致错误 应该先替换arcsin
		etmp = etmp.replaceAll("arccos", "");			//先替换cos导致错误 应该先替换arccos
		etmp = etmp.replaceAll("arctan", "");			//先替换tan导致错误 应该先替换arctan
		while(it.hasNext()){											//替换所有支持的函数，如果还有字母则有错
			Entry<String, Integer> entry = (Entry<String, Integer>)it.next();
			if( ((String)entry.getKey()).length() > 1 )
				etmp = etmp.replaceAll((String)entry.getKey(), "");
		}
		if( etmp.matches(".*[a-zA-Z]+.*") ){
			System.out.println("函数参数或者名错误 !!");
			return false;
		}
		int checknumber = 0;		//检测小括号
		int checknumber1 = 0; 	//检测中括号
		for(int i = 0; i < exp.lastIndexOf("#"); ++i) {
			switch( exp.charAt(i) ){
			case '(':
				++checknumber;
				break;
			case ')':
				--checknumber;
				break;
			case '[':
				++checknumber1;
				break;
			case ']':
				--checknumber1;
				break;
			}
			if( checknumber < 0 || checknumber1 < 0 || checknumber < checknumber1 ){
				System.out.println("括号匹配错误 !!");
				return false;				
			}
		}
		if( checknumber + checknumber1 != 0 )	{
			System.out.println("括号匹配错误 !!");
			return false;
		}		
		etmp = "     " + exp + "     ";
		boolean iscount = false;		//判断是否是统计函数，用来跳过其,号
		int j;
		for(int i = 4; i < etmp.lastIndexOf("#"); ++i) {	//同时完成统计函数和参数变量检测
			String tmp = etmp.substring(i, i+3);
			if( iscount ){
				switch( etmp.charAt(i) ){
				case '(':
				case '[':
					++checknumber;
					break;
				case ')':
				case ']':
					if ( --checknumber == 0 ){
						iscount = false;
						j = i;
						while( etmp.charAt(j) == ')' )
							--j;
						if( etmp.charAt(j) != ']' ){
							System.out.println("统计函数需要向量参数 !!");
							return false;
						}
					}
					break;
				}
			}
			if( tmp.equals("avg") || tmp.equals("sum") || tmp.equals("var") || tmp.equals("std") ){
				iscount = true;
				i += 2;
				j = i + 1;
				while( (etmp.charAt(j) >='a' && etmp.charAt(j) <= 'z') || etmp.charAt(j) == '(' )
					++j;
				if( etmp.charAt(j) != '[' ){
					System.out.println("统计函数需要向量参数 !!");
					return false;
				}
			}
			if( !iscount ){
				if( tmp.equals("mod") || tmp.equals("pow") || (tmp.equals("roo") && etmp.charAt(i-1) != 'e' ) ){
					++checknumber;
					i += 2;
				}
				if( etmp.charAt(i) == ',' )
					--checknumber;
			}
		}				
		if( checknumber != 0 ){
			System.out.println("函数参数个数错误 !!");
			return false;
		}
		
		return true;
	}
	void optimize() { 			//优化表达式
		exp = exp.toLowerCase();
		exp = exp.replaceAll(" " , ""); 	//去空格
		exp = "     " + exp + "     ";	//防止越界
		for(int i = 4; i < exp.lastIndexOf("#"); ++i ) {  //.1 -> 0.1
			if( exp.charAt(i+1) == '.' && operator3.indexOf(exp.substring(i, i+1)) < 0 ){
				exp = exp.substring(0, i + 1) + "0" + exp.substring(i + 1);	
				continue;
			}	//	mod -> %
			if( exp.substring(i, i + 3).equals("mod") ) {	//分别两种情况  直接跟数字和有括号，右括号要判断是不是函数mod
				if( exp.charAt(i + 3) != '(' ){
					exp = exp.substring(0, i) + "%" + exp.substring(i+3);
					continue;
				}else{
					int j = i+4;
					int sum = 1;
					boolean notfunc = true;
					while( sum > 0 && j < exp.length() ){
						switch( exp.charAt(j) ){
						case '(':
							++sum;
							break;
						case ')':
							--sum;
							break;
						case ',':
							if( sum == 1 )
								notfunc = false;
							break;
						}
						++j;
					}				
					if( notfunc ){
						exp = exp.substring(0, i) + "%" + exp.substring(i+3);
						continue;
					}
				}
			}	//	log10 -> lg
			if(exp.substring(i, i + 5).equals("log10")) {
				exp = exp.substring(0, i) + "lg" + exp.substring(i+5);
				continue;
			}
			//	1(2) -> 1*(2)
			if(exp.charAt(i+1) == '(' && operator3.indexOf(exp.substring(i, i+1)) > 0) {
				exp = exp.substring(0, i+1) + "*" + exp.substring(i+1);
				continue;
			}
			//	)( -> )*(
			if(exp.charAt(i) == ')' && exp.charAt(i+1) == '(') {
				exp = exp.substring(0, i+1) + "*" + exp.substring(i+1);
				continue;
			}	//	 )9 -> )*9
			if(exp.charAt(i) == ')' && operator3.indexOf(exp.substring(i+1, i+2)) > 0) {
				exp = exp.substring(0, i+1) + "*" + exp.substring(i+1);
				continue;
			}	//	)sin(1) -> )*sin(1)
			if(exp.charAt(i-1) == ')' && exp.charAt(i) >= 'a' && exp.charAt(i) <= 'z') {
				exp = exp.substring(0, i) + "*" + exp.substring(i);
				continue;
			} 	//	2sin -> 2*sin
			if( (exp.charAt(i) >= 'a' && exp.charAt(i) <= 'z') && operator3.indexOf(exp.substring(i-1, i)) > 0 ) {				
				exp = exp.substring(0, i) + "*" + exp.substring(i);
				--i;
				continue;
			}	//++ -> +
			if( exp.substring(i,i+2).equals("++") ) {
				exp = exp.substring(0, i+1) + exp.substring(i+2);
				continue;
			}	//+- -> -
			if( exp.substring(i,i+2).equals("+-") ) { 
				exp = exp.substring(0, i) + exp.substring(i+1);
				continue;
			} 	// -- -> +
			if( exp.substring(i,i+2).equals("--") ) { 
				exp = exp.substring(0, i) + "+" + exp.substring(i+2);
				continue;
			}	// -+ -> -
			if( exp.substring(i,i+2).equals("-+") ) {
				exp = exp.substring(0, i+1) + exp.substring(i+2);
				continue;
			}	//负数换成0-
			if(operator2.indexOf(exp.substring(i, i+1)) >= 0) {
				if((exp.charAt(i+1) == '+' || exp.charAt(i+1) == '-') && operator3.indexOf(exp.substring(i+2, i+3)) >= 0) {
					int k;
					for(k = i+3; operator3.indexOf(exp.substring(k, k+1)) >= 0; k++);
					exp = exp.substring(0, i+1) + "(0" + exp.substring(i+1, k) + ")" + exp.substring(k);
					continue;
				}
				if((exp.charAt(i+1) == '+' || exp.charAt(i+1) == '-' ) && 
						(exp.charAt(i+2) == '(' || (exp.charAt(i+2) >= 'a' && exp.charAt(i+2) <= 'z'))) {
					int p;
					int n = 1;
					for(p = i+2; exp.charAt(p) != '('; p++);
					for(p += 1 ; n != 0 && p < exp.lastIndexOf("#"); p++) {
						if(exp.charAt(p) == '(')
							n++;
						else if(exp.charAt(p) == ')')
							n--;
					}
					exp = exp.substring(0, i+1) + "(0" + exp.substring(i+1, p) + ")" + exp.substring(p);
					continue;
				}
			}	//逗号没数字补0
			if(exp.charAt(i) == ',') {
				if(exp.charAt(i-1) == '[' || exp.charAt(i-1) == '(') {
					exp = exp.substring(0, i) + "0" + exp.substring(i);
					continue;
				}
				else if(exp.charAt(i+1) == ']' || exp.charAt(i+1) == ')' || exp.charAt(i+1) == ',') {
					exp = exp.substring(0, i+1) + "0" + exp.substring(i+1);
					continue;
				}
			}	//yroot -> root(y,x)
			if(exp.charAt(i) == '*' && exp.charAt(i+1) == 'r') {
				if(exp.substring(i+1, i+5).equals("root")) {
					if(exp.charAt(i-1) == ')') {
						int n = 1;
						int p;
						for(p = i-1; n != 0 && exp.charAt(p) != ' '; p--) {
							if(exp.charAt(p) == ')')
								n++;
							else if(exp.charAt(p) == '(')
								n--;
						}
						if(exp.charAt(p) < 'a' || exp.charAt(p) > 'z') {
							exp = exp.substring(0, p+1) + exp.substring(i+1, i+6) + exp.substring(p+1, i) + "," + exp.substring(i+6);
							continue;
						}
					}
					else if(operator3.indexOf(exp.substring(i-1, i)) >= 0) {
						int k;
						for(k = i-1; operator3.indexOf(exp.substring(k-1, k)) >= 0 && exp.charAt(k) != ' '; k--);
						exp = exp.substring(0, k) + exp.substring(i+1, i+6) + exp.substring(k, i) + "," + exp.substring(i+6);
					}
				}
			}
		}
		exp = exp.trim();
	}
	
	public String process(String tmp){		//处理表达式返回值
		
		exp = tmp + "#";
		if( preprocess() )
			return null;
		
		optimize();		
		if( !iscorrect() ) 
			return null;

		ans = cal();
		vartable.put("ans", ans);
		return ans;
	}
	
	private String cal(){			//计算表达式返回值
		String var1 = "", var2 = "";
		String ctop;
		String c;
		int varsum = 1;
		
		oprd.clear();
		optr.clear();
		optr.push("#");			
		
		c = GetNextTr();
		while( !c.equals("#") || !optr.peek().equals("#") ){
			ctop = optr.peek();
			switch ( compare( ctop, c ) ) {
			case '<':
				optr.push(c);
				c = GetNextTr();
				break;
			case '=':
				if( optr.peek().equals(",") ){
					++varsum;
					optr.pop();
					break;
				}
				optr.pop();
				c = GetNextTr();
				break;
			case '>':
				ctop = optr.pop();
				if (ctop.length() > 1) {	//处理普通运算符和函数	
					if( varsum > 2) {
						int i = varsum;
						while( i-- > 0 )
							var1 = oprd.pop() + "," + var1;	//拼接统计函数参数
						ans = func.cal(ctop, var1, varsum);
						varsum = 1;
					} else if (varsum == 2) {
						var2 = oprd.pop();
						var1 = oprd.pop();
						ans = cal(ctop, var1, var2);
					} else {
						var1 = oprd.pop();
						ans = func.cal(ctop, var1);		
					}
					varsum = 1;
				} else {
					var2 = oprd.pop();
					var1 = oprd.pop();
					ans = cal(ctop, var1, var2);
				}
				if( ans == null )
					return null;
				var1 = "";
				var2 = "";
				oprd.push(ans);
				break;
			}
		}
		ans = oprd.pop();
		return ans;
	}
	private char compare(String top, String tr){							//简单的除法判断
		char c = top.charAt(0);
		char c1 = tr.charAt(0);

		if( c1 == ',' ){			//,()[]单独处理
			if( c == '(' || c == '[' || c == ',' )
				return '<';
			return '>';
		}
		if( c == ',' ){		
			if( c1 == ']' || c1 == ')' )
				return '=';
			return '<';
		}
		else if ( c == '(' ){
			if( c1 == ')' )
				return '=';
			return '<';
		}
		else if( c == ')' )
			return '>';
		if( c1 == '(' )
			return '<';
		else if ( c1 == ')' ){
			if( c == '(' )
				return '=';
			return '>';
		}
		if ( c == '[' ){
			if( c1 == ']' )
				return '=';
			return '<';
		}
		else if( c == ']' )
			return '>';
		if( c1 == '[' )
			return '<';	
		else if ( c1 == ']' ){
			if( c == '[' )
				return '=';
			return '>';
		}
		
		int i1 = trtable.get(top);
		int i2 = trtable.get(tr);
		if( i1 == i2){
			if( c == '#' )
				return '=';
			return '>';
		}
		else
			return i1 > i2?'>':'<';
	}
	private String cal(String tr, String rd1,  String rd2){		//计算双目运算符,简单的本函数处理，复杂的交给function类
		BigDecimal brd1 = new BigDecimal(rd1);
		BigDecimal brd2 = new BigDecimal(rd2);		

		if( tr.length() == 1 ){
			switch( tr ){
			case "+":
				ans = brd1.add(brd2).toString();
				break;
			case "-":
				ans = brd1.add(brd2.negate()).toString();
				break;
			case "*":
				ans = brd1.multiply(brd2).toString();
				break;
			case "/":
				Double i = new Double(rd2);	//简单的除法判断
				if( i.doubleValue() - 0 <= 0.00000001 ){
					System.out.println("除数不可为0 !!");
					return null;
				}
				ans = brd1.divide(brd2, 10, RoundingMode.DOWN).toString();
				break;
			case "%":
				if( brd2.doubleValue() - 0 <= 0.00000001 ){
					System.out.println("不能对0取余 !!");
					return null;
				}
				ans = brd1.remainder(brd2).toString();
				break;
			case "^":
				if( rd1.indexOf("-") >= 0 && rd2.indexOf(".") > 0 ){
					System.out.println("负数不能有小数次方 !!");
					return null;
				}
				try{
					if( brd2.intValue() < 0){
						BigDecimal one = new BigDecimal("1");
						brd1 = brd1.pow( Math.abs(brd2.intValue()) );
						brd1 = one.divide( brd1, 10, RoundingMode.DOWN );
					}
					else
						brd1 = brd1.pow( brd2.intValue() );			
					ans = brd1.toString();
				}catch(ArithmeticException e){
					System.out.println("指数过大 !!");
					ans = null;
				}
				break;
			}
		}else 
			ans = func.cal(tr, rd1, rd2);	//2参数函数
		return ans;
	}
	private String GetNextTr(){
		int i = 0;
		String c;
		String ret = "";

		while( true ){
			c = exp.substring(i, i+1);
			if( basetr.indexOf(c) >= 0 ){		//通过basetr来取操作符、操作数
				if( i > 0 ){
					if( exp.charAt( i-1 ) >= 'a' )
						ret = exp.substring(0, i);
					else{
						oprd.push( exp.substring(0, i) );
						ret = c;
						++i;
					}
				}else{
					ret = c;
					++i;
				}
				exp = exp.substring(i);
				i = 0;		
				if( !ret.equals("") )
					break;
			}else
				++i;
		}			
		return ret;
	}
	public void PS1(String format){			//改变CONSOLE 每次命令前的提示（比如 "[root /]# "）
		PS1 = format + " ";
		PS1_lock = true;
	}
	public void save(String file, String var){			//提供保存变量到文件 
		String path = deal_path(pwd, file);
		try {
			FileWriter fw = new FileWriter(path);
			
			int i = 0;
			String[] tmp = var.split(" ");
			while(i < tmp.length){
				fw.write( tmp[i] + " " + vartable.get(tmp[i]) + LF );
				++i;
			}
			fw.close();
		} catch (IOException e) {
			System.out.println("写文件错误 !!");
		}
	}
	public void load(String path) {				//加载文件变量
		path = deal_path(pwd, path);
		try {
			FileReader fr = new FileReader(path);
			BufferedReader br = new BufferedReader(fr);
			String tmp;
			while( (tmp=br.readLine()) != null )
				vartable.put(tmp.split(" ")[0], tmp.split(" ")[1]);
			br.close();
			fr.close();
		} catch (FileNotFoundException e) {
			System.out.println("读文件错误 !!");
		} catch (IOException e) {
			System.out.println("读文件错误 !!");		
		}
	}
}
