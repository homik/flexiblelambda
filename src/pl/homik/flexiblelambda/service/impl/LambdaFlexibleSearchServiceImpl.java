package pl.homik.flexiblelambda.service.impl;

import java.util.List;
import java.util.Optional;

import de.hybris.platform.core.model.ItemModel;
import de.hybris.platform.servicelayer.exceptions.AmbiguousIdentifierException;
import de.hybris.platform.servicelayer.exceptions.UnknownIdentifierException;
import de.hybris.platform.servicelayer.search.FlexibleSearchQuery;
import de.hybris.platform.servicelayer.search.FlexibleSearchService;

import org.springframework.beans.factory.annotation.Required;

import pl.homik.flexiblelambda.pojo.LambdaFlexibleSearchQuery;
import pl.homik.flexiblelambda.service.LambdaFlexibleSearchService;
import pl.homik.flexiblelambda.service.LambdaFlexibleSearchTranslationService;

public class LambdaFlexibleSearchServiceImpl implements LambdaFlexibleSearchService {

	private FlexibleSearchService flexibleSearchService;
	private LambdaFlexibleSearchTranslationService lambdaFlexibleSearchTranslationService;

	@Override
	public <T extends ItemModel> List<T> getList(final LambdaFlexibleSearchQuery<T> query) {
		final FlexibleSearchQuery translated = lambdaFlexibleSearchTranslationService.translate(query);
		return flexibleSearchService.<T>search(translated).getResult();
	}

	@Override
	public <T extends ItemModel> Optional<T> getFirst(final LambdaFlexibleSearchQuery<T> query) {
		query.limit(1);
		return getList(query).stream().findFirst();
	}

	@Override
	public <T extends ItemModel> T getSingleResult(final LambdaFlexibleSearchQuery<T> query) {

		query.limit(2);
		final List<T> resList = getList(query);

		if (resList.isEmpty()) {
			throw new UnknownIdentifierException("Cannot find item");
		}

		if (resList.size() > 1) {
			throw new AmbiguousIdentifierException("Found more than 1 result");
		}
		return resList.get(0);
	}

	@Required
	public void setFlexibleSearchService(final FlexibleSearchService flexibleSearchService) {
		this.flexibleSearchService = flexibleSearchService;
	}

	@Required
	public void setLambdaFlexibleSearchTranslationService(
					final LambdaFlexibleSearchTranslationService lambdaFlexibleSearchTranslationService) {
		this.lambdaFlexibleSearchTranslationService = lambdaFlexibleSearchTranslationService;
	}
}
