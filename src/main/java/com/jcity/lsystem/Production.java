package com.jcity.lsystem;

import org.apache.log4j.*;

import com.jcity.lsystem.condition.*;
import com.jcity.lsystem.modifier.*;

/**
 * Production of a L-System. This class supports context sensitive, conditional
 * Productions for parameterized L-System including modification of parameters.
 * 
 * @author philippd
 * 
 */
public class Production {

	private RoadModule predecessor;
	private ModuleString successor;
	private ModuleString leftContext;
	private ModuleString rightContext;
	private Condition condition;
	private Modifier modifier;
	private Logger logger = Logger.getLogger(this.getClass());

	/**
	 * @param predecessor
	 *            RoadModule which represents the predecessor, the left side of
	 *            a production.
	 * 
	 * @param successor
	 *            RoadModule string which represents the successor, the right
	 *            side of a production.
	 */
	public Production(RoadModule predecessor, ModuleString successor) {
		this(predecessor, successor, null, null, null, null);
	}

	public Production(RoadModule predecessor, ModuleString successor, Condition eval, Modifier pMod) {
		this(predecessor, successor, eval, pMod, null, null);
	}

	/**
	 * Except for the condition and the modifier the created object will work on
	 * its own copies of the given parameters. If you want to use only part of
	 * the features provide null pointers for unneeded parameters. Predecessor
	 * and successor are minimum requirements.
	 * 
	 * @param predecessor
	 *            RoadModule which represents the predecessor, the left side of
	 *            a production.
	 * @param successor
	 *            RoadModule string which represents the successor, the right
	 *            side of a production.
	 * @param eval
	 *            Evaluator which checks a optional condition for a production.
	 *            If true is returned the production will be executed, else not.
	 *            The first parameter is the index of the module that might get
	 *            replaced. The second parameter is the module string we work
	 *            on.
	 * @param pMod
	 *            Modifier which modifies the parameters of a derived module
	 *            string. The first parameter is the index of the module that
	 *            was replaced. The second and third parameters are the module
	 *            string before and after execution of the production.
	 * @param leftContext
	 *            Left context that must be met for the production to be
	 *            actually executed.
	 * @param rightContext
	 *            Right context that must be met for the production to be
	 *            actually executed.
	 */
	public Production(RoadModule predecessor, ModuleString successor, Condition eval, Modifier pMod, ModuleString leftContext, ModuleString rightContext) {

		// make local copies for predecessor and successor
		this.predecessor = new RoadModule(predecessor);
		this.successor = new ModuleString(successor);
		// now the optional parameters
		// if given pointer equals null create new object
		// else make a copy
		// left context
		if (leftContext == null)
			this.leftContext = new ModuleString();
		else
			this.leftContext = new ModuleString(leftContext);
		// right context
		if (rightContext == null)
			this.rightContext = new ModuleString();
		else
			this.rightContext = new ModuleString(rightContext);
		// set function pointers
		if (eval == null)
			this.condition = new Condition() {
				public boolean evaluate(int index, ModuleString mString) {
					return true;
				}
			};
		else
			this.condition = eval;

		// parameter modifier
		if (pMod == null)
			this.modifier = new Modifier() {
				public void modify(int index, ModuleString pred, ModuleString succ) {
					// do nothing.
				}
			};
		else
			this.modifier = pMod;

	}

	public Production(Production prod) {
		// copy all objects
		this(prod.predecessor, prod.successor, prod.condition, prod.modifier, prod.leftContext, prod.rightContext);
	}

	/**
	 * Applies this production at a given index of a module string. If the
	 * production can be applied (that is the module at the given index equals
	 * the left side of the production, both contexts and the condition is met)
	 * the actual replacement with the right side of the production will be
	 * carried out on the successor module string (can initially be treated as a
	 * copy of the predecessor module string). The method returns true if the
	 * production was successfully applied, else false. However the user should
	 * not call this method directly. It is called by the production manager.
	 * 
	 * @param index
	 *            index in module string
	 * @param mPredString
	 *            predecessor module string
	 * @param mSuccString
	 *            successor module string
	 * @return true if production was successfully applied, else false
	 */
	public boolean applyAtIndex(int index, ModuleString mPredString, ModuleString mSuccString) {
		// if module at index equals left side of production...
		if (mPredString.get(index).equals(this.predecessor)) {
//			logger.debug("Matched predecessor '" + this.predecessor.id+"'");
			// check if both contexts are true
			boolean lCon = checkLeftContext(index, mPredString);
			boolean rCon = checkRightContext(index, mPredString);
			// check if condition is true
			boolean cond = this.condition.evaluate(index, mPredString);
			// if condition and both contexts are true...
			if (lCon && rCon && cond) {
				// replace the module at the given index with a copy
				// of the right side in the successor string
				mSuccString.replace(index, this.successor.clone());
				// alter parameters
				this.modifier.modify(index, mPredString, mSuccString);
				return true;
			}
		} else {
//			logger.debug("No match for predecessor '" + this.predecessor.id+"'");
		}
		return false;
	}

	private boolean checkLeftContext(int index, ModuleString mString) {
		// get length of context
		int length = this.leftContext.size();
		// set result: true if length is zero, else false
		boolean result = (length == 0);
		// check if length of context is greater than zero and length
		// is less or equal than index (does the context actually fit?)
		if ((length > 0) && (length <= index)) {
			// make a copy of the sub string that should contain the context
			ModuleString temp = new ModuleString(index - length, index - 1, mString);
			// check if context is met
			result = (temp.equals(this.leftContext));
		}
		return result;
	}

	private boolean checkRightContext(int index, ModuleString mString) {
		// get length of context
		int length = this.rightContext.size();
		// set result: true if length is zero, else false
		boolean result = (length == 0);
		// check if length of context is greater than zero and length + index
		// is less than the length of the module string (does the context
		// actually fit?)
		if ((length > 0) && (index + length < mString.size())) {
			// make a copy of the sub string that should contain the context
			ModuleString temp = new ModuleString(index + 1, index + length, mString);
			// check if context is met
			result = (temp.equals(this.rightContext));
		}
		return result;
	}

	public void setRoadFlag(boolean lookOnlyAtRoads) {
		condition.setRoadFlag(lookOnlyAtRoads);
	}

}
