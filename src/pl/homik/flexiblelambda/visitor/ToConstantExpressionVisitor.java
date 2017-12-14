package pl.homik.flexiblelambda.visitor;

import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.trigersoft.jaque.expression.BinaryExpression;
import com.trigersoft.jaque.expression.ConstantExpression;
import com.trigersoft.jaque.expression.Expression;
import com.trigersoft.jaque.expression.ExpressionVisitor;
import com.trigersoft.jaque.expression.InvocationExpression;
import com.trigersoft.jaque.expression.LambdaExpression;
import com.trigersoft.jaque.expression.MemberExpression;
import com.trigersoft.jaque.expression.ParameterExpression;
import com.trigersoft.jaque.expression.UnaryExpression;

/**
 * Visitor that tries to execute given Expression and returns
 */
public class ToConstantExpressionVisitor implements ExpressionVisitor<ConstantExpression> {

	private List<Expression> parameters;
	private List<Expression> arguments = Collections.emptyList();
	private final Set<Expression> evaluatedParams = new HashSet<>();

	public ToConstantExpressionVisitor(final List<Expression> parameters) {
		this.parameters = new ArrayList<>(parameters);
	}

	@Override
	public ConstantExpression visit(final BinaryExpression binaryExpression) {
		throw new UnsupportedOperationException("Binary expression unsupported " + binaryExpression);
	}

	@Override
	public ConstantExpression visit(final ConstantExpression constantExpression) {
		return constantExpression;
	}

	@Override
	public ConstantExpression visit(final InvocationExpression invocationExpression) {
		final List<Expression> oldArgument = arguments;
		arguments = invocationExpression.getArguments().stream().map(e -> e.accept(this)).collect(Collectors.toList());
		final ConstantExpression result = invocationExpression.getTarget().accept(this);
		this.arguments = oldArgument;
		return result;
	}

	@Override
	public ConstantExpression visit(final LambdaExpression<?> lambdaExpression) {
		return lambdaExpression.getBody().accept(this);
	}

	@Override
	public ConstantExpression visit(final MemberExpression memberExpression) {

		final Member member = memberExpression.getMember();
		if (member instanceof Method) {
			final List<Expression> oldParams = parameters;
			parameters=arguments;
			final ConstantExpression obj = memberExpression.getInstance().accept(this);
			parameters=oldParams;
			final Object[] args = memberExpression.getParameters().stream().map(e -> e.getIndex())
							.map(e -> arguments.get(e.intValue())).map(e -> e.accept(this).getValue()).toArray();
			try {

				final Object result = ((Method) member).invoke(obj.getValue(), args);
				return Expression.constant(result);
			} catch (final ReflectiveOperationException e) {
				throw new IllegalStateException("cant do reflection call", e);
			}
		}

		throw new UnsupportedOperationException("member unsupported" + member);
	}

	@Override
	public ConstantExpression visit(final ParameterExpression parameterExpression) {
		final Expression params = parameters.get(parameterExpression.getIndex());
		evaluatedParams.add(params);
		return params.accept(this);
	}

	@Override
	public ConstantExpression visit(final UnaryExpression unaryExpression) {
		throw new UnsupportedOperationException("unaryExpression unsupported" + unaryExpression);
	}

	public Set<Expression> getEvaluatedParams() {
		return evaluatedParams;
	}
}
