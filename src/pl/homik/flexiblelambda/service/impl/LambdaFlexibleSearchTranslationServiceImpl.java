package pl.homik.flexiblelambda.service.impl;

import java.util.List;
import java.util.function.Predicate;

import de.hybris.platform.core.model.ItemModel;
import de.hybris.platform.servicelayer.model.ModelService;
import de.hybris.platform.servicelayer.search.FlexibleSearchQuery;

import org.springframework.beans.factory.annotation.Required;

import com.trigersoft.jaque.expression.LambdaExpression;

import pl.homik.flexiblelambda.constants.FlexiblelambdaConstants;
import pl.homik.flexiblelambda.function.SerializablePredicate;
import pl.homik.flexiblelambda.pojo.LambdaFlexibleSearchQuery;
import pl.homik.flexiblelambda.pojo.PredicateTranslationResult;
import pl.homik.flexiblelambda.service.LambdaFlexibleSearchTranslationService;
import pl.homik.flexiblelambda.tools.ParametersNameGenerator;
import pl.homik.flexiblelambda.visitor.ToFlexibleSearchVisitor;

public class LambdaFlexibleSearchTranslationServiceImpl implements LambdaFlexibleSearchTranslationService {

	private ModelService modelService;

	@Override
	public <T extends ItemModel> FlexibleSearchQuery translate(final LambdaFlexibleSearchQuery<T> query) {

		final List<SerializablePredicate<T>> filters = query.getFilters();

		final ParametersNameGenerator generator = new ParametersNameGenerator();
		final PredicateTranslationResult allFiltersResult = new PredicateTranslationResult();

		for (final Predicate<T> filter : filters) {

			final PredicateTranslationResult singleResult = LambdaExpression.parse(filter)
							.accept(new ToFlexibleSearchVisitor(generator, modelService));

			allFiltersResult.getJoins().addAll(singleResult.getJoins());
			allFiltersResult.getParameters().putAll(singleResult.getParameters());
			final StringBuilder allWhere = allFiltersResult.getWhere();
			final String filterWhere = singleResult.getWhere().toString().trim();
			if (!filterWhere.isEmpty()) {
				if (allWhere.length() > 0) {
					allWhere.append(" AND ");
				}
				allWhere.append('(').append(filterWhere).append(')');
			}
		}

		final FlexibleSearchQuery result = createQuery(allFiltersResult, query.getItemClass());
		if (query.getLimit() > 0) {
			result.setCount(query.getLimit());
		}
		return result;
	}

	private FlexibleSearchQuery createQuery(final PredicateTranslationResult allFiltersResult,
					final Class<?> itemClass) {
		final String typeCode = modelService.getModelType(itemClass);
		final StringBuilder query = new StringBuilder("SELECT {").append(FlexiblelambdaConstants.FS_MAIN_ALIAS)
						.append(".PK} from {").append(typeCode).append(" AS ")
						.append(FlexiblelambdaConstants.FS_MAIN_ALIAS);
		final String joins = String.join(" ", allFiltersResult.getJoins());
		if (!joins.isEmpty()) {
			query.append(" ").append(joins);
		}

		query.append("}");

		final StringBuilder where = allFiltersResult.getWhere();
		if (where.length() > 0) {
			query.append(" WHERE ").append(where);
		}

		return new FlexibleSearchQuery(query.toString(), allFiltersResult.getParameters());

	}

	@Required
	public void setModelService(final ModelService modelService) {
		this.modelService = modelService;
	}
}
