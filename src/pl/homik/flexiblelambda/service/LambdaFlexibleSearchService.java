package pl.homik.flexiblelambda.service;

import java.util.List;
import java.util.Optional;

import de.hybris.platform.core.model.ItemModel;
import de.hybris.platform.servicelayer.exceptions.AmbiguousIdentifierException;
import de.hybris.platform.servicelayer.exceptions.UnknownIdentifierException;

import pl.homik.flexiblelambda.pojo.LambdaFlexibleSearchQuery;

/**
 * Service which is capable of executing {@link LambdaFlexibleSearchQuery}
 */
public interface LambdaFlexibleSearchService {

	/**
	 * Executes given query and returns list of results
	 *
	 * @param query the query
	 * @return list of result or empty if not found
	 */
	<T extends ItemModel> List<T> getList(LambdaFlexibleSearchQuery<T> query);

	/**
	 * Executes given query and return first found result
	 *
	 * @param query the query
	 * @return First found result or empty
	 */
	<T extends ItemModel> Optional<T> getFirst(LambdaFlexibleSearchQuery<T> query);

	/**
	 * Executes given query and returns single result
	 *
	 * @param query the query
	 * @return single result returned by query (never null)
	 * @throws UnknownIdentifierException if query did not return any result
	 * @throws AmbiguousIdentifierException if query returned more than 1 result
	 */
	<T extends ItemModel> T getSingleResult(LambdaFlexibleSearchQuery<T> query);
}
