package pl.homik.flexiblelambda.service;

import java.util.List;
import java.util.Optional;
import javax.annotation.Resource;

import de.hybris.bootstrap.annotations.IntegrationTest;
import de.hybris.platform.core.enums.PhoneContactInfoType;
import de.hybris.platform.core.model.user.PhoneContactInfoModel;
import de.hybris.platform.core.model.user.UserModel;
import de.hybris.platform.servicelayer.ServicelayerTransactionalBaseTest;
import de.hybris.platform.servicelayer.model.ModelService;

import org.fest.assertions.Assertions;
import org.junit.Test;

import pl.homik.flexiblelambda.pojo.LambdaFlexibleSearchQuery;

@IntegrationTest
public class LambdaFlexibleSearchServiceIntegrationTest extends ServicelayerTransactionalBaseTest {

	@Resource
	LambdaFlexibleSearchService lambdaFlexibleSearchService;

	@Resource
	ModelService modelService;

	@Test
	public void shouldLoadBySimpleProp() {
		//given
		final UserModel user = createTestUser();
		final String userUid = user.getUid();

		//when
		final LambdaFlexibleSearchQuery<UserModel> query = new LambdaFlexibleSearchQuery<>(UserModel.class)
						.filter(e -> e.getUid().equals(userUid));
		final Optional<UserModel> first = lambdaFlexibleSearchService.getFirst(query);
		final List<UserModel> list = lambdaFlexibleSearchService.getList(query);
		final UserModel found = lambdaFlexibleSearchService.getSingleResult(query);

		//then
		Assertions.assertThat(first.isPresent()).isTrue();
		Assertions.assertThat(first.get()).isEqualTo(user);

		Assertions.assertThat(list).hasSize(1).containsExactly(user);

		Assertions.assertThat(found).isEqualTo(user);
	}


	@Test
	public void shouldLoadWithExpressionParam() {
		//given
		final UserModel user = createTestUser();
		final PhoneContactInfoModel contactInfoModel = createContactInfoModel(user);

		//when
		final LambdaFlexibleSearchQuery<PhoneContactInfoModel> query = new LambdaFlexibleSearchQuery<>(
						PhoneContactInfoModel.class).filter(e -> e.getUser().getUid().equals(user.getUid()));
		final Optional<PhoneContactInfoModel> first = lambdaFlexibleSearchService.getFirst(query);
		final List<PhoneContactInfoModel> list = lambdaFlexibleSearchService.getList(query);
		final PhoneContactInfoModel found = lambdaFlexibleSearchService.getSingleResult(query);

		//then
		Assertions.assertThat(first.isPresent()).isTrue();
		Assertions.assertThat(first.get()).isEqualTo(contactInfoModel);

		Assertions.assertThat(list).hasSize(1).containsExactly(contactInfoModel);

		Assertions.assertThat(found).isEqualTo(contactInfoModel);
	}

	@Test
	public void shouldLoadWithJoin() {
		//given
		final UserModel user = createTestUser();
		final PhoneContactInfoModel contactInfoModel = createContactInfoModel(user);
		final String userUid = user.getUid();

		//when
		final LambdaFlexibleSearchQuery<PhoneContactInfoModel> query = new LambdaFlexibleSearchQuery<>(
						PhoneContactInfoModel.class).filter(e -> e.getUser().getUid().equals(userUid));
		final Optional<PhoneContactInfoModel> first = lambdaFlexibleSearchService.getFirst(query);
		final List<PhoneContactInfoModel> list = lambdaFlexibleSearchService.getList(query);
		final PhoneContactInfoModel found = lambdaFlexibleSearchService.getSingleResult(query);

		//then
		Assertions.assertThat(first.isPresent()).isTrue();
		Assertions.assertThat(first.get()).isEqualTo(contactInfoModel);

		Assertions.assertThat(list).hasSize(1).containsExactly(contactInfoModel);

		Assertions.assertThat(found).isEqualTo(contactInfoModel);
	}


	@Test
	public void shouldWorkForModelParameter() {
		//given
		final UserModel user = createTestUser();
		final PhoneContactInfoModel contactInfoModel = createContactInfoModel(user);

		//when
		final LambdaFlexibleSearchQuery<PhoneContactInfoModel> query = new LambdaFlexibleSearchQuery<>(
						PhoneContactInfoModel.class).filter(e -> e.getUser().equals(user));
		final Optional<PhoneContactInfoModel> first = lambdaFlexibleSearchService.getFirst(query);
		final List<PhoneContactInfoModel> list = lambdaFlexibleSearchService.getList(query);
		final PhoneContactInfoModel found = lambdaFlexibleSearchService.getSingleResult(query);

		//then
		Assertions.assertThat(first.isPresent()).isTrue();
		Assertions.assertThat(first.get()).isEqualTo(contactInfoModel);

		Assertions.assertThat(list).hasSize(1).containsExactly(contactInfoModel);

		Assertions.assertThat(found).isEqualTo(contactInfoModel);
	}

	private PhoneContactInfoModel createContactInfoModel(final UserModel user) {

		final PhoneContactInfoModel contactInfoModel = modelService.create(PhoneContactInfoModel.class);
		contactInfoModel.setPhoneNumber("phone number");
		contactInfoModel.setUser(user);
		contactInfoModel.setCode("test_contact_phone");
		contactInfoModel.setType(PhoneContactInfoType.HOME);
		modelService.save(contactInfoModel);
		return contactInfoModel;
	}

	private UserModel createTestUser() {
		final UserModel user = modelService.create(UserModel.class);

		user.setName("test");
		user.setUid("some_example_uid");

		modelService.save(user);
		return user;
	}

}
