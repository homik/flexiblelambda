package pl.homik.flexiblelambda.service.impl;

import java.util.Collections;
import java.util.Map;

import de.hybris.bootstrap.annotations.UnitTest;
import de.hybris.platform.core.model.order.OrderModel;
import de.hybris.platform.core.model.test.TestItemModel;
import de.hybris.platform.servicelayer.model.ModelService;
import de.hybris.platform.servicelayer.search.FlexibleSearchQuery;

import org.apache.commons.lang3.StringUtils;
import org.fest.assertions.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import pl.homik.flexiblelambda.function.SerializablePredicate;
import pl.homik.flexiblelambda.pojo.LambdaFlexibleSearchQuery;

@UnitTest
@RunWith(MockitoJUnitRunner.class)
public class LambdaFlexibleSearchTranslationServiceImplUnitTest {

	private LambdaFlexibleSearchTranslationServiceImpl translationService;

	@Before
	public void prepare() {
		translationService = new LambdaFlexibleSearchTranslationServiceImpl();

		final ModelService modelService = Mockito.mock(ModelService.class);

		Mockito.when(modelService.getModelType(Mockito.any())).thenAnswer(invocationOnMock -> {
			final Class<?> argument = (Class<?>) invocationOnMock.getArguments()[0];
			return argument.getSimpleName().replace("Model", "");
		});
		translationService.setModelService(modelService);
	}

	@Test
	public void shouldGenerateCorrectNumberEqualityExpression() {
		checkWhere(e -> e.getInteger() == 0, "{this.integer} = ?a", 0);
	}

	@Test
	public void shouldGenerateCorrentNumberEqualityFromVariable() {
		final Integer intVar = 0;
		checkWhere(e -> e.getInteger() == intVar, "{this.integer} = ?a", 0);
	}

	@Test
	public void shouldGeneratoreCorrectEqualExpression() {
		checkWhere(e -> e.getString().equals("test"), "{this.string} = ?a", "test");

		final String stringVar = "xxx";
		checkWhere(e -> e.getString().equals(stringVar), "{this.string} = ?a", "xxx");
	}

	@Test
	public void shouldGeneratoreCorrectNotEqualExpression() {
		checkWhere(e -> !e.getString().equals("test"), "NOT({this.string} = ?a)", "test");

	}

	@Test
	public void shouldWorkWithComplexLambda() {

		final String stringVar = "xxx";
		final Integer intVar = 0;
		final SerializablePredicate<TestItemModel> lambda = e -> e.getString().equals(stringVar)
						|| e.getInteger() == intVar;
		checkWhere(lambda, "{this.string} = ?a or {this.integer} = ?b", stringVar, intVar);
	}

	@Test
	public void shouldWorkWithSameParameterUsedTwice() {
		final Integer intVar = 0;
		final SerializablePredicate<TestItemModel> lambda = e -> e.getInteger().equals(intVar)
						&& e.getPrimitiveInteger() == intVar;
		checkWhere(lambda, "{this.integer} = ?a AND {this.primitiveInteger} = ?b", intVar, intVar);
	}

	@Test
	public void shouldWorkWithReversedParametersOrder() {

		final Integer intVar = 0;
		final SerializablePredicate<TestItemModel> lambda = e -> intVar.equals(e.getInteger()) && intVar == e
						.getPrimitiveInteger();
		checkWhere(lambda, "?a = {this.integer} AND ?b = {this.primitiveInteger}", intVar, intVar);
	}

	@Test
	public void shouldLoadWithExpressionParam() {
		final Map<String, String> map2 = Collections.singletonMap("key2", "value");
		final Map<String, String> map1 = Collections.singletonMap("key", "key2");

		final SerializablePredicate<TestItemModel> lambda = e -> e.getString().equals(map2.get(map1.get("key")));
		checkWhere(lambda, "{this.string} = ?a", "value");
	}

	@Test
	public void shouldWorkWithOtherObject() {

		final StringBuilder builder = new StringBuilder();
		builder.append("test");

		final SerializablePredicate<TestItemModel> lambda = e -> e.getString().equals(builder.toString());
		checkWhere(lambda, "{this.string} = ?a", "test");
	}

	@Test
	public void shouldWorkWithNullComparison() {

		checkWhere(e -> e.getString() == null, "{this.string} is null");
		checkWhere(e -> e.getString() != null, "NOT({this.string} is null )");

	}

	@Test
	public void shouldWorkWithThreeExpression() {

		checkWhere(e -> e.getBoolean() && e.getString().equals("a") || e.getString().equals("b"),
						"{this.boolean} = ?a and {this.string} = ?b or {this.string} = ?c", true, "a", "b");
	}

	@Test
	public void shouldWorkWithBooleanFields() {

		checkWhere(e -> e.getBoolean(), "{this.boolean} = ?a", true);
		checkWhere(e -> !e.getBoolean(), "NOT({this.boolean} = ?a)", true);
		checkWhere(TestItemModel::getBoolean, "{this.boolean} = ?a", true);
	}

	@Test
	public void shouldGenerateCorrectStringFunctions() {
		checkWhere(e -> e.getString().startsWith("abc"), "{this.string} LIKE ?a", "abc%");
		checkWhere(e -> e.getString().endsWith("abc"), "{this.string} LIKE ?a", "%abc");
		checkWhere(e -> e.getString().contains("abc"), "{this.string} LIKE ?a", "%abc%");
		checkWhere(e -> e.getString().startsWith("abc") || e.getString().endsWith("abc"),
						"{this.string} LIKE ?a OR {this.string} LIKE ?b", "abc%", "%abc");
	}

	@Test
	public void shouldWorkWithoutOnSelfEqual() {
		checkWhere(e -> e.getString().equals(e.getA()), "{this.string} = {this.a}");
	}

	@Test
	public void shouldJoinWithoutArguments() {

		// given
		final SerializablePredicate<OrderModel> pred = e -> e.getUser().getName().equals(e.getStatusDisplay());
		final LambdaFlexibleSearchQuery<OrderModel> query = new LambdaFlexibleSearchQuery<>(OrderModel.class)
						.filter(pred);

		// when
		final FlexibleSearchQuery flex = translationService.translate(query);

		// then
		final String expectedQuery = "SELECT {this.PK} from {Order AS this"
						+ " LEFT JOIN User as thisuser on {this.user}={thisuser.PK}}" + " WHERE ({thisuser.name} = "
						+ "{this.statusDisplay})";
		Assertions.assertThat(flex.getQuery()).isEqualToIgnoringCase(expectedQuery);
		Assertions.assertThat(flex.getQueryParameters().values()).isEmpty();

	}

	@Test
	public void shouldJoinWhenNeeded() {

		// given
		final SerializablePredicate<OrderModel> pred = e -> e.getUser().getName().equals("Darek");
		final LambdaFlexibleSearchQuery<OrderModel> query = new LambdaFlexibleSearchQuery<>(OrderModel.class)
						.filter(pred);

		// when
		final FlexibleSearchQuery flex = translationService.translate(query);

		// then
		final String expectedQuery = "SELECT {this.PK} from {Order AS this"
						+ " LEFT JOIN User as thisuser on {this.user}={thisuser.PK}}" + " WHERE ({thisuser.name} = ?a)";
		Assertions.assertThat(flex.getQuery()).isEqualToIgnoringCase(expectedQuery);
		Assertions.assertThat(flex.getQueryParameters().values()).containsOnly("Darek");

	}

	@Test
	public void shouldDoubleJoinWhenNeeded() {

		// given
		final String poland = "pl";
		final SerializablePredicate<OrderModel> pred = e -> e.getDeliveryAddress().getCountry().getIsocode()
						.equals(poland);
		final LambdaFlexibleSearchQuery<OrderModel> query = new LambdaFlexibleSearchQuery<>(OrderModel.class)
						.filter(pred);

		final SerializablePredicate<OrderModel> pred2 = e ->
						e.getDeliveryAddress().getCountry().getIsocode().equals(poland) && e.getDeliveryAddress()
										.getCompany().equals("SAP");

		final LambdaFlexibleSearchQuery<OrderModel> query2 = new LambdaFlexibleSearchQuery<>(OrderModel.class)
						.filter(pred2);

		// when
		final FlexibleSearchQuery flex = translationService.translate(query);
		final FlexibleSearchQuery flex2 = translationService.translate(query2);

		// then
		final String expectedQuery = "SELECT {this.PK} from {Order AS this"
						+ " LEFT JOIN Address as thisdeliveryAddress on {this.deliveryAddress}={thisdeliveryAddress.PK}"
						+ " LEFT JOIN Country as thisdeliveryAddresscountry on {thisdeliveryAddress.country}={thisdeliveryAddresscountry.PK}}"
						+ " WHERE ({thisdeliveryAddresscountry.isocode} = ?a)";
		Assertions.assertThat(flex.getQuery()).isEqualToIgnoringCase(expectedQuery);

		Assertions.assertThat(flex.getQueryParameters().values()).containsOnly(poland);

		final String expectedQuery2 = "SELECT {this.PK} from {Order AS this"
						+ " LEFT JOIN Address as thisdeliveryAddress on {this.deliveryAddress}={thisdeliveryAddress.PK}"
						+ " LEFT JOIN Country as thisdeliveryAddresscountry on {thisdeliveryAddress.country}={thisdeliveryAddresscountry.PK}}"
						+ " WHERE ({thisdeliveryAddresscountry.isocode} = ?a AND {thisdeliveryAddress.company} = ?b)";
		Assertions.assertThat(flex2.getQuery()).isEqualToIgnoringCase(expectedQuery2);

		Assertions.assertThat(flex2.getQueryParameters().values()).containsOnly(poland, "SAP");

	}

	public void checkWhere(final SerializablePredicate<TestItemModel> pred, final String expectedWhere,
					final Object... expectedParams) {

		// when
		final LambdaFlexibleSearchQuery<TestItemModel> query = new LambdaFlexibleSearchQuery<>(TestItemModel.class)
						.filter(pred);
		final FlexibleSearchQuery flex = translationService.translate(query);

		// then
		final String actualWhere = getWhere(flex);
		Assertions.assertThat(actualWhere).isEqualToIgnoringCase(expectedWhere);
		Assertions.assertThat(flex.getQueryParameters().values()).containsOnly(expectedParams);

	}

	private String getWhere(final FlexibleSearchQuery flex) {

		final String where = StringUtils.substringAfter(flex.getQuery(), "WHERE").trim();
		// remove brackets
		return where.substring(1, where.length() - 1);
	}

}
