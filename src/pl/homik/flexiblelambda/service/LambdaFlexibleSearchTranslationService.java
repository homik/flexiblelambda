package pl.homik.flexiblelambda.service;

import de.hybris.platform.core.model.ItemModel;
import de.hybris.platform.servicelayer.search.FlexibleSearchQuery;

import pl.homik.flexiblelambda.pojo.LambdaFlexibleSearchQuery;

/**
 * Service designed to translate {@link LambdaFlexibleSearchQuery} to {@link FlexibleSearchQuery}
 */
public interface LambdaFlexibleSearchTranslationService {

	/**
	 * Translates given query to {@link FlexibleSearchQuery}
	 * @param query to translate
	 * @return flexible search query
	 */
	<T extends ItemModel> FlexibleSearchQuery translate(LambdaFlexibleSearchQuery<T> query);
}
