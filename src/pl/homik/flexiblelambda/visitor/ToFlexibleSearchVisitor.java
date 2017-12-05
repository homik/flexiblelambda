package pl.homik.flexiblelambda.visitor;

import static com.trigersoft.jaque.expression.ExpressionType.Equal;
import static com.trigersoft.jaque.expression.ExpressionType.GreaterThan;
import static com.trigersoft.jaque.expression.ExpressionType.GreaterThanOrEqual;
import static com.trigersoft.jaque.expression.ExpressionType.IsNull;
import static com.trigersoft.jaque.expression.ExpressionType.LessThan;
import static com.trigersoft.jaque.expression.ExpressionType.LessThanOrEqual;
import static com.trigersoft.jaque.expression.ExpressionType.LogicalAnd;
import static com.trigersoft.jaque.expression.ExpressionType.LogicalOr;

import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.Optional;
import java.util.Stack;
import java.util.function.UnaryOperator;

import de.hybris.bootstrap.annotations.Accessor;
import de.hybris.platform.core.model.ItemModel;
import de.hybris.platform.servicelayer.model.ModelService;

import com.trigersoft.jaque.expression.BinaryExpression;
import com.trigersoft.jaque.expression.ConstantExpression;
import com.trigersoft.jaque.expression.Expression;
import com.trigersoft.jaque.expression.ExpressionType;
import com.trigersoft.jaque.expression.ExpressionVisitor;
import com.trigersoft.jaque.expression.InvocationExpression;
import com.trigersoft.jaque.expression.LambdaExpression;
import com.trigersoft.jaque.expression.MemberExpression;
import com.trigersoft.jaque.expression.ParameterExpression;
import com.trigersoft.jaque.expression.UnaryExpression;

import pl.homik.flexiblelambda.constants.FlexiblelambdaConstants;
import pl.homik.flexiblelambda.pojo.PredicateTranslationResult;
import pl.homik.flexiblelambda.tools.ParametersNameGenerator;

public class ToFlexibleSearchVisitor implements ExpressionVisitor<PredicateTranslationResult> {

	private static final String SQL_LIKE = "LIKE";
	private final PredicateTranslationResult sb = new PredicateTranslationResult();
	private final ParametersNameGenerator paramGenerator;
	private final ModelService modelService;
	private final Stack<UnaryOperator<Object>> parameterModifiers = new Stack<>();
	private boolean columnBlock = false;

	public ToFlexibleSearchVisitor(final ParametersNameGenerator paramGenerator, final ModelService modelService) {
		this.paramGenerator = paramGenerator;
		this.modelService = modelService;
	}

	private String toSqlOp(final int expressionType) {
		switch (expressionType) {
		case LessThanOrEqual:
			return "<=";
		case GreaterThanOrEqual:
			return ">=";
		case LessThan:
			return "<";
		case GreaterThan:
			return ">";
		case Equal:
			return "=";
		case LogicalAnd:
			return "AND";
		case LogicalOr:
			return "OR";
		case IsNull:
			return "IS NULL";
		default:
			throw new UnsupportedOperationException(
							"unsupported expression type: " + expressionType + " " + ExpressionType
											.toString(expressionType));
		}
	}

	@Override
	public PredicateTranslationResult visit(final LambdaExpression<?> e) {
		final Expression body = e.getBody();
		return body.accept(this);
	}

	@Override
	public PredicateTranslationResult visit(final BinaryExpression e) {

		e.getFirst().accept(this);
		final int expressionType = e.getExpressionType();
		addSqlOperator(expressionType);
		e.getSecond().accept(this);

		return sb;
	}

	private void addSqlOperator(final int expressionType) {
		final String operator = toSqlOp(expressionType);
		addSqlOperator(operator);
	}

	private void addSqlOperator(final String operator) {
		sb.getWhere().append(" ").append(operator).append(" ");
	}

	@Override
	public PredicateTranslationResult visit(final ConstantExpression e) {
		final Object value = e.getValue();
		addSqlParam(value);
		return sb;
	}

	private void addSqlParam(Object value) {
		final String paramName = paramGenerator.next();
		sb.getWhere().append('?').append(paramName);

		if(!parameterModifiers.isEmpty()){
			final UnaryOperator<Object> modifier = parameterModifiers.pop();
			value = modifier.apply(value);
		}
		sb.getParameters().put(paramName, value);
	}

	@Override
	public PredicateTranslationResult visit(final InvocationExpression e) {
		//TODO: do some magic here to support expression parameters

		e.getTarget().accept(this);
		// sometime we have lambda in lambda when Object is used inside lambda and we have the value in args
		for (final Expression arg : e.getArguments()) {
			arg.accept(this);
		}

		return sb;
	}

	@Override
	public PredicateTranslationResult visit(final ParameterExpression e) {
		return sb;
	}

	@Override
	public PredicateTranslationResult visit(final UnaryExpression e) {

		if (e.getExpressionType() == ExpressionType.IsNull) {
			e.getFirst().accept(this);
			addSqlOperator(e.getExpressionType());
			return sb;
		} else if (e.getExpressionType() == ExpressionType.Convert) {
			// if its cast then we don do operation (its probable Integer to int or something like that)
			return e.getFirst().accept(this);
		} else if (e.getExpressionType() == ExpressionType.LogicalNot) {
			// if negate -> negate whole statement
			sb.getWhere().append("NOT(");
			e.getFirst().accept(this);
			sb.getWhere().append(")");
			return sb;
		} else {
			addSqlOperator(e.getExpressionType());
			return e.getFirst().accept(this);
		}
	}

	private String getTableAlias(final MemberExpression e) {

		if (isFromRelation(e)) {
			final InvocationExpression instance = (InvocationExpression) e.getInstance();
			final MemberExpression parentMember = (MemberExpression) instance.getTarget();
			final String columnName = getColumnName(parentMember).get();
			return getTableAlias(parentMember) + columnName;
		}
		// by default normal
		return FlexiblelambdaConstants.FS_MAIN_ALIAS;
	}

	@Override
	public PredicateTranslationResult visit(final MemberExpression e) {

		final Optional<String> colName = getColumnName(e);
		final boolean isGetter = colName.isPresent();
		final boolean relation = isGetter && isFromRelation(e);

		final boolean oldColumnBlock = columnBlock;
		if (isGetter) {
			// if getter we only need the last getter so we block adding columns on recursive calls
			columnBlock = true;
		}

		e.getInstance().accept(this);
		if (relation) {
			final InvocationExpression instance = (InvocationExpression) e.getInstance();
			// this is getRelation
			final MemberExpression parentMember = (MemberExpression) instance.getTarget();
			final String columnName = getColumnName(parentMember).get();
			final String parentTable = getTableAlias(parentMember);
			final String table = getTableAlias(e);

			final String typeCode = modelService.getModelType(instance.getResultType());
			final String join =
							"LEFT JOIN " + typeCode + " as " + table + " on {" + parentTable + "." + columnName + "}={"
											+ table + ".PK}";
			sb.getJoins().add(join);
		}
		columnBlock = oldColumnBlock;

		if (isGetter) {
			if (!columnBlock) {
				addColumn(colName.get(), getTableAlias(e));

				if (isBoolean(e.getMember())) {
					// for boolean  methods invocation we have to add '= true'
					addSqlOperator(Equal);
					addSqlParam(Boolean.TRUE);
				}
			}
		} else if (isEquals(e.getMember())) {
			// if method is equals we use '=' operator for sql
			addSqlOperator(ExpressionType.Equal);
		} else {
			tryHandleStringFunctions(e.getMember());
		}

		return sb;
	}

	private boolean tryHandleStringFunctions(final Member member) {
		if (member instanceof Method && member.getDeclaringClass().equals(String.class)
						&& ((Method) member).getParameters().length == 1) {
			final String methodName = member.getName();
			if (methodName.equals("startsWith")) {
				addSqlOperator(SQL_LIKE);
				parameterModifiers.push(e -> e + "%");
				return true;
			} else if (methodName.equals("endsWith")) {
				addSqlOperator(SQL_LIKE);
				parameterModifiers.push(e -> "%" + e);
				return true;
			} else if (methodName.equals("contains")) {
				addSqlOperator(SQL_LIKE);
				parameterModifiers.push(e -> "%" + e + "%");
				return true;
			}

		}
		return false;
	}

	private void handleStringStartsWith() {
	}

	private boolean isFromRelation(final MemberExpression e) {
		final Expression expression = e.getInstance();
		return expression instanceof InvocationExpression && ItemModel.class
						.isAssignableFrom(expression.getResultType());
	}

	private void addColumn(final String columnName, final String tableAlias) {
		sb.getWhere().append("{").append(tableAlias).append(".").append(columnName).append("}");
	}

	private Optional<String> getColumnName(final MemberExpression ex) {

		// @formatter:off
		return Optional.of(ex.getMember()).filter(e-> e instanceof  Method)
						.map(e-> (Method)e)
						.map(e -> e.getAnnotation(Accessor.class))
						.map( Accessor::qualifier);
		// @formatter:on
	}

	private boolean isBoolean(final Member member) {
		return member instanceof Method && ((Method) member).getReturnType().equals(Boolean.class);
	}

	private boolean isEquals(final Member member) {
		if (member instanceof Method) {
			final Method method = (Method) member;
			if (method.getName().equals("equals") && method.getParameters().length == 1 && method.getParameters()[0]
							.getType().equals(Object.class)) {
				return true;
			}
		}
		return false;
	}

}
