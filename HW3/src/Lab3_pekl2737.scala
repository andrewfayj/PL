object Lab3_pekl2737 {
  import jsy.lab3.ast._
  
  /*
   * CSCI 3155: Lab 3 
   * Peter Klipfel
   * 
   * Partner: Henry Broddle
   * Collaborators: Dana Hughes, 
   * 				Waverly Hinton, 
   * 				John Shaw, 
   * 				Andrew Fischer, 
   * 				Greg Guyles
   */

  /*
   * Fill in the appropriate portions above by replacing things delimited
   * by '<'... '>'.
   * 
   * Replace 'YourIdentiKey' in the object name above with your IdentiKey.
   * 
   * Replace the 'throw new UnsupportedOperationException' expression with
   * your code in each function.
   * 
   * Do not make other modifications to this template, such as
   * - adding "extends App" or "extends Application" to your Lab object,
   * - adding a "main" method, and
   * - leaving any failing asserts.
   * 
   * Your lab will not be graded if it does not compile.
   * 
   * This template compiles without error. Before you submit comment out any
   * code that does not compile or causes a failing assert.  Simply put in a
   * 'throws new UnsupportedOperationException' as needed to get something
   * that compiles without error.
   */
  case class customException(smth:String)  extends Exception

  type Env = Map[String, Expr]
  val emp: Env = Map()
  def get(env: Env, x: String): Expr = env(x)
  def extend(env: Env, x: String, v: Expr): Env = {
    require(isValue(v))
    env + (x -> v)
  }
  
  def toNumber(v: Expr): Double = {
    require(isValue(v))
    (v: @unchecked) match {
      case N(n) => n
      case B(false) => 0
      case B(true) => 1
      case Undefined => 0
      case S(s) => try s.toDouble catch { case _ => Double.NaN }
      case Function(_, _, _) => Double.NaN
    }
  }
  
  def toBoolean(v: Expr): Boolean = {
    require(isValue(v))
    (v: @unchecked) match {
      case N(n) if ((n compare 0.0) == 0 || (n compare -0.0) == 0 || n.isNaN()) => false
      case N(_) => true
      case B(b) => b
      case Undefined => false
      case S("") => false
      case S(_) => true
      case Function(_, _, _) => true
    }
  }
  
  def toString(v: Expr): String = {
    require(isValue(v))
    (v: @unchecked) match {
      case N(n) => n.toString
      case B(b) => b.toString
      case Undefined => "undefined"
      case S(s) => s
      case Function(_, _, _) => "function"
    }
  }
  
  /* Big-Step Interpreter with Dynamic Scoping */
  
  /*
   * This code is a reference implementation of JavaScripty without
   * strings and functions (i.e., Lab 2).  You are to welcome to
   * replace it with your code from Lab 2.
   */
  def eval(env: Env, e: Expr): Expr = {
    def eToN(e: Expr): Double = toNumber(eval(env, e))
    def eToB(e: Expr): Boolean = toBoolean(eval(env, e))
    def eToS(e: Expr): String = toString(eval(env,e))
    def eToVal(e: Expr): Expr = eval(env, e)
    e match {
      /* Base Cases */
      case _ if (isValue(e)) => e
      case Var(x) => get(env, x)
      
      /* Inductive Cases */
      case Print(e1) => println(pretty(eval(env, e1))); Undefined
      
      case Unary(Neg, e1) => N(- eToN(e1))
      case Unary(Not, e1) => B(! eToB(e1))
      
      case Binary(Plus, e1, e2) => (eval(env, e1), eval(env, e2)) match {
        case (N(e1), N(e2)) => N(e1 + e2)
        case (S(e1), _)     => S(e1 + eToS(e2))
        case (_, S(e2))     => S(eToS(e1) + e2)
        case _ => N(eToN(e1) + eToN(e2))
      }
//      case Binary(Plus, e1, e2) => {
//        (eval(e1), eval(e2)) match => {
//          case ()
//        }
//      }
        
      case Binary(Minus, e1, e2) => N(eToN(e1) - eToN(e2))
      case Binary(Times, e1, e2) => N(eToN(e1) * eToN(e2))
      case Binary(Div, e1, e2) => N(eToN(e1) / eToN(e2))
      
      case Binary(Eq, e1, e2) => B(eToVal(e1) == eToVal(e2))
      case Binary(Ne, e1, e2) => B(eToVal(e1) != eToVal(e2))
      
      case Binary(Lt, e1, e2) => B(eToN(e1) < eToN(e2))
      case Binary(Le, e1, e2) => B(eToN(e1) <= eToN(e2))
      case Binary(Gt, e1, e2) => B(eToN(e1) > eToN(e2))
      case Binary(Ge, e1, e2) => B(eToN(e1) >= eToN(e2))
      
      case Binary(And, e1, e2) => if (eToB(e1)) eToVal(e2) else B(false)
      case Binary(Or, e1, e2) => if (eToB(e1)) B(true) else eToVal(e2)
      
      case Binary(Seq, e1, e2) => eToVal(e1); eToVal(e2)
      
      case If(e1, e2, e3) => if (eToB(e1)) eToVal(e2) else eToVal(e3)
      
      case ConstDecl(x, e1, e2) => eval(extend(env, x, eToVal(e1)), e2)
      
      case Call(e1, e2) => eval(env, e1) match {
      	case Function(None, x, eprime) => {  // non-recursive case
      	  val v1 = eval(env, e2)
      	  val env1 = extend(env, x, v1)
      	  eval(env1, eprime)
      	}
		case v1 @ Function(Some(x1), x2, eprime) => {
			val v2 = eval(env, e2)
			val env1 = extend(env, x1, v1)
			val env2 = extend(env1, x2, v2)
			eval(env2, eprime)
		}
		case _ => throw new DynamicTypeError(e)
      }
      
      case _ => throw new UnsupportedOperationException
    }
  }
    
  def evaluate(e: Expr): Expr = eval(emp, e)
  
  
  /* Small-Step Interpreter with Static Scoping */
  
  def substitute(e: Expr, v: Expr, x: String): Expr = {
    require(isValue(v))
    /* Simple helper that calls substitute on an expression
     * with the input value v and variable name x. */
    def subst(e: Expr): Expr = substitute(e, v, x)
    /* Body */
    e match {
      case N(_) | B(_) | Undefined | S(_) => e
      case Print(e1) => Print(subst(e1))
      case Unary(uop, e1) => Unary(uop, substitute(e1, v, x))
      case Binary(op, e1, e2) => Binary(op, substitute(e1, v, x), substitute(e2, v, x)) 
      case Var(y) => if(x == y) v else e
      case ConstDecl(y,e1, e2) => ConstDecl(y, subst(e1), if (x == y) e2 else subst(e2))
      case Call(e1, e2) => Call(subst(e1), subst(e2))
      case If(e1, e2, e3) => If(subst(e1), subst(e2), subst(e3))
      case _ => throw new UnsupportedOperationException
    }
  }
    
  def step(e: Expr): Expr = {
    require(!isValue(e))
    e match {
      /* Base Cases: Do Rules */
      case Print(v1) if (isValue(v1)) => println(pretty(v1)); Undefined
      
      case Unary(Neg, v1) => N(-toNumber(v1)) // Do Negative
      case Unary(Not, v1) => B(!toBoolean(v1)) // Do Not
      
      case Binary(Plus, e1, e2) if(isValue(e1) && isValue(e2)) => (e1, e2) match {
        case (S(e1), _) => S(e1+toString(e2))
        case (_, S(e2)) => S(toString(e1) + e2)
        case (N(e1), N(e2)) => N(e1 + e2)
        case _ => N(toNumber(e1)+toNumber(e2))
      }
      case Binary(And,v1, e2)   if(isValue(v1) && (v1 == B(true))) => e2
      case Binary(And,v1, e2)   if(isValue(v1) && (v1 == B(false)))=> B(false)
      case Binary(Or, v1, e2)   if(isValue(e2) && (v1 == B(false)))=> e2
      case Binary(Or, v1, e2)   if(isValue(e2) && (v1 == B(true))) => B(true)
      case Binary(Seq,e1, e2)   if(isValue(e1)) => e2
      
      case Binary(op, e1, e2) if(isValue(e1) && isValue(e2)) => op match {
        /*     doArith        */
        case Minus => N(toNumber(e1) - toNumber(e2))
        case Times => N(toNumber(e1) * toNumber(e2))
        case Div   => N(toNumber(e1) / toNumber(e2))
        /*    DoEquality      */
        case Eq    => B(e1 == e2)
        case Ne    => B(e1 != e2)
        /*    DoInequality    */
        case Ge    => B(toNumber(e1) >= toNumber(e2))
        case Gt    => B(toNumber(e1) >  toNumber(e2))
        case Le    => B(toNumber(e1) <= toNumber(e2))
        case lt    => B(toNumber(e1) <  toNumber(e2))        
      }
      
      case If(e1, e2, e3) if(isValue(e1) && B(toBoolean(e1))==B(true)) => e2
      case If(e1, e2, e3) if(isValue(e1) && B(toBoolean(e1))==B(false)) => e3
      case ConstDecl(x, v1, e2) if(isValue(v1)) => substitute(e2, v1, x) // substitute v1 for x into e2

      case Call(e1, e2) if(isValue(e2)) => e1 match {
        case Function(None, x, ebody) => substitute(ebody, e2, x)
        case Function(Some(fun), x, ebody) => {
          val v3body = substitute(ebody, e2, x)
          substitute(v3body, Function(Some(fun), x, ebody), fun)
        }

        case _ => throw new customException("This is a hack")
            
      }

      /* Inductive Cases: Search Rules */
      
      case Print(e1) => Print(step(e1))
      case Binary(op, v1, e2) if(isValue(v1)) => (op, v1, e2) match {
        case (Eq, v1, Function(_,_,_)) => throw new customException("Dynamic Typing Error")
        case (Eq, Function(_,_,_), e2) => throw new customException("Dynamic Typing Error")
        case (Ne, v1, Function(_,_,_)) => throw new customException("Dynamic Typing Error")
        case (Ne, Function(_,_,_), e2) => throw new customException("Dynamic Typing Error")
        case (_,_,_) => Binary(op, v1, step(e2))
      }
      case Binary(op, e1, e2) => Binary(op, step(e1), e2)
      case Unary(op, e1) if(!isValue(e1)) => Unary(op, step(e1))
      case If(e1, e2, e3) if(!isValue(e1)) => If(step(e1), e2, e3)
      case ConstDecl(x,v1, e2) if(!isValue(v1)) => ConstDecl(x, step(v1), e2)
      case Call(e1, e2) if(isValue(e1)) => Call(e1, step(e2))
//      case Call(e1, e2) if(!isValue(e1)) => Call(step(e1), e2)
      case Call(e1, e2) => Call(step(e1), e2)
   
      case _ => throw new UnsupportedOperationException
    }
  }
  
  def iterateStep(e: Expr): Expr =
    if (isValue(e)) e else iterateStep(step(e))
    
}