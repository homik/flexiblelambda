package pl.homik.flexiblelambda.pojo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import de.hybris.platform.core.model.ItemModel;

import pl.homik.flexiblelambda.function.SerializablePredicate;

public class LambdaFlexibleSearchQuery<T extends ItemModel> {

	private final List<SerializablePredicate<T>> filters = new ArrayList<>();
	private final Class<T> itemClass;
	private int limit=0;

	public LambdaFlexibleSearchQuery(final Class<T> itemClass) {
		this.itemClass = itemClass;
	}

	public LambdaFlexibleSearchQuery<T> filter(final SerializablePredicate<T> filter) {
		filters.add(filter);
		return this;
	}

	public LambdaFlexibleSearchQuery<T> limit(final int limit) {
		this.limit = limit;
		return this;
	}

	public List<SerializablePredicate<T>> getFilters() {
		return Collections.unmodifiableList(filters);
	}

	public int getLimit() {
		return limit;
	}

	public Class<T> getItemClass() {
		return itemClass;
	}
}
