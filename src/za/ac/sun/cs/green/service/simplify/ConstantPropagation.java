package za.ac.sun.cs.green.service.simplify;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.logging.Level;

import za.ac.sun.cs.green.Instance;
import za.ac.sun.cs.green.Green;
import za.ac.sun.cs.green.expr.Expression;
import za.ac.sun.cs.green.service.BasicService;
import za.ac.sun.cs.green.util.Reporter;
import za.ac.sun.cs.green.expr.IntConstant;
import za.ac.sun.cs.green.expr.IntVariable;
import za.ac.sun.cs.green.expr.Operation;
import za.ac.sun.cs.green.expr.Variable;
import za.ac.sun.cs.green.expr.Visitor;
import za.ac.sun.cs.green.expr.VisitorException;

public class ConstantPropagation extends BasicService {
	private int invocations = 0;

	public ConstantPropagation(Green solver) {
		super(solver);
	}

	@Override
	public Set<Instance> processRequest(Instance instance) {
		@SuppressWarnings("unchecked")
		Set<Instance> result = (Set<Instance>) instance.getData(getClass());
		if (result == null) {
			final Map<Variable, Variable> map = new HashMap<Variable, Variable>();
			final Expression e = propagate(instance.getFullExpression(), map);
			final Instance i = new Instance(getSolver(), instance.getSource(), null, e);
			result = Collections.singleton(i);
			instance.setData(getClass(), result);
		}
		return result;
	}

	@Override
	public void report(Reporter reporter) {
		reporter.report(getClass().getSimpleName(), "invocations = " + invocations);
	}

	public Expression propagate(Expression expression, Map<Variable, Variable> map) {
		try {
			log.log(Level.FINEST, "Before Propagation: " + expression);
			invocations++;
			ConstPropVisitor propVisitor = new ConstPropVisitor();
			expression.accept(propVisitor);
			expression = propVisitor.getExpression();
			log.log(Level.FINEST, "After Propagation: " + expression);
			return expression;
		} catch (VisitorException x) {
			log.log(Level.SEVERE,
					"encountered an exception -- this should not be happening!",
					x);
		}
		return null;
	}


	private static class ConstPropVisitor extends Visitor {

		private Stack<Expression> stack;
		private Map<IntVariable, IntConstant> vars;

		public ConstPropVisitor() {
			stack = new Stack<>();
			vars = new HashMap<>();
		}

		public Expression getExpression() {
			return stack.pop();
		}

		@Override
		public void postVisit(IntConstant constant) {
			stack.push(constant);
		}

		@Override
		public void postVisit(IntVariable variable) {
			stack.push(variable);
		}

		@Override
		public void postVisit(Operation operation) throws VisitorException {
			Operation.Operator op = operation.getOperator();

			Expression r = stack.pop();
			Expression l = stack.pop();

			if (op.equals(Operation.Operator.EQ)) {
				if (r instanceof IntVariable && l instanceof IntConstant) {
					vars.put((IntVariable) r, (IntConstant) l);
				} else if (l instanceof IntVariable && r instanceof IntConstant) {
					vars.put((IntVariable) l, (IntConstant) r);
				}
			} else {
				if (r instanceof IntVariable && vars.containsKey(r)) {
					r = vars.get(r);
				} else if (l instanceof IntVariable && vars.containsKey(l)) {
					l = vars.get(l);
				}
			}
			stack.push(new Operation(op, l, r));
		}

	}
}