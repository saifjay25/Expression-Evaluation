package apps;

import java.io.*; 
import java.util.*;
import java.util.regex.*;

import structures.Stack;

public class Expression {

	/**
	 * Expression to be evaluated
	 */
	String expr;                
    
	/**
	 * Scalar symbols in the expression 
	 */
	ArrayList<ScalarSymbol> scalars;   
	
	/**
	 * Array symbols in the expression
	 */
	ArrayList<ArraySymbol> arrays;
    
    /**
     * String containing all delimiters (characters other than variables and constants), 
     * to be used with StringTokenizer
     */
    public static final String delims = " \t*+-/()[]";
    
    /**
     * Initializes this Expression object with an input expression. Sets all other
     * fields to null.
     * 
     * @param expr Expression
     */
    public Expression(String expr) {
        this.expr = expr;
    }

    /**
     * Populates the scalars and arrays lists with symbols for scalar and array
     * variables in the expression. For every variable, a SINGLE symbol is created and stored,
     * even if it appears more than once in the expression.
     * At this time, values for all variables are set to
     * zero - they will be loaded from a file in the loadSymbolValues method.
     */
    public void buildSymbols() {
        scalars = new ArrayList<ScalarSymbol>();
        arrays = new ArrayList<ArraySymbol>();
        StringTokenizer st = new StringTokenizer(expr,delims); // breaks the string in tokens separated by delims
        ArrayList<String> tokens = new ArrayList<String>(); 
        while(st.hasMoreTokens()){
            String token = st.nextToken();
            tokens.add(token);
        }
        for(int i=0;i<tokens.size();i++){
            int tokenindex=expr.indexOf(tokens.get(i));
            char ch=expr.charAt(tokenindex);// gets letter
            if (i==tokens.size()-1){ //checks the end of expression
            	if(Character.isLetter(ch)){
            		ScalarSymbol scalar=new ScalarSymbol(tokens.get(i));
            		if(scalars.indexOf(scalar)==-1){
            			scalars.add(scalar);
            			return;
                }
                }else if (Character.isDigit(ch)){
                	return;
                }else{
                    ArraySymbol array=new ArraySymbol(tokens.get(i));
                    if(arrays.contains(array)){
                    	return;
                        }else{
                        	arrays.add(array);
                            return;
                    }
                }
            }
            // checks to if it is an letter
            if(Character.isLetter(ch) && expr.charAt(tokenindex+tokens.get(i).length())!='['){
            	ScalarSymbol scalar = new ScalarSymbol(tokens.get(i));
                if(scalars.indexOf(scalar)==-1){
            		scalars.add(scalar);
            	}
            }
            // checks to see if an array is inside another array
            if(expr.charAt(tokenindex+tokens.get(i).length())=='['){
                ArraySymbol array=new ArraySymbol(tokens.get(i));
                if(arrays.contains(array)){
                	continue;
                }else{
                	arrays.add(array);
                }
            }
       }
  }
    
    
    /**
     * Loads values for symbols in the expression
     * 
     * @param sc Scanner for values input
     * @throws IOException If there is a problem with the input 
     */
    public void loadSymbolValues(Scanner sc) 
    throws IOException {
        while (sc.hasNextLine()) {
            StringTokenizer st = new StringTokenizer(sc.nextLine().trim());
            int numTokens = st.countTokens();
            String sym = st.nextToken();
            ScalarSymbol ssymbol = new ScalarSymbol(sym); //creates symbol to index it in scalars
            ArraySymbol asymbol = new ArraySymbol(sym);
            int ssi = scalars.indexOf(ssymbol);
            int asi = arrays.indexOf(asymbol);
            if (ssi == -1 && asi == -1) {
            	continue;
            }
            int num = Integer.parseInt(st.nextToken()); // turns second token in integer
            if (numTokens == 2) { // scalar symbol
                scalars.get(ssi).value = num; // sets the scalar at the ssi index to the int num
            } else { // array symbol
            	asymbol = arrays.get(asi);
            	asymbol.values = new int[num]; //sets the length of array
                // following are (index,val) pairs
                while (st.hasMoreTokens()) {
                    String tok = st.nextToken();
                    StringTokenizer stt = new StringTokenizer(tok," (,)"); //splits token up using comma
                    int index = Integer.parseInt(stt.nextToken());
                    int val = Integer.parseInt(stt.nextToken());
                    asymbol.values[index] = val;              
                }
            }
        }
    }
    
    
    /**
     * Evaluates the expression, using RECURSION to evaluate subexpressions and to evaluate array 
     * subscript expressions.
     * 
     * @return Result of evaluation
     */
    public float evaluate() { 
    	String expression=expr;
    	int i=0;
    	int paren=0;
    	int array=0;
    	for ( i=0;i<expression.length();i++){
    		if (expression.charAt(i)=='('){
    			paren++;
    		}
    		if(expression.charAt(i)=='['){
    			array++;
    		}
    	}
    	i=0;
    	if(paren==0 && array==0){// checks if its only letters
    		expression=changeLetters(expression);
    		expression=evaluateExpr(expression);
    	}else{
    		int f=0;
    		int second=0;
    		int third=0;
    		while (expression.indexOf('(')>=0 || expression.indexOf('[')>=0){
    			if (expression.indexOf('(')<expression.indexOf('[') && expression.indexOf('(')!=-1 || expression.indexOf('(')>=0 && expression.indexOf('[')==-1){
    				f=expression.indexOf('(')+1;
    				second=expression.indexOf('(');
    				third=expression.indexOf(')');
    				if (f!=0){ //checks only for one parenthesis so apply multiplication
    					expression=insideEval(expression,f,second,third);
    				}
    			}else{
    				f=expression.indexOf('[')+1;
    				second=expression.indexOf('[')-1;
    				third=expression.indexOf(']');
    				if (f!=0){
    					expression=insideEval(expression,f,second,third);
    				}
    			}
    			if(expression.indexOf('(')==-1 && expression.indexOf('[')==-1 && (expression.indexOf('+')>0 ||expression.indexOf('-')>0 ||expression.indexOf('/')>0|| expression.indexOf('*')>0)){
    				expression=evaluateExpr(expression);
    			}
    		}
    	}
    	
    float answer=Integer.parseInt(expression);
    return answer;
    }
    private String insideEval(String exp, int f, int second, int third){
    String inside="";
    String var="";
		while(f!=third){
			var+=exp.charAt(f);
			//checks to see if it is not monomial in parenthsis or array
			if(exp.charAt(f)=='+'||exp.charAt(f)=='-'||exp.charAt(f)=='/'||exp.charAt(f)=='*'||exp.charAt(f)=='('||exp.charAt(f)=='['){ 
				inside=subExpr(exp);
				exp=stack(exp,inside);
				while(exp.indexOf('[')>0 || exp.indexOf('(')>0){
					inside=subExpr(exp);
					if(exp.indexOf('[')>=0 || exp.indexOf('(')>=0){
						exp=stack(exp,inside);
					}
					if(exp.indexOf('[')==-1 && exp.indexOf('(')==-1){
						return exp;
					}
				}
				if (exp.indexOf('[')==-1 && exp.indexOf('(')==-1 && (exp.indexOf('+')>0 ||exp.indexOf('-')>0 ||exp.indexOf('/')>0|| exp.indexOf('*')>0)){
					return exp;
				}
			}
			f++;
		}
		if(exp.charAt(f)==']'){
			exp=findArray(var, exp, second, third);
		}
		else if(exp.charAt(f)==')'){
			exp=shortenParen(exp, second, third,1);
		}
		return exp;
	}
    private String subExpr(String exp){ //gets subexpression
    	int count=1;
    	String subexpr="";
    	if(exp.indexOf('[')<exp.indexOf('(') && exp.indexOf('[')!=-1 || exp.indexOf('[')>=0 && exp.indexOf('(')<0){
    		int i=exp.indexOf('[')+1;
    		while(i!=exp.length()){
    			if(exp.charAt(i)=='['){
    				count++;
    			}
    			if(exp.charAt(i)==']'){
    				count--;
    			}
    			if(count==0){
    				subexpr=exp.substring(exp.indexOf('['),i+1); //with brackets
    				break;
    			}
    			i++;
    		}
    	}
    	else if(exp.indexOf('(')<exp.indexOf('[') && exp.indexOf('(')!=-1 || exp.indexOf('(')>=0 && exp.indexOf('[')<0){
    		int i=exp.indexOf('(')+1;
    		while(i!=exp.length()){
    			if(exp.charAt(i)=='('){
    				count++;
    			}
    			if(exp.charAt(i)==')'){
    				count--;
    			}
    			if(count==0){
    				subexpr=exp.substring(exp.indexOf('('),i+1); //with parenthesis
    				break;
    			}
    			i++;
    		}
    	}
    	return subexpr;
    }
    private String stack(String exp,String inside){
    	String withoutparen=inside.substring(1,inside.length()-1); //inside is whole subexpr dup is inner most expression
    	int z=0;
    	String dup=inside;
    	dup=shorten(dup,withoutparen);
    	String dup4=dup;
    	while(exp.indexOf(inside)>=0){
    		String dummy="";
    		//checks inside paren that is inside paren or array
    		if(dup.indexOf('-')==-1 && dup.indexOf('+')==-1 && dup.indexOf('/')==-1 && dup.indexOf('*')==-1 && dup.indexOf(')')>0 || dup.indexOf('-')<=1 && dup.indexOf('+')==-1 && dup.indexOf('/')==-1 && dup.indexOf('*')==-1 && dup.indexOf(')')>0){
    			int i=exp.indexOf(dup)+1;
    			if(i>0){
    				String dup2=dup.substring(1,dup.length()-1);
    				String dup3=changeLetters(dup2);
    				if(dup2!=dup3){
    					exp=exp.replaceAll(dup2,dup3);
    					dup="("+dup3+")";
    					exp=shortenParen(exp,exp.indexOf(dup),exp.indexOf(dup)+dup.length()-1,0);
    				}
    			}else{
    				exp=replaceFrom(exp,dup4,dup);
    				exp=shortenParen(exp,exp.indexOf(dup),exp.indexOf(dup)+dup.length()-1,0);
    				continue;
    			}
    		}
    		else if(dup.indexOf('-')==-1 && dup.indexOf('+')==-1 && dup.indexOf('/')==-1 && dup.indexOf('*')==-1 && dup.indexOf(']')>0){
    			int i=exp.indexOf(dup)+1;
    			if(i>0){
    				int index=exp.indexOf(dup);//used for replacement in exp
    		    	int last=exp.indexOf(dup)+dup.length()-1;//used for replacement in exp
    				exp=findArray(dup,exp,index,last);
    			}else{
    				exp=replaceFrom(exp,inside,dup);
    				int index=exp.indexOf(dup);//used for replacement in exp
    		    	int last=exp.indexOf(dup)+dup.length()-1;//used for replacement in exp
    				exp=findArray(dup,exp,index,last);
    				continue;
    			}
    		}
    		for(z=dup.length()-2;z>=1;z--){ //reverses expression to put operands in stack
    			dummy+=dup.substring(z,z+1);
    		}
    		Stack <String> operand =new Stack <String>();
    		while(dummy.indexOf("*")>0 || dummy.indexOf("/")>0 || dummy.indexOf("+")>0 || dummy.indexOf("-")>0){
    			int mul=dummy.indexOf('*');
    			int div=dummy.indexOf('/');
    			int min=dummy.indexOf('-');
    			int plu=dummy.indexOf('+');
    			if(min<plu && min!=-1 && plu!=-1 || min>0 && plu==-1){
    				operand.push("-");
    				dummy=dummy.replace(dummy.charAt(min),'0');
    			}
    			else if(plu<min && min!=-1 && plu!=-1 || plu>0 && min==-1){
    				operand.push("+");
    				dummy=dummy.replace(dummy.charAt(plu),'0');
    			}
    			else if(mul<div && mul!=-1 && div!=-1 || mul>0 && div==-1){
    				operand.push("*");
    				dummy=dummy.replace(dummy.charAt(mul),'0');
    			}
    			else if(div>min && mul!=-1 && div!=-1|| mul>0 && div==-1){
    				operand.push("/");
    				dummy=dummy.replace(dummy.charAt(div),'0');
    			}
    		}
    		dup=recursion(operand,dup);
    		dup=shorten(dup,withoutparen);
    	}
    	return exp;
    }
    private String recursion(Stack <String> operand, String dup){
    	if(operand.size()==0){
    		return dup;
    	}else{
    		String operate=operand.pop();
    		int index=dup.indexOf(operate);
    		String first2=findval(index, dup);
    		String first=changeLetters(first2);
    		int fnum=Integer.parseInt(first);
        	if(first2!=first){
        		dup=dup.replace(first2,first);
        	}
    		String second=findval2(index,dup);
    		String second2=changeLetters(second);
    		int snum=Integer.parseInt(second2);
        	if(second!=second2){
        		dup=dup.replace(second,second2);
        	}
    		dup=eval(fnum,snum,operate,dup);
    		return recursion(operand,dup);
    	}
    }
    private String eval(int first,int second,String operate,String dup){
    	String together=first+operate+second;
    	String num11=String.valueOf(first);
    	if (dup.charAt(0)=='-' && dup.substring(dup.indexOf(num11)+num11.length(),dup.indexOf(num11)+num11.length()+1)==operate || first<second && operate=="-"){ //checks only negatives
    		if (operate=="*"){
    			String answer= String.valueOf(first*second);
    			dup=replaceFrom(dup,together,answer);
        		return dup;
    		}
    		if (operate=="+" && first>second){
    			String answer=String.valueOf(first-second);
    			dup=replaceFrom(dup,together,answer);
    			return dup;
    		}
    		if (operate=="/"){
    			String answer= String.valueOf(first/second);
    			dup=replaceFrom(dup,together,answer);
        		return dup;
    		}
    		if(operate=="+" &&first<second){
    			String answer=String.valueOf(second-first);
    			dup=dup.replace(dup.charAt(0),' ');
    			dup=dup.replaceAll("\\s+","");
    			dup=replaceFrom(dup,together,answer);
    			return dup;
    		}
    		if(operate=="-" && dup.charAt(0)=='-'){
    			String answer=String.valueOf(second+first);
    			dup=replaceFrom(dup,together,answer);
    			return dup;
    		}
    		if (operate=="-" && first<second){
    			String answer="-"+String.valueOf(second-first);
    			dup=replaceFrom(dup,together,answer);
    			return dup;
    		}
    	}
    	if(operate=="*"){
    		String answer= String.valueOf(first*second);
    		dup=replaceFrom(dup,together,answer);
    		return dup;
    	}
    	if(operate=="/"){
    		String answer= String.valueOf(first/second);
    		dup=replaceFrom(dup,together,answer);
    		return dup;
    	}
    	if(operate=="-"){
    		String answer= String.valueOf(first-second);
    		dup=replaceFrom(dup,together, answer);
    		return dup;
    	}
    	if(operate=="+"){
    		String answer= String.valueOf(first+second);
    		dup=replaceFrom(dup,together, answer);
    		return dup;
    	}
    	return dup;
    }
    private String findval(int index, String dup){
    	int i=index-1;
    	String first="";
    	String first2="";
    	while(Character.isLetter(dup.charAt(i)) || Character.isDigit(dup.charAt(i))){
    		first+=dup.charAt(i);
    		i--;
    		if(i<0){
    			break;
    		}
    	}
    	for(int z=first.length()-1;z>=0;z--){ //reverses expression to put operands in stack
    		first2+=first.substring(z,z+1);
		}
    	return first2;
    }
    private String findval2(int index, String dup){
    	int i=index+1;
    	String second="";
    	while(Character.isLetter(dup.charAt(i)) || Character.isDigit(dup.charAt(i))){
    		second+=dup.charAt(i);
    		i++;
    		if(i<0){
    			break;
    		}
    	}
    	return second;
    }
    private String shorten(String duplicate, String exp){ //gets innermost subexpression with only paren or array around it
    	String hold="";
    	int par=duplicate.indexOf('(');
    	int par2=duplicate.indexOf(')');
    	int arr=duplicate.indexOf('[');
    	int arr2=duplicate.indexOf(']');
    	while(par>=0 || arr>=0){
    		par=duplicate.indexOf('(');
        	par2=duplicate.indexOf(')');
        	arr=duplicate.indexOf('[');
        	arr2=duplicate.indexOf(']');
    		if(arr>par && arr<par2 && arr!=-1 && par!=-1 && par2!=-1){
    			duplicate=duplicate.substring(arr+1,arr2+1);
    			hold="[";
    			continue;
    		}
    		else if(par>arr && par<arr2 && arr!=-1 && par!=-1 && arr2!=-1){
    			duplicate=duplicate.substring(par+1,par2+1);
    			hold="(";
    			continue;
    		}
    		else if (par>=0){
    			duplicate=duplicate.substring(par+1,par2+1);
    			hold="(";
    			continue;
    		}
    		else if (arr>=0){
    			duplicate=duplicate.substring(arr+1,arr2+1);
    			hold="[";
    			continue;
    		}
    	}
    	if(hold=="["){
			duplicate=hold+duplicate;
		}
		if(hold=="("){
			duplicate=hold+duplicate;
		}
    	return duplicate;
    }
    private String shortenParen(String expression, int parin, int parin2,int num){
    	String before="";
    	String after="";
    	if(parin-1>=0){
    		before=expression.substring(parin-1, parin);
    	}
    	if(parin2+2<=expression.length()){
    		after=expression.substring(parin2+1, parin2+2);
    	}
    	int bb=expression.indexOf(before);
    	int aa=expression.indexOf(after);
    	char b='+';
    	char a='+';
    	if(bb+1>=0){
    		b=expression.charAt(bb+1);
    	}
    	if(aa+1<expression.length()-1){
    		a=expression.charAt(aa+1);
    	}
    	if (parin==0){
			expression=expression.replace(expression.substring(parin,parin+1), " ");
			expression=expression.replace(expression.substring(parin2,parin2+1), "*");
			expression=expression.replaceAll("\\s+","");
		}
		else if(parin2==expression.length()-1){
			expression=expression.replace(expression.substring(parin2,parin2+1), " ");
			expression=expression.replace(expression.substring(parin,parin+1), "*");
			expression=expression.replaceAll("\\s+","");
		}
		else if (before.equals("[") && after.equals("]") && expression.charAt(bb+1)=='(' && expression.charAt(aa-1)==')' || ((before.equals("*") ||before.equals("+")||before.equals("/")||before.equals("-") ||before.equals("(") ||before.equals("[")) && (after.equals("*") ||after.equals("+")||after.equals("/")||after.equals("-") ||after.equals(")") ||after.equals("]")))){ //[()]
    		expression=replaceFrom(expression,"("," ");
    		expression=replaceFrom(expression,")"," ");
    		expression=expression.replaceAll("\\s+","");
    	}
		else if (before.equals("(") && after.equals(")") && expression.charAt(bb+1)=='(' && expression.charAt(aa+1)==')'){ //(()) 
    		expression=replaceFrom(expression,"("," ");
    		expression=replaceFrom(expression,")"," ");
    		expression=replaceFrom(expression,before," ");
    		expression=replaceFrom(expression,after," ");
    		expression=expression.replaceAll("\\s+","");
    	}
		else if (before.equals("/")||before.equals("+")||before.equals("-")){
			expression=expression.replace(expression.substring(parin,parin+1), " ");
			expression=expression.replace(expression.substring(parin2,parin2+1), "*");
			expression=expression.replaceAll("\\s+","");
		}
		else if (after.equals("/")||after.equals("+")||after.equals("-")){
			expression=expression.replace(expression.substring(parin2,parin2+1), " ");
			expression=expression.replace(expression.substring(parin,parin+1), "*");
			expression=expression.replaceAll("\\s+","");
		}
		else if (after.equals("]")||after.equals(")") && Character.isDigit(b) || Character.isLetter(b)){
			expression=expression.replace(expression.substring(parin2,parin2+1), " ");
			expression=expression.replace(expression.substring(parin,parin+1), "*");
			expression=expression.replaceAll("\\s+","");
		}
		else if (before.equals("(")||before.equals("[") && Character.isDigit(a) || Character.isLetter(a)){
			expression=expression.replace(expression.substring(parin2,parin2+1), "*");
			expression=expression.replace(expression.substring(parin,parin+1), " ");
			expression=expression.replaceAll("\\s+","");
		}else{
			expression=expression.replace(expression.substring(parin,parin+1), "*");
			expression=expression.replace(expression.substring(parin2,parin2+1), "*");
		}
    	if(num==0){
    		return expression;
    	}
		expression=changeLetters(expression);
		expression=evaluateExpr(expression);
		return expression;
    }
    
    private String changeLetters(String expression){
    	int i=0;
    	String variable="";
    	while (i<expression.length()){
    		if (expression.charAt(i)>='a' && expression.charAt(i)<='z'){ // checks if there is letter
    			while (Character.isLetter(expression.charAt(i))||Character.isDigit(expression.charAt(i))){ // checks for scalar variable
    				variable+=expression.substring(i,i+1);
    				i++;
    				// checks if its only one variable
    				if(i==expression.length()){ //reaches end
    					expression=findScalar(variable,expression);
    					break;
    				}
    				if(!(Character.isLetter(expression.charAt(i)) && !(Character.isDigit(expression.charAt(i))))){ //reaches operator
    					expression=findScalar(variable,expression);
    					break;
    				}
    			}
    			i=0;
    			variable="";
    		}
    		else if (Character.isDigit(expression.charAt(i))){
    			i++;
    			continue;
    		}else{
    			i++;
    		}
    	}
    	return expression;
    }
   
    
    private String evaluateExpr(String expr){ //this evaluates expression with only operators and variables or digits
    	int mul=expr.indexOf('*');
		int div=expr.indexOf('/');
		int min=expr.indexOf('-');
		int plu=expr.indexOf('+');
		String express=expr;
    	while (mul!=-1 || div!=-1 || min!=-1 || plu!=-1){
    		mul=express.indexOf('*');
    		div=express.indexOf('/');
    		min=express.indexOf('-');
    		plu=express.indexOf('+');
    		if (express.charAt(0)=='-' && (mul==-1 && div==-1 && plu==-1)){
    			break;
    		}
    		if((mul<div && mul!=-1) || (mul>0 && div==-1)){
    			express=operator(express,'*');
    			continue;
    		}
    		if((div<mul && div!=-1) || (div>0 && mul==-1)){
    			express=operator(express,'/');
    			continue;
    		}
    		if((min<plu && div==-1 && mul==-1 && min!=-1 && min!=0 && plu!=-1) || (min>0 && plu==-1)){
    			express=operator (express,'-');
    			continue;
    		}
    		if((plu<min && div==-1 && mul==-1 && plu!=-1 && min!=-1) || (plu>0 && min==-1) || (plu>0 && min==0)){
    			express=operator (express,'+');
    			continue;
    		}
    	}
    	return express;
    }
    
    private String operator( String express,char character){
    	String num1="";
    	String num11="";
    	String num2="";
    	int var1=express.indexOf(character)-1;
    	int var2=express.indexOf(character)+1;
    	while(Character.isDigit(express.charAt(var1)) || Character.isLetter(express.charAt(var1))){
    		num1+=express.charAt(var1);
    		var1--;
    		if(var1==-1){
    			break;
    		}
    	}
    	if(num1!=""){
    		for(int i=num1.length()-1;i>=0;i--){
    			num11+=num1.charAt(i);
    		}
    	}
    	String num111=changeLetters(num11);
    	while(Character.isDigit(express.charAt(var2))|| Character.isLetter(express.charAt(var2))){
    		num2+=express.charAt(var2);
    		var2++;
    		if(var2==express.length()){
    			break;
    		}
    	}
    	String num22=changeLetters(num2);
    	String answer="";
    	int number1= Integer.parseInt(num111);
    	int number2= Integer.parseInt(num22);
    	String ex=num11+character+num2;
    	if (express.charAt(0)=='-' && express.charAt(express.indexOf(num11)+num11.length())==character || number1<number2 && character=='-'){ //checks only negatives
    		if (character=='*'){
    			answer= String.valueOf(number1*number2);
        		express=replaceFrom(express,ex,answer);
        		return express;
    		}
    		if (character=='+' && number1>number2){
    			answer=String.valueOf(number1-number2);
    			express=replaceFrom(express,ex,answer);
    			return express;
    		}
    		if (character=='/'){
    			answer= String.valueOf(number1/number2);
        		express=replaceFrom(express,ex,answer);
        		return express;
    		}
    		if(character=='+' &&number1<number2){
    			answer=String.valueOf(number2-number1);
    			express=express.replace(express.charAt(0),' ');
    			express=express.replaceAll("\\s+","");
    			express=replaceFrom(express,ex,answer);
    			return express;
    		}
    		if(character=='-' && express.charAt(0)=='-'){
    			answer=String.valueOf(number2+number1);
    			express=replaceFrom(express,ex,answer);
    			return express;
    		}
    		if (character=='-' && number1<number2){
    			answer="-"+String.valueOf(number2-number1);
    			express=replaceFrom(express,ex,answer);
    			return express;
    		}
    	}
    	if(character=='*'){
    		answer= String.valueOf(number1*number2);
    		express=replaceFrom(express,ex,answer);
    		return express;
    	}
    	if(character=='/'){
    		answer= String.valueOf(number1/number2);
    		express=replaceFrom(express,ex,answer);
    		return express;
    	}
    	if(character=='-'){
    		answer= String.valueOf(number1-number2);
    		express=replaceFrom(express,ex, answer);
    		return express;
    	}
    	if(character=='+'){
    		answer= String.valueOf(number1+number2);
    		express=replaceFrom(express,ex, answer);
    		return express;
    	}
    	return express;
    }
    //regex is old char in express, exp is new expression, express is old expression
    private String replaceFrom(String express, String regex, String replacement){ 
    	int i=0;
    	String exp="";
    	int index=express.indexOf(regex);
    	String prefix=express.substring(index,index+regex.length());
        prefix=prefix.replace(regex, replacement);
        while(i!=express.length()){
        	if (i>=index && i<index+regex.length()){
        		i++;
        		if(i==index+regex.length()-1){
            		exp=exp+prefix;
            	}
        		continue;
        	}
        	exp+=express.substring(i,i+1);
        	i++;
        }
        return exp;
    }
    //finds variable for a scalar symbol (even if expr has same scalar symbol)
    private String findScalar(String variable, String expression){ 
    	for(int k=0;k<scalars.size();k++){
    		if (variable.equals(scalars.get(k).name)){
				int val=scalars.get(k).value;
				String value=Integer.toString(val);
				expression=expression.replaceAll(variable, value);
				break;
			}
		}
		return expression;
	}
    //finds variable for a array symbol and name for array and replaces it c in beginning of array name arr is end of array ']'
    private String findArray(String variable, String expression, int c, int arr){ 
    	int hold=c;
    	c--;
    	if(c<0){
    		c=0;
    	}
    	String name2="";
		String name="";
		if(variable.indexOf('[')>=0){
			variable=variable.substring(1,variable.length()-1);
		}
    	while(Character.isLetter(expression.charAt(c)) || Character.isDigit(expression.charAt(c))){
			name+=expression.charAt(c);
			c--;
			if(c<0){
				break;
			}
		}
    	if(name.length()>1){
    		for (int v=name.length()-1;v<=0;v--){ //gets name of array
    			name2+=name.charAt(v);
    		}
    	}else{
    		name2=name;
    	}
    	for(int i=0;i<variable.length();i++){ // checks if there is variable inside array
    		if (Character.isLetter(variable.charAt(i))){
    			variable=changeLetters(variable);
    		}
    		if(Character.isDigit(variable.charAt(i))){
    			continue;
    		}
    	}
    	int num=Integer.parseInt(variable);
    	for(int k=0;k<arrays.size();k++){
    		if(name2.equals(arrays.get(k).name)){
    			System.out.println(arrays.get(k).values.length);
    			if (num>arrays.get(k).values.length){
    				expression=expression.replace(expression.substring(hold-name2.length(),arr+1),"0");
    				return expression;
    			}
    			int num2=arrays.get(k).values[num];
    			String word=Integer.toString(num2);
    			if (hold==0){
    				expression=expression.replace(expression.substring(hold,arr+1),word);
    				expression=expression.replaceAll("\\s+","");
    				return expression;
    			}
    			expression=expression.replace(expression.substring(hold-name2.length(),arr+1),word);
    		}
    	}
    	expression=expression.replaceAll("\\s+","");
		return expression;
	}
	

    
	/**
     * Utility method, prints the symbols in the scalars list
     */
    public void printScalars() {
        for (ScalarSymbol ss: scalars) {
            System.out.println(ss);
        }
    }
    
    /**
     * Utility method, prints the symbols in the arrays list
     */
    public void printArrays() {
    		for (ArraySymbol as: arrays) {
    			System.out.println(as);
    		}
    }

}
