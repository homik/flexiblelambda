package pl.homik.flexiblelambda.visitor;

import com.trigersoft.jaque.expression.ConstantExpression;
import com.trigersoft.jaque.expression.Expression;
import com.trigersoft.jaque.expression.SimpleExpressionVisitor;

public class ConstantExpressionsCountVisitor extends SimpleExpressionVisitor {

	private int constantExpressionsCount=0;

	@Override
	public Expression visit(final ConstantExpression e) {
		constantExpressionsCount++;
		return super.visit(e);
	}

	public int getConstantExpressionsCount() {
		return constantExpressionsCount;
	}
}
